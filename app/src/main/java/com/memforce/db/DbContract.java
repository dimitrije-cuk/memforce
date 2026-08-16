package com.memforce.db;

import android.provider.BaseColumns;

/** Table and column names of the MemForce SQLite database. */
public final class DbContract {

    private DbContract() {
    }

    public static final class Users implements BaseColumns {
        public static final String TABLE = "users";
        public static final String NAME = "name";
        public static final String PASSWORD_HASH = "password_hash";
        public static final String SALT = "salt";

        private Users() {
        }
    }

    public static final class Tags implements BaseColumns {
        public static final String TABLE = "tags";
        public static final String NAME = "name";

        private Tags() {
        }
    }

    public static final class Categories implements BaseColumns {
        public static final String TABLE = "categories";
        public static final String NAME = "name";

        private Categories() {
        }
    }

    public static final class Questions implements BaseColumns {
        public static final String TABLE = "questions";
        public static final String NAME = "name";
        public static final String ANSWER = "answer";
        public static final String CATEGORY_ID = "category_id";

        private Questions() {
        }
    }

    public static final class Decks implements BaseColumns {
        public static final String TABLE = "decks";
        public static final String NAME = "name";
        public static final String USER_ID = "user_id";

        private Decks() {
        }
    }

    /** Question to tag assignment; a question may carry any number of tags. */
    public static final class QuestionTags {
        public static final String TABLE = "question_tags";
        public static final String QUESTION_ID = "question_id";
        public static final String TAG_ID = "tag_id";

        private QuestionTags() {
        }
    }

    /** Category to tag assignment; supports searching categories by tag. */
    public static final class CategoryTags {
        public static final String TABLE = "category_tags";
        public static final String CATEGORY_ID = "category_id";
        public static final String TAG_ID = "tag_id";

        private CategoryTags() {
        }
    }

    /** Questions collected into a deck. */
    public static final class DeckQuestions {
        public static final String TABLE = "deck_questions";
        public static final String DECK_ID = "deck_id";
        public static final String QUESTION_ID = "question_id";

        private DeckQuestions() {
        }
    }
}
