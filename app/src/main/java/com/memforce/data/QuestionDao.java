package com.memforce.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.db.DbContract;
import com.memforce.db.MemForceDbHelper;
import com.memforce.model.Question;
import com.memforce.search.SearchQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class QuestionDao {

    /** SQLite accepts at most 999 statement arguments, so long identifier lists are read in parts. */
    private static final int MAX_IDS_PER_QUERY = 400;

    private final MemForceDbHelper helper;

    public QuestionDao(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
    }

    /**
     * Reads the questions the criteria select, ordered by question text.
     *
     * @param query the text pattern and the chosen tags; empty criteria select every question
     */
    @NonNull
    public List<Question> search(@NonNull SearchQuery query) {
        QuestionFilter filter = QuestionFilter.of(query);
        return load(filter.sql(), filter.args());
    }

    @Nullable
    public Question findById(long id) {
        List<Question> found = load(
                "q." + DbContract.Questions._ID + " = ?", new String[]{String.valueOf(id)});
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Reads the named questions, ordered by question text. Identifiers that match no stored
     * question are skipped, so a lobby that still names a deleted question stays usable.
     */
    @NonNull
    public List<Question> findByIds(@NonNull Collection<Long> ids) {
        List<Long> remaining = new ArrayList<>(new LinkedHashSet<>(ids));
        List<Question> questions = new ArrayList<>();
        for (int from = 0; from < remaining.size(); from += MAX_IDS_PER_QUERY) {
            List<Long> chunk = remaining.subList(
                    from, Math.min(from + MAX_IDS_PER_QUERY, remaining.size()));
            StringBuilder predicate = new StringBuilder("q.")
                    .append(DbContract.Questions._ID).append(" IN (");
            String[] args = new String[chunk.size()];
            for (int i = 0; i < chunk.size(); i++) {
                predicate.append(i == 0 ? "?" : ",?");
                args[i] = String.valueOf(chunk.get(i));
            }
            questions.addAll(load(predicate.append(')').toString(), args));
        }
        if (remaining.size() > MAX_IDS_PER_QUERY) {
            // Each part came back ordered, but the parts were read one after another, so the
            // order only holds across the whole list once they are merged.
            Collections.sort(questions,
                    (left, right) -> String.CASE_INSENSITIVE_ORDER.compare(
                            left.getName(), right.getName()));
        }
        return questions;
    }

    /** The identifiers of every question carrying this tag. */
    @NonNull
    public List<Long> idsWithTag(long tagId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.QuestionTags.TABLE,
                new String[]{DbContract.QuestionTags.QUESTION_ID},
                DbContract.QuestionTags.TAG_ID + " = ?",
                new String[]{String.valueOf(tagId)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    /**
     * @return the id of the question with this exact text, or null when there is none. The column
     * has no collation of its own, so the comparison asks for a case insensitive one.
     */
    @Nullable
    public Long findIdByName(@NonNull String name) {
        String sql = "SELECT " + DbContract.Questions._ID
                + " FROM " + DbContract.Questions.TABLE
                + " WHERE " + DbContract.Questions.NAME + " = ? COLLATE NOCASE"
                + " LIMIT 1";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, new String[]{name})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : null;
        }
    }

    /**
     * Reads the questions a condition selects together with their tag names.
     *
     * <p>The tag names are read by a second statement rather than by a {@code GROUP_CONCAT} over a
     * join: SQLite before 3.44 cannot order the values an aggregate collects, and a tag name may
     * itself contain the separator such a concatenation would use. Two statements keep the tag
     * order defined and keep names with punctuation intact.
     *
     * @param predicate a condition over the question table aliased {@code q}
     */
    @NonNull
    private List<Question> load(@NonNull String predicate, @NonNull String[] args) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT q." + DbContract.Questions._ID
                + ", q." + DbContract.Questions.NAME
                + ", q." + DbContract.Questions.ANSWER
                + " FROM " + DbContract.Questions.TABLE + " q"
                + " WHERE " + predicate
                + " ORDER BY q." + DbContract.Questions.NAME + " COLLATE NOCASE ASC";

        Map<Long, String[]> rows = new LinkedHashMap<>();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                rows.put(cursor.getLong(0), new String[]{
                        cursor.getString(1), cursor.isNull(2) ? null : cursor.getString(2)});
            }
        }
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<String>> tagNames = new HashMap<>();
        String tagSql = "SELECT qt." + DbContract.QuestionTags.QUESTION_ID
                + ", t." + DbContract.Tags.NAME
                + " FROM " + DbContract.QuestionTags.TABLE + " qt"
                + " JOIN " + DbContract.Tags.TABLE + " t"
                + " ON t." + DbContract.Tags._ID + " = qt." + DbContract.QuestionTags.TAG_ID
                + " WHERE qt." + DbContract.QuestionTags.QUESTION_ID + " IN ("
                + "SELECT q." + DbContract.Questions._ID
                + " FROM " + DbContract.Questions.TABLE + " q WHERE " + predicate + ")"
                + " ORDER BY t." + DbContract.Tags.NAME + " COLLATE NOCASE ASC";
        try (Cursor cursor = db.rawQuery(tagSql, args)) {
            while (cursor.moveToNext()) {
                List<String> names = tagNames.get(cursor.getLong(0));
                if (names == null) {
                    names = new ArrayList<>();
                    tagNames.put(cursor.getLong(0), names);
                }
                names.add(cursor.getString(1));
            }
        }

        List<Question> questions = new ArrayList<>(rows.size());
        for (Map.Entry<Long, String[]> row : rows.entrySet()) {
            List<String> names = tagNames.get(row.getKey());
            questions.add(new Question(row.getKey(), row.getValue()[0], row.getValue()[1],
                    names == null ? Collections.<String>emptyList() : names));
        }
        return questions;
    }

    @NonNull
    public List<Long> tagIdsOf(long questionId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.QuestionTags.TABLE,
                new String[]{DbContract.QuestionTags.TAG_ID},
                DbContract.QuestionTags.QUESTION_ID + " = ?",
                new String[]{String.valueOf(questionId)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    /**
     * @return the new row id, or -1 when the question could not be stored. The transaction is
     * always closed as successful, so a failure here cannot roll back a surrounding import.
     */
    public long insert(@NonNull String name,
                       @Nullable String answer,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insert(DbContract.Questions.TABLE, null, toValues(name, answer));
            if (id != -1) {
                replaceTags(db, id, tagIds);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void update(long id,
                       @NonNull String name,
                       @Nullable String answer,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.update(DbContract.Questions.TABLE, toValues(name, answer),
                    DbContract.Questions._ID + " = ?", new String[]{String.valueOf(id)});
            replaceTags(db, id, tagIds);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Adds tags a question does not carry yet and keeps the ones it already has. */
    public void addTags(long questionId, @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        for (Long tagId : tagIds) {
            ContentValues link = new ContentValues();
            link.put(DbContract.QuestionTags.QUESTION_ID, questionId);
            link.put(DbContract.QuestionTags.TAG_ID, tagId);
            db.insertWithOnConflict(DbContract.QuestionTags.TABLE, null, link,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    /**
     * Stores an answer only for a question that has none, so an existing answer is never lost.
     *
     * @return true when the answer was stored
     */
    public boolean fillMissingAnswer(long id, @NonNull String answer) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Questions.ANSWER, answer);
        return helper.getWritableDatabase().update(
                DbContract.Questions.TABLE, values,
                DbContract.Questions._ID + " = ?"
                        + " AND (" + DbContract.Questions.ANSWER + " IS NULL"
                        + " OR TRIM(" + DbContract.Questions.ANSWER + ") = '')",
                new String[]{String.valueOf(id)}) > 0;
    }

    /** Tag assignments are removed by the schema's cascade rules. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Questions.TABLE,
                DbContract.Questions._ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(String name, @Nullable String answer) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Questions.NAME, name);
        values.put(DbContract.Questions.ANSWER, answer);
        return values;
    }

    private void replaceTags(SQLiteDatabase db, long questionId, List<Long> tagIds) {
        db.delete(DbContract.QuestionTags.TABLE,
                DbContract.QuestionTags.QUESTION_ID + " = ?",
                new String[]{String.valueOf(questionId)});
        for (Long tagId : tagIds) {
            ContentValues link = new ContentValues();
            link.put(DbContract.QuestionTags.QUESTION_ID, questionId);
            link.put(DbContract.QuestionTags.TAG_ID, tagId);
            db.insert(DbContract.QuestionTags.TABLE, null, link);
        }
    }
}
