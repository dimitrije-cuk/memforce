package com.memforce.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.db.DbContract;
import com.memforce.db.MemForceDbHelper;
import com.memforce.db.SearchPatterns;
import com.memforce.model.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionDao {

    private final MemForceDbHelper helper;

    public QuestionDao(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
    }

    /**
     * @param namePattern LIKE pattern applied to the question text
     * @param tagId       when set, only questions carrying this tag are returned
     */
    @NonNull
    public List<Question> search(@Nullable String namePattern, @Nullable Long tagId) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE q.").append(DbContract.Questions.NAME).append(" LIKE ?");

        List<String> args = new ArrayList<>();
        args.add(SearchPatterns.like(namePattern));
        if (tagId != null) {
            sql.append(" AND q.").append(DbContract.Questions._ID)
                    .append(" IN (SELECT ").append(DbContract.QuestionTags.QUESTION_ID)
                    .append(" FROM ").append(DbContract.QuestionTags.TABLE)
                    .append(" WHERE ").append(DbContract.QuestionTags.TAG_ID).append(" = ?)");
            args.add(String.valueOf(tagId));
        }
        sql.append(" GROUP BY q.").append(DbContract.Questions._ID)
                .append(" ORDER BY q.").append(DbContract.Questions.NAME).append(" COLLATE NOCASE ASC");

        List<Question> questions = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase()
                .rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                questions.add(read(cursor));
            }
        }
        return questions;
    }

    @Nullable
    public Question findById(long id) {
        String sql = baseSelect()
                + " WHERE q." + DbContract.Questions._ID + " = ?"
                + " GROUP BY q." + DbContract.Questions._ID;
        try (Cursor cursor = helper.getReadableDatabase()
                .rawQuery(sql, new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
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

    private String baseSelect() {
        return "SELECT q." + DbContract.Questions._ID
                + ", q." + DbContract.Questions.NAME
                + ", q." + DbContract.Questions.ANSWER
                + ", IFNULL(GROUP_CONCAT(t." + DbContract.Tags.NAME + ", ', '), '')"
                + " FROM " + DbContract.Questions.TABLE + " q"
                + " LEFT JOIN " + DbContract.QuestionTags.TABLE + " qt"
                + " ON qt." + DbContract.QuestionTags.QUESTION_ID + " = q." + DbContract.Questions._ID
                + " LEFT JOIN " + DbContract.Tags.TABLE + " t"
                + " ON t." + DbContract.Tags._ID + " = qt." + DbContract.QuestionTags.TAG_ID;
    }

    private Question read(Cursor cursor) {
        return new Question(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.isNull(2) ? null : cursor.getString(2),
                cursor.getString(3));
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
