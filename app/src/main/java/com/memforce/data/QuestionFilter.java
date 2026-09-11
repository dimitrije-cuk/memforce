package com.memforce.data;

import androidx.annotation.NonNull;

import com.memforce.db.DbContract;
import com.memforce.db.SearchPatterns;
import com.memforce.search.SearchQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a {@link SearchQuery} into the SQL condition that selects the matching questions.
 *
 * <p>The condition is written against a question table aliased {@code q}, so the same fragment
 * serves the query that reads the questions, the query that reads their tag names, and the query
 * that ranks the tag suggestions. Keeping one translation means those three can never disagree
 * about what "matching" means.
 *
 * <p>The text is matched against the question text <em>or</em> the name of any tag the question
 * carries, which is what puts title matches and tag matches into one result set. Each chosen tag
 * adds a further condition, so choosing more tags narrows the result rather than widening it.
 */
public final class QuestionFilter {

    private final String sql;
    private final String[] args;

    private QuestionFilter(@NonNull String sql, @NonNull List<String> args) {
        this.sql = sql;
        this.args = args.toArray(new String[0]);
    }

    @NonNull
    public static QuestionFilter of(@NonNull SearchQuery query) {
        String pattern = SearchPatterns.like(query.getText());
        List<String> args = new ArrayList<>();

        StringBuilder sql = new StringBuilder("(q.").append(DbContract.Questions.NAME)
                .append(" LIKE ? OR EXISTS (SELECT 1 FROM ").append(DbContract.QuestionTags.TABLE)
                .append(" mt JOIN ").append(DbContract.Tags.TABLE).append(" mg ON mg.")
                .append(DbContract.Tags._ID).append(" = mt.").append(DbContract.QuestionTags.TAG_ID)
                .append(" WHERE mt.").append(DbContract.QuestionTags.QUESTION_ID).append(" = q.")
                .append(DbContract.Questions._ID).append(" AND mg.").append(DbContract.Tags.NAME)
                .append(" LIKE ?))");
        args.add(pattern);
        args.add(pattern);

        List<Long> tagIds = query.getTagIds();
        for (int i = 0; i < tagIds.size(); i++) {
            sql.append(" AND EXISTS (SELECT 1 FROM ").append(DbContract.QuestionTags.TABLE)
                    .append(" f").append(i)
                    .append(" WHERE f").append(i).append('.')
                    .append(DbContract.QuestionTags.QUESTION_ID).append(" = q.")
                    .append(DbContract.Questions._ID)
                    .append(" AND f").append(i).append('.')
                    .append(DbContract.QuestionTags.TAG_ID).append(" = ?)");
            args.add(String.valueOf(tagIds.get(i)));
        }
        return new QuestionFilter(sql.toString(), args);
    }

    /** Selects every question, used where a filter is structurally required but none is wanted. */
    @NonNull
    public static QuestionFilter all() {
        return of(SearchQuery.empty());
    }

    /** The condition, without the {@code WHERE} keyword. */
    @NonNull
    public String sql() {
        return sql;
    }

    /** A fresh array each call, because a statement consumes the arguments it is given. */
    @NonNull
    public String[] args() {
        return args.clone();
    }

    /** The sub-select yielding the identifiers of the matching questions. */
    @NonNull
    public String idSelect() {
        return "SELECT q." + DbContract.Questions._ID
                + " FROM " + DbContract.Questions.TABLE + " q"
                + " WHERE " + sql;
    }
}
