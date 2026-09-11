package com.memforce.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.db.DbContract;
import com.memforce.db.SearchPatterns;
import com.memforce.model.TagSort;
import com.memforce.search.SearchQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The statements that read tags together with the number of questions counted for them.
 *
 * <p>Building the text apart from running it keeps the two counts this application shows honest
 * about what they mean. The tag list counts every question carrying the tag, because that is what
 * a user managing tags wants to know. The suggestions count only the questions the current search
 * already found, because that is what tells the user how much choosing the tag would narrow the
 * result, and because it is the same test that decides whether the tag is worth offering at all.
 */
public final class TagQueries {

    /** A statement and the arguments it is run with. */
    public static final class Statement {

        private final String sql;
        private final String[] args;

        Statement(@NonNull String sql, @NonNull List<String> args) {
            this.sql = sql;
            this.args = args.toArray(new String[0]);
        }

        @NonNull
        public String sql() {
            return sql;
        }

        @NonNull
        public String[] args() {
            return args.clone();
        }
    }

    private TagQueries() {
    }

    /**
     * Reads every tag whose name matches, with the number of questions carrying it. A tag no
     * question carries is read too, with a count of zero, because a tag list that hid them would
     * hide exactly the tags worth tidying up.
     */
    @NonNull
    public static Statement usage(@Nullable String namePattern, @NonNull TagSort sort) {
        String sql = "SELECT t." + DbContract.Tags._ID
                + ", t." + DbContract.Tags.NAME
                + ", (SELECT COUNT(*) FROM " + DbContract.QuestionTags.TABLE + " qt"
                + " WHERE qt." + DbContract.QuestionTags.TAG_ID + " = t." + DbContract.Tags._ID
                + ") AS question_count"
                + " FROM " + DbContract.Tags.TABLE + " t"
                + " WHERE t." + DbContract.Tags.NAME + " LIKE ?"
                + " ORDER BY " + orderBy(sort);
        return new Statement(sql, Collections.singletonList(SearchPatterns.like(namePattern)));
    }

    /**
     * Reads the tags worth offering as the next criterion, most used first.
     *
     * <p>A tag is only read when a question the criteria already select carries it, so a tag with
     * no question in common with what is chosen cannot be offered. Tags already chosen are left
     * out, since choosing one twice would narrow nothing.
     */
    @NonNull
    public static Statement suggestions(@NonNull SearchQuery query, int limit) {
        QuestionFilter filter = QuestionFilter.of(query);
        List<String> args = new ArrayList<>();
        Collections.addAll(args, filter.args());

        StringBuilder sql = new StringBuilder("SELECT t.")
                .append(DbContract.Tags._ID).append(", t.").append(DbContract.Tags.NAME)
                .append(", COUNT(*) AS question_count")
                .append(" FROM ").append(DbContract.Tags.TABLE).append(" t")
                .append(" JOIN ").append(DbContract.QuestionTags.TABLE).append(" qt")
                .append(" ON qt.").append(DbContract.QuestionTags.TAG_ID)
                .append(" = t.").append(DbContract.Tags._ID)
                .append(" WHERE qt.").append(DbContract.QuestionTags.QUESTION_ID)
                .append(" IN (").append(filter.idSelect()).append(')');

        for (Long chosen : query.getTagIds()) {
            sql.append(" AND t.").append(DbContract.Tags._ID).append(" <> ?");
            args.add(String.valueOf(chosen));
        }
        sql.append(" GROUP BY t.").append(DbContract.Tags._ID)
                .append(", t.").append(DbContract.Tags.NAME)
                .append(" ORDER BY ").append(orderBy(TagSort.MOST_USED))
                .append(" LIMIT ?");
        args.add(String.valueOf(limit));

        return new Statement(sql.toString(), args);
    }

    @NonNull
    static String orderBy(@NonNull TagSort sort) {
        String byName = "t." + DbContract.Tags.NAME + " COLLATE NOCASE ASC";
        switch (sort) {
            case MOST_USED:
                return "question_count DESC, " + byName;
            case LEAST_USED:
                return "question_count ASC, " + byName;
            case ALPHABETICAL:
            default:
                return byName;
        }
    }
}
