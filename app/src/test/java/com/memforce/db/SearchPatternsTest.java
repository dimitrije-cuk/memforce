package com.memforce.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Checks the one place that turns what a user types into a {@code LIKE} argument, because it
 * decides what an ordinary search finds on every screen that offers one.
 */
public class SearchPatternsTest {

    @Test
    public void looksForTheTypedTextAnywhereInTheValue() {
        assertEquals("%history%", SearchPatterns.like("history"));
        assertEquals("%world war%", SearchPatterns.like("world war"));
    }

    @Test
    public void matchesEverythingWhenNothingIsTyped() {
        assertEquals("%", SearchPatterns.like(null));
        assertEquals("%", SearchPatterns.like(""));
        assertEquals("%", SearchPatterns.like("   "));
    }

    @Test
    public void trimsTheSurroundingSpacesBeforeWrapping() {
        assertEquals("%history%", SearchPatterns.like("  history  "));
    }

    @Test
    public void doublesNoWildcardTheUserTypedAtEitherEnd() {
        assertEquals("%history%", SearchPatterns.like("%history"));
        assertEquals("%history%", SearchPatterns.like("history%"));
        assertEquals("%history%", SearchPatterns.like("%history%"));
        assertEquals("%history%", SearchPatterns.like("%%history%%"));
    }

    @Test
    public void readsATextOfWildcardsAloneAsNoCriterion() {
        assertEquals("%", SearchPatterns.like("%"));
        assertEquals("%", SearchPatterns.like("%%"));
    }

    @Test
    public void keepsAWildcardTypedInsideTheTextWhereItStands() {
        assertEquals("%world%war%", SearchPatterns.like("world%war"));
    }

    @Test
    public void neitherInsertsAnUnderscoreNorAltersOne() {
        assertEquals("%c_t%", SearchPatterns.like("c_t"));
        assertEquals("%_at%", SearchPatterns.like("_at"));
    }
}
