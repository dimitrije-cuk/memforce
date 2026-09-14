package com.memforce.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Checks what {@link AnswerMatcher} forgives and what it does not. */
public class AnswerMatcherTest {

    /**
     * Marking a question that accepts one wording, which most questions are. The game marks every
     * question through {@link AnswerMatcher#matchesAny}, so the single-wording rules of
     * REQ-GAME-80 are checked on that path rather than on one of their own.
     */
    private static boolean isCorrect(String expected, String submitted) {
        return AnswerMatcher.matchesAny(Collections.singletonList(expected), submitted);
    }

    @Test
    public void acceptsTheStoredAnswer() {
        assertTrue(isCorrect("Bismarck", "Bismarck"));
    }

    @Test
    public void ignoresLetterCase() {
        assertTrue(isCorrect("Bismarck", "bIsMaRcK"));
    }

    @Test
    public void ignoresSpacesAroundTheAnswer() {
        assertTrue(isCorrect("Bismarck", "  Bismarck \n"));
    }

    @Test
    public void ignoresRepeatedSpacesInsideTheAnswer() {
        assertTrue(isCorrect("Otto von Bismarck", "Otto   von\tBismarck"));
    }

    @Test
    public void refusesADifferentAnswer() {
        assertFalse(isCorrect("Bismarck", "Metternich"));
    }

    @Test
    public void refusesAnAnswerMissingAWord() {
        assertFalse(isCorrect("Otto von Bismarck", "Otto Bismarck"));
    }

    @Test
    public void refusesAnEmptySubmission() {
        assertFalse(isCorrect("Bismarck", ""));
        assertFalse(isCorrect("Bismarck", "   "));
        assertFalse(isCorrect("Bismarck", null));
    }

    @Test
    public void refusesEverythingWhenNoAnswerIsStored() {
        assertFalse(isCorrect(null, "Bismarck"));
        assertFalse(isCorrect("", "Bismarck"));
        assertFalse(isCorrect("   ", "   "));
    }

    @Test
    public void tellsWhetherSomethingWasSubmitted() {
        assertTrue(AnswerMatcher.isAnswered("x"));
        assertFalse(AnswerMatcher.isAnswered(" \t "));
        assertFalse(AnswerMatcher.isAnswered(null));
    }

    @Test
    public void normalizesToOneComparableForm() {
        assertEquals("otto von bismarck", AnswerMatcher.normalize("  Otto  VON   Bismarck  "));
        assertEquals("", AnswerMatcher.normalize(null));
    }

    @Test
    public void acceptsAnyOfTheAnswersAQuestionCarries() {
        List<String> accepted = Arrays.asList("Four", "4", "IV");

        assertTrue(AnswerMatcher.matchesAny(accepted, "Four"));
        assertTrue(AnswerMatcher.matchesAny(accepted, "4"));
        assertTrue(AnswerMatcher.matchesAny(accepted, " iv "));
    }

    @Test
    public void refusesASubmissionMatchingNoneOfTheAnswers() {
        assertFalse(AnswerMatcher.matchesAny(Arrays.asList("Four", "4"), "Five"));
    }

    @Test
    public void refusesAnEmptySubmissionWhateverTheAnswersAre() {
        List<String> accepted = Arrays.asList("Four", "4");

        assertFalse(AnswerMatcher.matchesAny(accepted, ""));
        assertFalse(AnswerMatcher.matchesAny(accepted, "   "));
        assertFalse(AnswerMatcher.matchesAny(accepted, null));
    }

    @Test
    public void refusesEverythingWhenNoAnswerIsAccepted() {
        assertFalse(AnswerMatcher.matchesAny(Collections.<String>emptyList(), "Four"));
        assertFalse(AnswerMatcher.matchesAny(Arrays.asList("", "  "), "Four"));
    }
}
