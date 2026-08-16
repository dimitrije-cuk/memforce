package com.memforce.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.db.DbContract;
import com.memforce.db.MemForceDbHelper;
import com.memforce.db.SearchPatterns;
import com.memforce.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private final MemForceDbHelper helper;

    public CategoryDao(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
    }

    /**
     * @param namePattern LIKE pattern applied to the category name
     * @param tagId       when set, only categories carrying this tag are returned
     */
    @NonNull
    public List<Category> search(@Nullable String namePattern, @Nullable Long tagId) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT c.").append(DbContract.Categories._ID)
                .append(", c.").append(DbContract.Categories.NAME)
                .append(", IFNULL(GROUP_CONCAT(t.").append(DbContract.Tags.NAME).append(", ', '), '')")
                .append(" FROM ").append(DbContract.Categories.TABLE).append(" c")
                .append(" LEFT JOIN ").append(DbContract.CategoryTags.TABLE).append(" ct")
                .append(" ON ct.").append(DbContract.CategoryTags.CATEGORY_ID)
                .append(" = c.").append(DbContract.Categories._ID)
                .append(" LEFT JOIN ").append(DbContract.Tags.TABLE).append(" t")
                .append(" ON t.").append(DbContract.Tags._ID)
                .append(" = ct.").append(DbContract.CategoryTags.TAG_ID)
                .append(" WHERE c.").append(DbContract.Categories.NAME).append(" LIKE ?");

        List<String> args = new ArrayList<>();
        args.add(SearchPatterns.like(namePattern));
        if (tagId != null) {
            sql.append(" AND c.").append(DbContract.Categories._ID)
                    .append(" IN (SELECT ").append(DbContract.CategoryTags.CATEGORY_ID)
                    .append(" FROM ").append(DbContract.CategoryTags.TABLE)
                    .append(" WHERE ").append(DbContract.CategoryTags.TAG_ID).append(" = ?)");
            args.add(String.valueOf(tagId));
        }
        sql.append(" GROUP BY c.").append(DbContract.Categories._ID)
                .append(" ORDER BY c.").append(DbContract.Categories.NAME).append(" COLLATE NOCASE ASC");

        List<Category> categories = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase()
                .rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                categories.add(new Category(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        }
        return categories;
    }

    @Nullable
    public Category findById(long id) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.Categories.TABLE,
                new String[]{DbContract.Categories._ID, DbContract.Categories.NAME},
                DbContract.Categories._ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst()
                    ? new Category(cursor.getLong(0), cursor.getString(1), "")
                    : null;
        }
    }

    @NonNull
    public List<Long> tagIdsOf(long categoryId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.CategoryTags.TABLE,
                new String[]{DbContract.CategoryTags.TAG_ID},
                DbContract.CategoryTags.CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    /** @return the new row id, or -1 when the name is already taken */
    public long insert(@NonNull String name, @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DbContract.Categories.NAME, name);
            long id = db.insert(DbContract.Categories.TABLE, null, values);
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

    /** @return true when saved, false when the new name is already taken */
    public boolean update(long id, @NonNull String name, @NonNull List<Long> tagIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DbContract.Categories.NAME, name);
            db.update(DbContract.Categories.TABLE, values,
                    DbContract.Categories._ID + " = ?", new String[]{String.valueOf(id)});
            replaceTags(db, id, tagIds);
            db.setTransactionSuccessful();
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Questions keep existing; the schema clears their category reference. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Categories.TABLE,
                DbContract.Categories._ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int countQuestionsIn(long categoryId) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + DbContract.Questions.TABLE
                        + " WHERE " + DbContract.Questions.CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private void replaceTags(SQLiteDatabase db, long categoryId, List<Long> tagIds) {
        db.delete(DbContract.CategoryTags.TABLE,
                DbContract.CategoryTags.CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)});
        for (Long tagId : tagIds) {
            ContentValues link = new ContentValues();
            link.put(DbContract.CategoryTags.CATEGORY_ID, categoryId);
            link.put(DbContract.CategoryTags.TAG_ID, tagId);
            db.insert(DbContract.CategoryTags.TABLE, null, link);
        }
    }
}
