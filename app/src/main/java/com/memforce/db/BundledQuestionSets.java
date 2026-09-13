package com.memforce.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.importer.MergedQuestion;
import com.memforce.importer.QuestionSet;
import com.memforce.importer.QuestionSetFormatException;
import com.memforce.importer.QuestionSetParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads the question sets that ship inside the application package, so that a fresh database
 * already holds the topics the team decided every installation should have.
 *
 * <p>A set is an ordinary question-set file of {@code docs/question-import-format.md} kept under
 * {@value #FOLDER} in the source tree; the build packages that folder into the APK. Adding a topic
 * is therefore adding a file — no code, no resource and no user action.
 *
 * <p>The files are read by {@link QuestionSetParser}, the same parser the user-facing import uses,
 * and stored under the same merge rules, so a bundled set and an imported one cannot disagree
 * about what a question set means. They cannot go through {@code QuestionSetImporter} because this
 * runs inside {@link MemForceDbHelper#onCreate}, where asking the helper for a writable database
 * would re-enter the open that is still in progress; the rows are written on the handle
 * {@code onCreate} supplies instead.
 *
 * <p>A bundled file that does not parse is a fault in the build, not bad input from a user, so it
 * fails loudly rather than being skipped. {@code BundledQuestionSetsTest} parses every one of them
 * on the JVM to keep that failure inside the build rather than on a device.
 */
final class BundledQuestionSets {

    /** Every {@code .json} file directly inside this asset folder is loaded, in name order. */
    static final String FOLDER = "question-sets";

    static final String SUFFIX = ".json";

    private BundledQuestionSets() {
    }

    static void load(@NonNull SQLiteDatabase db, @NonNull Context context) {
        Map<String, Long> tagIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String asset : list(context)) {
            store(db, parse(read(context, asset), asset), tagIds);
        }
    }

    /**
     * Sorted so that the rows of a fresh database, and therefore their identifiers, do not depend
     * on the order in which the platform happens to list the folder.
     */
    private static List<String> list(Context context) {
        String[] names;
        try {
            names = context.getAssets().list(FOLDER);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + FOLDER, e);
        }
        List<String> sets = new ArrayList<>();
        if (names != null) {
            Arrays.sort(names);
            for (String name : names) {
                if (name.endsWith(SUFFIX)) {
                    sets.add(FOLDER + "/" + name);
                }
            }
        }
        return sets;
    }

    private static String read(Context context, String asset) {
        try (InputStream input = context.getAssets().open(asset)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + asset, e);
        }
    }

    private static QuestionSet parse(String json, String asset) {
        try {
            return QuestionSetParser.parse(json);
        } catch (QuestionSetFormatException e) {
            throw new IllegalStateException(asset + " is not a valid question set", e);
        }
    }

    /**
     * Applies one set with the rules of the user-facing import: a tag or a question already
     * present is reused rather than duplicated, matched without regard to letter case, and an
     * answer already stored is never overwritten.
     */
    private static void store(SQLiteDatabase db, QuestionSet set, Map<String, Long> tagIds) {
        for (MergedQuestion question : MergedQuestion.mergeAll(set)) {
            List<Long> ids = new ArrayList<>();
            for (String tag : question.getTags()) {
                Long id = tagIds.get(tag);
                if (id == null) {
                    id = findTagId(db, tag);
                    if (id == null) {
                        id = insertTag(db, tag);
                    }
                    tagIds.put(tag, id);
                }
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }

            Long existing = findQuestionId(db, question.getQuestion());
            if (existing == null) {
                addTags(db, insertQuestion(db, question), ids);
            } else {
                addTags(db, existing, ids);
                if (question.getAnswer() != null) {
                    fillMissingAnswer(db, existing, question.getAnswer());
                }
            }
        }
    }

    @Nullable
    private static Long findTagId(SQLiteDatabase db, String name) {
        return firstId(db, "SELECT " + DbContract.Tags._ID
                + " FROM " + DbContract.Tags.TABLE
                + " WHERE " + DbContract.Tags.NAME + " = ? COLLATE NOCASE"
                + " LIMIT 1", name);
    }

    @Nullable
    private static Long findQuestionId(SQLiteDatabase db, String name) {
        return firstId(db, "SELECT " + DbContract.Questions._ID
                + " FROM " + DbContract.Questions.TABLE
                + " WHERE " + DbContract.Questions.NAME + " = ? COLLATE NOCASE"
                + " LIMIT 1", name);
    }

    @Nullable
    private static Long firstId(SQLiteDatabase db, String sql, String argument) {
        try (Cursor cursor = db.rawQuery(sql, new String[]{argument})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : null;
        }
    }

    private static long insertTag(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Tags.NAME, name);
        return db.insertOrThrow(DbContract.Tags.TABLE, null, values);
    }

    private static long insertQuestion(SQLiteDatabase db, MergedQuestion question) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Questions.NAME, question.getQuestion());
        values.put(DbContract.Questions.ANSWER, question.getAnswer());
        return db.insertOrThrow(DbContract.Questions.TABLE, null, values);
    }

    /** A question already carrying one of the tags keeps the single assignment it has. */
    private static void addTags(SQLiteDatabase db, long questionId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            ContentValues link = new ContentValues();
            link.put(DbContract.QuestionTags.QUESTION_ID, questionId);
            link.put(DbContract.QuestionTags.TAG_ID, tagId);
            db.insertWithOnConflict(DbContract.QuestionTags.TABLE, null, link,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private static void fillMissingAnswer(SQLiteDatabase db, long id, String answer) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Questions.ANSWER, answer);
        db.update(DbContract.Questions.TABLE, values,
                DbContract.Questions._ID + " = ?"
                        + " AND (" + DbContract.Questions.ANSWER + " IS NULL"
                        + " OR TRIM(" + DbContract.Questions.ANSWER + ") = '')",
                new String[]{String.valueOf(id)});
    }
}
