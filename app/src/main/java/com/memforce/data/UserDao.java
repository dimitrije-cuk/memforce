package com.memforce.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.db.DbContract;
import com.memforce.db.MemForceDbHelper;
import com.memforce.model.User;
import com.memforce.security.PasswordHasher;

public class UserDao {

    private final MemForceDbHelper helper;

    public UserDao(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
    }

    /**
     * Signs the user in, registering them on first use.
     *
     * @return the signed in user, or {@code null} when the name is taken and the password is wrong
     */
    @Nullable
    public User authenticateOrRegister(@NonNull String name, @NonNull String password) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Credentials stored = findCredentials(db, name);
            User user;
            if (stored == null) {
                user = register(db, name, password);
            } else if (PasswordHasher.matches(password, stored.salt, stored.hash)) {
                user = new User(stored.id, stored.name);
            } else {
                return null;
            }
            db.setTransactionSuccessful();
            return user;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    private Credentials findCredentials(SQLiteDatabase db, String name) {
        try (Cursor cursor = db.query(
                DbContract.Users.TABLE,
                new String[]{DbContract.Users._ID, DbContract.Users.NAME,
                        DbContract.Users.PASSWORD_HASH, DbContract.Users.SALT},
                DbContract.Users.NAME + " = ?",
                new String[]{name},
                null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new Credentials(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3));
        }
    }

    @NonNull
    private User register(SQLiteDatabase db, String name, String password) {
        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put(DbContract.Users.NAME, name);
        values.put(DbContract.Users.SALT, salt);
        values.put(DbContract.Users.PASSWORD_HASH, PasswordHasher.hash(password, salt));
        long id = db.insertOrThrow(DbContract.Users.TABLE, null, values);
        return new User(id, name);
    }

    private static final class Credentials {
        final long id;
        final String name;
        final String hash;
        final String salt;

        Credentials(long id, String name, String hash, String salt) {
            this.id = id;
            this.name = name;
            this.hash = hash;
            this.salt = salt;
        }
    }
}
