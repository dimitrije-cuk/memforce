package com.memforce.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.memforce.search.SearchQuery;

import org.junit.Test;

/**
 * Checks the condition {@link QuestionFilter} builds, because it decides what "matching" means for
 * the result list, for the tag names read beside it, and for the tag suggestions.
 */
public class QuestionFilterTest {

    @Test
    public void matchesEverythingWhenNoCriterionIsGiven() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty());

        assertArrayEquals(new String[]{"%", "%"}, filter.args());
    }

    @Test
    public void looksForTheTextInQuestionNamesAndInTagNames() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty().withText("%rev%"));

        // The same pattern is asked of the question text and of the names of the tags it carries.
        assertArrayEquals(new String[]{"%rev%", "%rev%"}, filter.args());
        assertTrue(filter.sql(), filter.sql().startsWith("(q.name LIKE ? OR EXISTS ("));
        assertTrue(filter.sql(), filter.sql().contains("mg.name LIKE ?"));
    }

    @Test
    public void trimsThePatternAndLooksForItAnywhereInTheValue() {
        assertEquals("%rev%", QuestionFilter.of(SearchQuery.empty().withText("  rev  ")).args()[0]);
    }

    @Test
    public void keepsAnUnderscoreTheUserTypedAsASingleCharacterWildcard() {
        assertEquals("%c_t%", QuestionFilter.of(SearchQuery.empty().withText("c_t")).args()[0]);
    }

    @Test
    public void readsTextOfSpacesAsNoCriterion() {
        assertEquals("%", QuestionFilter.of(SearchQuery.empty().withText("   ")).args()[0]);
    }

    @Test
    public void demandsEveryChosenTag() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty().withTag(4L).withTag(9L));

        assertEquals(2, countOf(filter.sql(), " AND EXISTS (SELECT 1 FROM question_tags f"));
        assertArrayEquals(new String[]{"%", "%", "4", "9"}, filter.args());
    }

    @Test
    public void narrowsRatherThanWidensWithEveryFurtherTag() {
        String one = QuestionFilter.of(SearchQuery.empty().withTag(4L)).sql();
        String two = QuestionFilter.of(SearchQuery.empty().withTag(4L).withTag(9L)).sql();

        assertTrue(two.startsWith(one));
        assertEquals(0, countOf(two, " OR EXISTS (SELECT 1 FROM question_tags f"));
    }

    @Test
    public void combinesTheTextWithTheChosenTags() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty().withText("rev").withTag(4L));

        assertArrayEquals(new String[]{"%rev%", "%rev%", "4"}, filter.args());
    }

    @Test
    public void tellsTheChosenTagsApartInTheSameCondition() {
        String sql = QuestionFilter.of(SearchQuery.empty().withTag(4L).withTag(9L)).sql();

        // Each condition uses its own alias, so the two cannot be read as one.
        assertTrue(sql, sql.contains("f0.tag_id = ?"));
        assertTrue(sql, sql.contains("f1.tag_id = ?"));
    }

    @Test
    public void selectsTheMatchingIdentifiersForASubQuery() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty().withTag(4L));

        assertEquals("SELECT q._id FROM questions q WHERE " + filter.sql(), filter.idSelect());
    }

    @Test
    public void handsOutTheArgumentsAfresh() {
        QuestionFilter filter = QuestionFilter.of(SearchQuery.empty());
        filter.args()[0] = "changed";

        assertEquals("%", filter.args()[0]);
    }

    @Test
    public void matchesEverythingWhenAskedForEveryQuestion() {
        assertEquals(QuestionFilter.of(SearchQuery.empty()).sql(), QuestionFilter.all().sql());
    }

    private static int countOf(String text, String fragment) {
        int count = 0;
        for (int at = text.indexOf(fragment); at >= 0; at = text.indexOf(fragment, at + 1)) {
            count++;
        }
        return count;
    }

    private static void assertArrayEquals(String[] expected, String[] actual) {
        org.junit.Assert.assertArrayEquals(
                "expected " + java.util.Arrays.toString(expected)
                        + " but was " + java.util.Arrays.toString(actual),
                expected, actual);
    }
}
