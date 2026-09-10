package com.memforce.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/** Creates and opens the on-device SQLite database. */
public final class MemForceDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "memforce.db";
    private static final int DATABASE_VERSION = 1;

    private static MemForceDbHelper instance;

    private final Context context;

    private MemForceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    public static synchronized MemForceDbHelper getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new MemForceDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DbContract.Users.TABLE + " ("
                + DbContract.Users._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Users.NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + DbContract.Users.PASSWORD_HASH + " TEXT NOT NULL, "
                + DbContract.Users.SALT + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + DbContract.Tags.TABLE + " ("
                + DbContract.Tags._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Tags.NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE)");

        db.execSQL("CREATE TABLE " + DbContract.Questions.TABLE + " ("
                + DbContract.Questions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Questions.NAME + " TEXT NOT NULL, "
                + DbContract.Questions.ANSWER + " TEXT)");

        db.execSQL("CREATE TABLE " + DbContract.QuestionTags.TABLE + " ("
                + DbContract.QuestionTags.QUESTION_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Questions.TABLE + "(" + DbContract.Questions._ID + ") ON DELETE CASCADE, "
                + DbContract.QuestionTags.TAG_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Tags.TABLE + "(" + DbContract.Tags._ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DbContract.QuestionTags.QUESTION_ID + ", "
                + DbContract.QuestionTags.TAG_ID + "))");

        db.execSQL("CREATE INDEX idx_question_tags_tag ON " + DbContract.QuestionTags.TABLE
                + "(" + DbContract.QuestionTags.TAG_ID + ")");

        DatabaseSeeder.seed(db, context);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.QuestionTags.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Questions.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Tags.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Users.TABLE);
        onCreate(db);
    }
}
