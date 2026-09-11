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
import com.memforce.model.Tag;
import com.memforce.model.TagSort;
import com.memforce.model.TagUsage;
import com.memforce.search.SearchQuery;

import java.util.ArrayList;
import java.util.List;

public class TagDao {

    private final MemForceDbHelper helper;

    public TagDao(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
    }

    @NonNull
    public List<Tag> search(@Nullable String namePattern) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Tag> tags = new ArrayList<>();
        try (Cursor cursor = db.query(
                DbContract.Tags.TABLE,
                new String[]{DbContract.Tags._ID, DbContract.Tags.NAME},
                DbContract.Tags.NAME + " LIKE ?",
                new String[]{SearchPatterns.like(namePattern)},
                null, null, DbContract.Tags.NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                tags.add(new Tag(cursor.getLong(0), cursor.getString(1)));
            }
        }
        return tags;
    }

    @Nullable
    public Tag findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Tags.TABLE,
                new String[]{DbContract.Tags._ID, DbContract.Tags.NAME},
                DbContract.Tags._ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? new Tag(cursor.getLong(0), cursor.getString(1)) : null;
        }
    }

    /** @return the tag with this name, matched case insensitively by the column's collation */
    @Nullable
    public Tag findByName(@NonNull String name) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbContract.Tags.TABLE,
                new String[]{DbContract.Tags._ID, DbContract.Tags.NAME},
                DbContract.Tags.NAME + " = ?",
                new String[]{name},
                null, null, null)) {
            return cursor.moveToFirst() ? new Tag(cursor.getLong(0), cursor.getString(1)) : null;
        }
    }

    /**
     * Reads the tags with the number of questions each carries, in the requested order.
     *
     * @param namePattern LIKE pattern applied to the tag name; blank reads every tag
     */
    @NonNull
    public List<TagUsage> searchWithCounts(@Nullable String namePattern, @NonNull TagSort sort) {
        return readUsages(TagQueries.usage(namePattern, sort));
    }

    /** The number of stored tags. */
    public int countAll() {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + DbContract.Tags.TABLE, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    /**
     * Reads the tags worth offering as the next search criterion, most used first.
     *
     * @param limit the greatest number of tags to read
     */
    @NonNull
    public List<TagUsage> suggest(@NonNull SearchQuery query, int limit) {
        return readUsages(TagQueries.suggestions(query, limit));
    }

    @NonNull
    private List<TagUsage> readUsages(@NonNull TagQueries.Statement statement) {
        List<TagUsage> usages = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase()
                .rawQuery(statement.sql(), statement.args())) {
            while (cursor.moveToNext()) {
                usages.add(new TagUsage(cursor.getLong(0), cursor.getString(1), cursor.getInt(2)));
            }
        }
        return usages;
    }

    /** @return the new row id, or -1 when the name is already taken */
    public long insert(@NonNull String name) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Tags.NAME, name);
        return helper.getWritableDatabase().insert(DbContract.Tags.TABLE, null, values);
    }

    /** @return true when the tag was renamed, false when the new name is already taken */
    public boolean update(long id, @NonNull String name) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Tags.NAME, name);
        try {
            return helper.getWritableDatabase().update(
                    DbContract.Tags.TABLE, values,
                    DbContract.Tags._ID + " = ?",
                    new String[]{String.valueOf(id)}) > 0;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    /** Assignments to questions are removed by the schema's cascade rules. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Tags.TABLE,
                DbContract.Tags._ID + " = ?",
                new String[]{String.valueOf(id)});
    }
}
