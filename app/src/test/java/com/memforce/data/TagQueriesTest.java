package com.memforce.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.memforce.model.TagSort;
import com.memforce.search.SearchQuery;

import org.junit.Test;

/** Checks the statements that count tags, rank them, and decide which are worth offering. */
public class TagQueriesTest {

    @Test
    public void countsEveryQuestionCarryingATagForTheTagList() {
        String sql = TagQueries.usage(null, TagSort.MOST_USED).sql();

        assertTrue(sql, sql.contains("(SELECT COUNT(*) FROM question_tags qt"
                + " WHERE qt.tag_id = t._id) AS question_count"));
        // Counted by a sub-select rather than a join, so a tag no question carries is still read.
        assertTrue(sql, sql.contains("FROM tags t WHERE t.name LIKE ?"));
    }

    @Test
    public void readsEveryTagWhenNoNameIsGiven() {
        assertArrayEquals(new String[]{"%"}, TagQueries.usage(null, TagSort.MOST_USED).args());
        assertArrayEquals(new String[]{"%"}, TagQueries.usage("  ", TagSort.MOST_USED).args());
    }

    @Test
    public void passesTheNamePatternThrough() {
        assertArrayEquals(new String[]{"h%"}, TagQueries.usage("h%", TagSort.ALPHABETICAL).args());
    }

    @Test
    public void ordersTheTagListTheThreeWaysOffered() {
        assertEquals("question_count DESC, t.name COLLATE NOCASE ASC",
                TagQueries.orderBy(TagSort.MOST_USED));
        assertEquals("t.name COLLATE NOCASE ASC",
                TagQueries.orderBy(TagSort.ALPHABETICAL));
        assertEquals("question_count ASC, t.name COLLATE NOCASE ASC",
                TagQueries.orderBy(TagSort.LEAST_USED));
    }

    @Test
    public void breaksTiesByNameSoTheOrderIsNeverArbitrary() {
        assertTrue(TagQueries.orderBy(TagSort.MOST_USED).endsWith("t.name COLLATE NOCASE ASC"));
        assertTrue(TagQueries.orderBy(TagSort.LEAST_USED).endsWith("t.name COLLATE NOCASE ASC"));
    }

    @Test
    public void offersOnlyTagsCarriedByAQuestionTheSearchAlreadyFound() {
        TagQueries.Statement statement = TagQueries.suggestions(SearchQuery.empty(), 24);

        assertTrue(statement.sql(), statement.sql().contains(
                "JOIN question_tags qt ON qt.tag_id = t._id WHERE qt.question_id IN (SELECT q._id"));
    }

    @Test
    public void ranksTheSuggestionsByUsageWithinWhatWasFound() {
        TagQueries.Statement statement = TagQueries.suggestions(SearchQuery.empty(), 24);

        assertTrue(statement.sql(), statement.sql().contains("COUNT(*) AS question_count"));
        assertTrue(statement.sql(), statement.sql().contains(
                "ORDER BY question_count DESC, t.name COLLATE NOCASE ASC"));
    }

    @Test
    public void leavesOutTheTagsAlreadyChosen() {
        TagQueries.Statement statement =
                TagQueries.suggestions(SearchQuery.empty().withTag(3L).withTag(8L), 24);

        assertEquals(2, countOf(statement.sql(), "AND t._id <> ?"));
        assertArrayEquals(new String[]{"%", "%", "3", "8", "3", "8", "24"}, statement.args());
    }

    @Test
    public void narrowsTheSuggestionsByTheTextAsWell() {
        TagQueries.Statement statement =
                TagQueries.suggestions(SearchQuery.empty().withText("rev"), 24);

        assertArrayEquals(new String[]{"rev", "rev", "24"}, statement.args());
    }

    @Test
    public void asksForNoMoreSuggestionsThanTheLimit() {
        TagQueries.Statement statement = TagQueries.suggestions(SearchQuery.empty(), 5);

        assertTrue(statement.sql(), statement.sql().endsWith("LIMIT ?"));
        assertEquals("5", statement.args()[statement.args().length - 1]);
    }

    @Test
    public void handsOutTheArgumentsAfresh() {
        TagQueries.Statement statement = TagQueries.usage(null, TagSort.MOST_USED);
        statement.args()[0] = "changed";

        assertEquals("%", statement.args()[0]);
    }

    private static int countOf(String text, String fragment) {
        int count = 0;
        for (int at = text.indexOf(fragment); at >= 0; at = text.indexOf(fragment, at + 1)) {
            count++;
        }
        return count;
    }
}
