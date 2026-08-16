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
import com.memforce.model.Deck;

import java.util.ArrayList;
import java.util.List;

/** Decks are private, so every statement here is scoped to the signed in user. */
public class DeckDao {

    private final MemForceDbHelper helper;
    private final long userId;

    public DeckDao(@NonNull Context context, long userId) {
        this.helper = MemForceDbHelper.getInstance(context);
        this.userId = userId;
    }

    @NonNull
    public List<Deck> search(@Nullable String namePattern) {
        String sql = "SELECT d." + DbContract.Decks._ID
                + ", d." + DbContract.Decks.NAME
                + ", COUNT(dq." + DbContract.DeckQuestions.QUESTION_ID + ")"
                + " FROM " + DbContract.Decks.TABLE + " d"
                + " LEFT JOIN " + DbContract.DeckQuestions.TABLE + " dq"
                + " ON dq." + DbContract.DeckQuestions.DECK_ID + " = d." + DbContract.Decks._ID
                + " WHERE d." + DbContract.Decks.USER_ID + " = ?"
                + " AND d." + DbContract.Decks.NAME + " LIKE ?"
                + " GROUP BY d." + DbContract.Decks._ID
                + " ORDER BY d." + DbContract.Decks.NAME + " COLLATE NOCASE ASC";

        List<Deck> decks = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(userId), SearchPatterns.like(namePattern)})) {
            while (cursor.moveToNext()) {
                decks.add(new Deck(cursor.getLong(0), cursor.getString(1), cursor.getInt(2)));
            }
        }
        return decks;
    }

    @Nullable
    public Deck findById(long id) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.Decks.TABLE,
                new String[]{DbContract.Decks._ID, DbContract.Decks.NAME},
                DbContract.Decks._ID + " = ? AND " + DbContract.Decks.USER_ID + " = ?",
                new String[]{String.valueOf(id), String.valueOf(userId)},
                null, null, null)) {
            return cursor.moveToFirst() ? new Deck(cursor.getLong(0), cursor.getString(1), 0) : null;
        }
    }

    @NonNull
    public List<Long> questionIdsOf(long deckId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                DbContract.DeckQuestions.TABLE,
                new String[]{DbContract.DeckQuestions.QUESTION_ID},
                DbContract.DeckQuestions.DECK_ID + " = ?",
                new String[]{String.valueOf(deckId)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    /** @return the new row id, or -1 when this user already has a deck with that name */
    public long insert(@NonNull String name, @NonNull List<Long> questionIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DbContract.Decks.NAME, name);
            values.put(DbContract.Decks.USER_ID, userId);
            long id = db.insert(DbContract.Decks.TABLE, null, values);
            if (id == -1) {
                return -1;
            }
            replaceQuestions(db, id, questionIds);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    /** @return true when saved, false when this user already has a deck with that name */
    public boolean update(long id, @NonNull String name, @NonNull List<Long> questionIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DbContract.Decks.NAME, name);
            int updated = db.update(DbContract.Decks.TABLE, values,
                    DbContract.Decks._ID + " = ? AND " + DbContract.Decks.USER_ID + " = ?",
                    new String[]{String.valueOf(id), String.valueOf(userId)});
            if (updated == 0) {
                return false;
            }
            replaceQuestions(db, id, questionIds);
            db.setTransactionSuccessful();
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Questions are shared, so only the deck and its membership rows are removed. */
    public void delete(long id) {
        helper.getWritableDatabase().delete(
                DbContract.Decks.TABLE,
                DbContract.Decks._ID + " = ? AND " + DbContract.Decks.USER_ID + " = ?",
                new String[]{String.valueOf(id), String.valueOf(userId)});
    }

    private void replaceQuestions(SQLiteDatabase db, long deckId, List<Long> questionIds) {
        db.delete(DbContract.DeckQuestions.TABLE,
                DbContract.DeckQuestions.DECK_ID + " = ?",
                new String[]{String.valueOf(deckId)});
        for (Long questionId : questionIds) {
            ContentValues link = new ContentValues();
            link.put(DbContract.DeckQuestions.DECK_ID, deckId);
            link.put(DbContract.DeckQuestions.QUESTION_ID, questionId);
            db.insert(DbContract.DeckQuestions.TABLE, null, link);
        }
    }
}
