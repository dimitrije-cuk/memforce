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

    private MemForceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        db.execSQL("CREATE TABLE " + DbContract.Categories.TABLE + " ("
                + DbContract.Categories._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Categories.NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE)");

        // A question outlives its category: deleting a category leaves the question uncategorised.
        db.execSQL("CREATE TABLE " + DbContract.Questions.TABLE + " ("
                + DbContract.Questions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Questions.NAME + " TEXT NOT NULL, "
                + DbContract.Questions.ANSWER + " TEXT, "
                + DbContract.Questions.CATEGORY_ID + " INTEGER REFERENCES "
                + DbContract.Categories.TABLE + "(" + DbContract.Categories._ID + ") ON DELETE SET NULL)");

        db.execSQL("CREATE TABLE " + DbContract.Decks.TABLE + " ("
                + DbContract.Decks._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.Decks.NAME + " TEXT NOT NULL, "
                + DbContract.Decks.USER_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Users.TABLE + "(" + DbContract.Users._ID + ") ON DELETE CASCADE, "
                + "UNIQUE(" + DbContract.Decks.USER_ID + ", " + DbContract.Decks.NAME + " COLLATE NOCASE))");

        db.execSQL("CREATE TABLE " + DbContract.QuestionTags.TABLE + " ("
                + DbContract.QuestionTags.QUESTION_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Questions.TABLE + "(" + DbContract.Questions._ID + ") ON DELETE CASCADE, "
                + DbContract.QuestionTags.TAG_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Tags.TABLE + "(" + DbContract.Tags._ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DbContract.QuestionTags.QUESTION_ID + ", "
                + DbContract.QuestionTags.TAG_ID + "))");

        db.execSQL("CREATE TABLE " + DbContract.CategoryTags.TABLE + " ("
                + DbContract.CategoryTags.CATEGORY_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Categories.TABLE + "(" + DbContract.Categories._ID + ") ON DELETE CASCADE, "
                + DbContract.CategoryTags.TAG_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Tags.TABLE + "(" + DbContract.Tags._ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DbContract.CategoryTags.CATEGORY_ID + ", "
                + DbContract.CategoryTags.TAG_ID + "))");

        db.execSQL("CREATE TABLE " + DbContract.DeckQuestions.TABLE + " ("
                + DbContract.DeckQuestions.DECK_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Decks.TABLE + "(" + DbContract.Decks._ID + ") ON DELETE CASCADE, "
                + DbContract.DeckQuestions.QUESTION_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Questions.TABLE + "(" + DbContract.Questions._ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DbContract.DeckQuestions.DECK_ID + ", "
                + DbContract.DeckQuestions.QUESTION_ID + "))");

        db.execSQL("CREATE INDEX idx_questions_category ON " + DbContract.Questions.TABLE
                + "(" + DbContract.Questions.CATEGORY_ID + ")");
        db.execSQL("CREATE INDEX idx_decks_user ON " + DbContract.Decks.TABLE
                + "(" + DbContract.Decks.USER_ID + ")");
        db.execSQL("CREATE INDEX idx_question_tags_tag ON " + DbContract.QuestionTags.TABLE
                + "(" + DbContract.QuestionTags.TAG_ID + ")");
        db.execSQL("CREATE INDEX idx_category_tags_tag ON " + DbContract.CategoryTags.TABLE
                + "(" + DbContract.CategoryTags.TAG_ID + ")");
        db.execSQL("CREATE INDEX idx_deck_questions_question ON " + DbContract.DeckQuestions.TABLE
                + "(" + DbContract.DeckQuestions.QUESTION_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.DeckQuestions.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.CategoryTags.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.QuestionTags.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Decks.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Questions.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Categories.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Tags.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.Users.TABLE);
        onCreate(db);
    }
}
