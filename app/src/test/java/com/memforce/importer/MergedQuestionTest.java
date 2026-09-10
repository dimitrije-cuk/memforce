package com.memforce.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Checks how {@link MergedQuestion} folds a question set into the questions to store. */
public class MergedQuestionTest {

    @Test
    public void putsSetTagsBeforeQuestionTags() throws Exception {
        List<MergedQuestion> merged = mergeAll("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\",\"world-history\"],"
                + "\"questions\":[{\"question\":\"Q\",\"tags\":[\"france\"]}]}");

        assertEquals(1, merged.size());
        assertEquals(Arrays.asList("history", "world-history", "france"),
                merged.get(0).getTags());
    }

    @Test
    public void dropsSetTagRepeatedByAQuestion() throws Exception {
        List<MergedQuestion> merged = mergeAll("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\"],"
                + "\"questions\":[{\"question\":\"Q\",\"tags\":[\"History\"]}]}");

        assertEquals(Arrays.asList("history"), merged.get(0).getTags());
    }

    @Test
    public void keepsQuestionsWithoutTags() throws Exception {
        List<MergedQuestion> merged = mergeAll(
                "{\"formatVersion\":\"1.0\",\"questions\":[{\"question\":\"Q\"}]}");

        assertEquals(1, merged.size());
        assertEquals(0, merged.get(0).getTags().size());
        assertNull(merged.get(0).getAnswer());
    }

    @Test
    public void keepsTheOrderOfTheFile() throws Exception {
        List<MergedQuestion> merged = mergeAll("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{\"question\":\"Second\"},{\"question\":\"First\"}]}");

        assertEquals("Second", merged.get(0).getQuestion());
        assertEquals("First", merged.get(1).getQuestion());
    }

    @Test
    public void foldsAQuestionRepeatedInOneFile() throws Exception {
        List<MergedQuestion> merged = mergeAll("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\"],"
                + "\"questions\":["
                + "{\"question\":\"Who unified Germany?\",\"tags\":[\"19th-century\"]},"
                + "{\"question\":\"WHO UNIFIED GERMANY?\",\"answer\":\"Bismarck\",\"tags\":[\"prussia\"]}]}");

        assertEquals(1, merged.size());
        assertEquals("Who unified Germany?", merged.get(0).getQuestion());
        assertEquals("Bismarck", merged.get(0).getAnswer());
        assertEquals(Arrays.asList("history", "19th-century", "prussia"), merged.get(0).getTags());
    }

    @Test
    public void keepsTheFirstAnswerOfARepeatedQuestion() throws Exception {
        List<MergedQuestion> merged = mergeAll("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":["
                + "{\"question\":\"Q\",\"answer\":\"First\"},"
                + "{\"question\":\"q\",\"answer\":\"Second\"}]}");

        assertEquals(1, merged.size());
        assertEquals("First", merged.get(0).getAnswer());
    }

    private static List<MergedQuestion> mergeAll(String json) throws QuestionSetFormatException {
        return MergedQuestion.mergeAll(QuestionSetParser.parse(json));
    }
}
