package com.memforce.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/** Creates, opens and migrates the on-device SQLite database. */
public final class MemForceDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "memforce.db";

    /**
     * 1 — the schema of baseline BL-1.<br>
     * 2 — adds {@code alternative_answers}.
     */
    private static final int DATABASE_VERSION = 2;

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

        createAlternativeAnswers(db);

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

    /**
     * Brings an existing database up to {@value #DATABASE_VERSION} one version at a time, so that
     * an installation's questions, tags, assignments and accounts survive the upgrade. A version
     * added later adds a step here; none of them may drop a table that holds a user's work.
     *
     * @throws IllegalStateException when no step is known for a version, which would otherwise
     *                               leave the database silently short of the expected schema
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            if (version == 2) {
                createAlternativeAnswers(db);
            } else {
                throw new IllegalStateException(
                        "No migration to database version " + version);
            }
        }
    }

    /**
     * The unique index is what stops the same wording being stored twice for one question; it
     * compares without regard to letter case, exactly as the game marks a submission.
     */
    private static void createAlternativeAnswers(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DbContract.AlternativeAnswers.TABLE + " ("
                + DbContract.AlternativeAnswers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.AlternativeAnswers.QUESTION_ID + " INTEGER NOT NULL REFERENCES "
                + DbContract.Questions.TABLE + "(" + DbContract.Questions._ID + ") ON DELETE CASCADE, "
                + DbContract.AlternativeAnswers.ANSWER + " TEXT NOT NULL)");

        db.execSQL("CREATE UNIQUE INDEX idx_alternative_answers_answer ON "
                + DbContract.AlternativeAnswers.TABLE
                + "(" + DbContract.AlternativeAnswers.QUESTION_ID + ", "
                + DbContract.AlternativeAnswers.ANSWER + " COLLATE NOCASE)");
    }
}
