package com.memforce.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Checks the criteria {@link SearchQuery} carries between one change and the next. */
public class SearchQueryTest {

    @Test
    public void startsWithNoCriterion() {
        SearchQuery query = SearchQuery.empty();

        assertEquals("", query.getText());
        assertEquals(Collections.emptyList(), query.getTagIds());
        assertTrue(query.isEmpty());
    }

    @Test
    public void treatsTextOfSpacesAsNoCriterion() {
        assertTrue(SearchQuery.empty().withText("   ").isEmpty());
    }

    @Test
    public void isNotEmptyWithATagEvenWithoutText() {
        assertFalse(SearchQuery.empty().withTag(7L).isEmpty());
    }

    @Test
    public void keepsTheOrderTagsWereChosenIn() {
        SearchQuery query = SearchQuery.empty().withTag(3L).withTag(1L).withTag(2L);

        assertEquals(Arrays.asList(3L, 1L, 2L), query.getTagIds());
    }

    @Test
    public void choosesATagOnlyOnce() {
        SearchQuery query = SearchQuery.empty().withTag(3L).withTag(3L);

        assertEquals(Collections.singletonList(3L), query.getTagIds());
    }

    @Test
    public void leavesTheValueAloneWhenNothingChanges() {
        SearchQuery query = SearchQuery.empty().withTag(3L);

        assertSame(query, query.withTag(3L));
        assertSame(query, query.withoutTag(9L));
        assertSame(query, query.withText(""));
    }

    @Test
    public void dropsAChosenTag() {
        SearchQuery query = SearchQuery.empty().withTag(3L).withTag(4L).withoutTag(3L);

        assertEquals(Collections.singletonList(4L), query.getTagIds());
        assertFalse(query.hasTag(3L));
    }

    @Test
    public void togglesATagInAndOut() {
        SearchQuery chosen = SearchQuery.empty().toggleTag(5L);
        assertTrue(chosen.hasTag(5L));
        assertFalse(chosen.toggleTag(5L).hasTag(5L));
    }

    @Test
    public void keepsTheTagsWhenTheTextChanges() {
        SearchQuery query = SearchQuery.empty().withTag(1L).withText("rev");

        assertEquals("rev", query.getText());
        assertEquals(Collections.singletonList(1L), query.getTagIds());
    }

    @Test
    public void readsNullTextAsNoText() {
        assertEquals("", SearchQuery.empty().withText("rev").withText(null).getText());
    }

    @Test
    public void leavesTheValueItWasMadeFromUnchanged() {
        SearchQuery first = SearchQuery.empty().withTag(1L);
        SearchQuery second = first.withTag(2L);

        assertEquals(Collections.singletonList(1L), first.getTagIds());
        assertEquals(Arrays.asList(1L, 2L), second.getTagIds());
    }

    @Test
    public void comparesByTextAndTags() {
        SearchQuery query = SearchQuery.empty().withText("a").withTag(1L);

        assertEquals(SearchQuery.empty().withText("a").withTag(1L), query);
        assertEquals(SearchQuery.empty().withText("a").withTag(1L).hashCode(), query.hashCode());
        assertNotEquals(SearchQuery.empty().withText("a").withTag(2L), query);
        assertNotEquals(SearchQuery.empty().withText("b").withTag(1L), query);
    }
}
