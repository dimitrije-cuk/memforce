package com.memforce.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Checks what {@link AnswerMatcher} forgives and what it does not. */
public class AnswerMatcherTest {

    @Test
    public void acceptsTheStoredAnswer() {
        assertTrue(AnswerMatcher.matches("Bismarck", "Bismarck"));
    }

    @Test
    public void ignoresLetterCase() {
        assertTrue(AnswerMatcher.matches("Bismarck", "bIsMaRcK"));
    }

    @Test
    public void ignoresSpacesAroundTheAnswer() {
        assertTrue(AnswerMatcher.matches("Bismarck", "  Bismarck \n"));
    }

    @Test
    public void ignoresRepeatedSpacesInsideTheAnswer() {
        assertTrue(AnswerMatcher.matches("Otto von Bismarck", "Otto   von\tBismarck"));
    }

    @Test
    public void refusesADifferentAnswer() {
        assertFalse(AnswerMatcher.matches("Bismarck", "Metternich"));
    }

    @Test
    public void refusesAnAnswerMissingAWord() {
        assertFalse(AnswerMatcher.matches("Otto von Bismarck", "Otto Bismarck"));
    }

    @Test
    public void refusesAnEmptySubmission() {
        assertFalse(AnswerMatcher.matches("Bismarck", ""));
        assertFalse(AnswerMatcher.matches("Bismarck", "   "));
        assertFalse(AnswerMatcher.matches("Bismarck", null));
    }

    @Test
    public void refusesEverythingWhenNoAnswerIsStored() {
        assertFalse(AnswerMatcher.matches(null, "Bismarck"));
        assertFalse(AnswerMatcher.matches("", "Bismarck"));
        assertFalse(AnswerMatcher.matches("   ", "   "));
    }

    @Test
    public void tellsWhetherSomethingWasSubmitted() {
        assertTrue(AnswerMatcher.isAnswered("x"));
        assertFalse(AnswerMatcher.isAnswered(" \t "));
        assertFalse(AnswerMatcher.isAnswered(null));
    }

    @Test
    public void tellsWhetherAQuestionCanBeMarkedAtAll() {
        assertTrue(AnswerMatcher.isAnswerable("Bismarck"));
        assertFalse(AnswerMatcher.isAnswerable("  "));
        assertFalse(AnswerMatcher.isAnswerable(null));
    }

    @Test
    public void normalizesToOneComparableForm() {
        assertEquals("otto von bismarck", AnswerMatcher.normalize("  Otto  VON   Bismarck  "));
        assertEquals("", AnswerMatcher.normalize(null));
    }
}
