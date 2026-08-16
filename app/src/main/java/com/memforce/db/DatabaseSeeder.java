package com.memforce.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.memforce.security.PasswordHasher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Fills a freshly created database with the demo data described in docs/demo-script.md. */
final class DatabaseSeeder {

    private static final String SEED_ASSET = "seed/memforce_seed.sql";
    private static final String DEMO_PASSWORD = "demo1234";

    private DatabaseSeeder() {
    }

    static void seed(@NonNull SQLiteDatabase db, @NonNull Context context) {
        // The seed file references these ids as deck owners, so they are fixed rather than generated.
        insertUser(db, 1, "ana");
        insertUser(db, 2, "marko");
        execAsset(db, context);
    }

    private static void insertUser(SQLiteDatabase db, long id, String name) {
        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put(DbContract.Users._ID, id);
        values.put(DbContract.Users.NAME, name);
        values.put(DbContract.Users.SALT, salt);
        values.put(DbContract.Users.PASSWORD_HASH, PasswordHasher.hash(DEMO_PASSWORD, salt));
        db.insertOrThrow(DbContract.Users.TABLE, null, values);
    }

    /** Statements end with a semicolon on its own line, so the seed file must not contain one inside a literal. */
    private static void execAsset(SQLiteDatabase db, Context context) {
        StringBuilder statement = new StringBuilder();
        try (InputStream input = context.getAssets().open(SEED_ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.append(trimmed).append(' ');
                if (trimmed.endsWith(";")) {
                    String sql = statement.toString().trim();
                    db.execSQL(sql.substring(0, sql.length() - 1));
                    statement.setLength(0);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + SEED_ASSET, e);
        }
    }
}
