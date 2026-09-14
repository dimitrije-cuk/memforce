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
import java.util.Set;
import java.util.TreeSet;

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
     * Reads the questions a condition selects together with their tag names and the alternative
     * answers they accept.
     *
     * <p>Both lists are read by a further statement rather than by a {@code GROUP_CONCAT} over a
     * join: SQLite before 3.44 cannot order the values an aggregate collects, and a tag name or an
     * answer may itself contain the separator such a concatenation would use. Separate statements
     * keep the order defined and keep values with punctuation intact.
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
        collect(db, tagSql, args, tagNames);

        // Ordered by row id, which is the order the alternatives were stored in.
        Map<Long, List<String>> alternatives = new HashMap<>();
        String alternativeSql = "SELECT a." + DbContract.AlternativeAnswers.QUESTION_ID
                + ", a." + DbContract.AlternativeAnswers.ANSWER
                + " FROM " + DbContract.AlternativeAnswers.TABLE + " a"
                + " WHERE a." + DbContract.AlternativeAnswers.QUESTION_ID + " IN ("
                + "SELECT q." + DbContract.Questions._ID
                + " FROM " + DbContract.Questions.TABLE + " q WHERE " + predicate + ")"
                + " ORDER BY a." + DbContract.AlternativeAnswers._ID + " ASC";
        collect(db, alternativeSql, args, alternatives);

        List<Question> questions = new ArrayList<>(rows.size());
        for (Map.Entry<Long, String[]> row : rows.entrySet()) {
            List<String> names = tagNames.get(row.getKey());
            List<String> accepted = alternatives.get(row.getKey());
            questions.add(new Question(row.getKey(), row.getValue()[0], row.getValue()[1],
                    accepted == null ? Collections.<String>emptyList() : accepted,
                    names == null ? Collections.<String>emptyList() : names));
        }
        return questions;
    }

    /** Reads an (question id, text) statement into a list per question, keeping its order. */
    private void collect(SQLiteDatabase db,
                         String sql,
                         String[] args,
                         Map<Long, List<String>> into) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                List<String> values = into.get(cursor.getLong(0));
                if (values == null) {
                    values = new ArrayList<>();
                    into.put(cursor.getLong(0), values);
                }
                values.add(cursor.getString(1));
            }
        }
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
                       @NonNull List<String> alternativeAnswers,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insert(DbContract.Questions.TABLE, null, toValues(name, answer));
            if (id != -1) {
                replaceTags(db, id, tagIds);
                writeAlternativeAnswers(db, id, answer, alternativeAnswers);
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
                       @NonNull List<String> alternativeAnswers,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.update(DbContract.Questions.TABLE, toValues(name, answer),
                    DbContract.Questions._ID + " = ?", new String[]{String.valueOf(id)});
            replaceTags(db, id, tagIds);
            db.delete(DbContract.AlternativeAnswers.TABLE,
                    DbContract.AlternativeAnswers.QUESTION_ID + " = ?",
                    new String[]{String.valueOf(id)});
            writeAlternativeAnswers(db, id, answer, alternativeAnswers);
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
     * Adds alternative answers a question does not accept yet and keeps the ones it has. An
     * alternative is additive, as a tag is: it widens what counts as correct and so can never
     * destroy work already done, which is why an import may apply it to an existing question while
     * the stored answer itself is left alone.
     */
    public void addAlternativeAnswers(long questionId,
                                      @NonNull List<String> alternativeAnswers) {
        writeAlternativeAnswers(helper.getWritableDatabase(), questionId,
                storedAnswer(questionId), alternativeAnswers);
    }

    @Nullable
    private String storedAnswer(long questionId) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.Questions.TABLE,
                new String[]{DbContract.Questions.ANSWER},
                DbContract.Questions._ID + " = ?",
                new String[]{String.valueOf(questionId)},
                null, null, null)) {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0) : null;
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

    /** Tag assignments and alternative answers are removed by the schema's cascade rules. */
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

    /**
     * Writes the alternatives that widen what a question accepts. A blank entry, one repeating the
     * answer, and one repeating an earlier alternative add nothing a game would mark differently,
     * so they are left out; letter case is disregarded throughout, exactly as marking disregards
     * it. The unique index holds the same rule at the storage level, so a row already present is
     * ignored rather than raised.
     */
    private void writeAlternativeAnswers(SQLiteDatabase db,
                                         long questionId,
                                         @Nullable String answer,
                                         List<String> alternativeAnswers) {
        Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (answer != null && !answer.trim().isEmpty()) {
            seen.add(answer.trim());
        }
        for (String alternative : alternativeAnswers) {
            if (alternative == null) {
                continue;
            }
            String value = alternative.trim();
            if (value.isEmpty() || !seen.add(value)) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(DbContract.AlternativeAnswers.QUESTION_ID, questionId);
            values.put(DbContract.AlternativeAnswers.ANSWER, value);
            db.insertWithOnConflict(DbContract.AlternativeAnswers.TABLE, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
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
