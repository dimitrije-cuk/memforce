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

    /** Assignments to questions and categories are removed by the schema's cascade rules. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Tags.TABLE,
                DbContract.Tags._ID + " = ?",
                new String[]{String.valueOf(id)});
    }
}
