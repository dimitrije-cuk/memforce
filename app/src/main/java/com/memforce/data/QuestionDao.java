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
     * @param categoryId  when set, only questions in this category are returned
     * @param tagId       when set, only questions carrying this tag are returned
     */
    @NonNull
    public List<Question> search(@Nullable String namePattern,
                                 @Nullable Long categoryId,
                                 @Nullable Long tagId) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE q.").append(DbContract.Questions.NAME).append(" LIKE ?");

        List<String> args = new ArrayList<>();
        args.add(SearchPatterns.like(namePattern));
        if (categoryId != null) {
            sql.append(" AND q.").append(DbContract.Questions.CATEGORY_ID).append(" = ?");
            args.add(String.valueOf(categoryId));
        }
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

    private String baseSelect() {
        return "SELECT q." + DbContract.Questions._ID
                + ", q." + DbContract.Questions.NAME
                + ", q." + DbContract.Questions.ANSWER
                + ", q." + DbContract.Questions.CATEGORY_ID
                + ", IFNULL(c." + DbContract.Categories.NAME + ", '')"
                + ", IFNULL(GROUP_CONCAT(t." + DbContract.Tags.NAME + ", ', '), '')"
                + " FROM " + DbContract.Questions.TABLE + " q"
                + " LEFT JOIN " + DbContract.Categories.TABLE + " c"
                + " ON c." + DbContract.Categories._ID + " = q." + DbContract.Questions.CATEGORY_ID
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
                cursor.isNull(3) ? null : cursor.getLong(3),
                cursor.getString(4),
                cursor.getString(5));
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

    public long insert(@NonNull String name,
                       @Nullable String answer,
                       @Nullable Long categoryId,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insert(DbContract.Questions.TABLE, null, toValues(name, answer, categoryId));
            if (id == -1) {
                return -1;
            }
            replaceTags(db, id, tagIds);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void update(long id,
                       @NonNull String name,
                       @Nullable String answer,
                       @Nullable Long categoryId,
                       @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.update(DbContract.Questions.TABLE, toValues(name, answer, categoryId),
                    DbContract.Questions._ID + " = ?", new String[]{String.valueOf(id)});
            replaceTags(db, id, tagIds);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Tag assignments and deck membership are removed by the schema's cascade rules. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Questions.TABLE,
                DbContract.Questions._ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(String name, @Nullable String answer, @Nullable Long categoryId) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Questions.NAME, name);
        values.put(DbContract.Questions.ANSWER, answer);
        if (categoryId == null) {
            values.putNull(DbContract.Questions.CATEGORY_ID);
        } else {
            values.put(DbContract.Questions.CATEGORY_ID, categoryId);
        }
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
