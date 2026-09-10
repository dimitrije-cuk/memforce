package com.memforce.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.memforce.importer.ValidationError.Reason;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Checks {@link QuestionSetParser} against the rules of docs/schemas/question-set.schema.json. */
public class QuestionSetParserTest {

    private static final String VALID = "{"
            + "\"formatVersion\":\"1.0\","
            + "\"tags\":[\"history\"],"
            + "\"questions\":[{\"question\":\"Q\",\"answer\":\"A\"}]}";

    @Test
    public void parsesShippedTemplate() throws Exception {
        QuestionSet set = QuestionSetParser.parse(readTemplate("question-set-template.json"));

        assertEquals(QuestionSetParser.SUPPORTED_FORMAT_VERSION, set.getFormatVersion());
        assertEquals(2, set.getQuestions().size());
        assertEquals(2, set.getTags().size());
    }

    @Test
    public void parsesShippedExample() throws Exception {
        QuestionSet set = QuestionSetParser.parse(readTemplate("question-set-example.json"));

        assertEquals("World history basics", set.getName());
        assertNotNull(set.getDescription());
        assertEquals(Arrays.asList("history", "world-history"), set.getTags());
        assertEquals(3, set.getQuestions().size());

        QuestionSetEntry first = set.getQuestions().get(0);
        assertEquals("Who was the first emperor of Rome?", first.getQuestion());
        assertEquals("Augustus", first.getAnswer());
        assertEquals(Arrays.asList("ancient-history", "1st-century-bc"), first.getTags());
    }

    @Test
    public void keepsSetAndQuestionTagsApart() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\"],"
                + "\"questions\":[{\"question\":\"Q\",\"tags\":[\"france\"]}]}");

        assertEquals(Arrays.asList("history"), set.getTags());
        assertEquals(Arrays.asList("france"), set.getQuestions().get(0).getTags());
    }

    @Test
    public void trimsTextAndDropsOptionalFieldsLeftBlank() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"description\":\"  \","
                + "\"questions\":[{\"question\":\"  Q  \",\"answer\":\"  A  \"}]}");

        assertNull(set.getName());
        assertNull(set.getDescription());
        assertEquals("Q", set.getQuestions().get(0).getQuestion());
        assertEquals("A", set.getQuestions().get(0).getAnswer());
        assertTrue(set.getQuestions().get(0).getTags().isEmpty());
    }

    @Test
    public void acceptsOmittedNullAndBlankAnswer() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":["
                + "{\"question\":\"Q1\"},"
                + "{\"question\":\"Q2\",\"answer\":null},"
                + "{\"question\":\"Q3\",\"answer\":\"\"}]}");

        assertEquals(3, set.getQuestions().size());
        for (QuestionSetEntry entry : set.getQuestions()) {
            assertNull(entry.getAnswer());
        }
    }

    @Test
    public void acceptsQuestionTagRepeatingSetTag() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\"],"
                + "\"questions\":[{\"question\":\"Q\",\"tags\":[\"history\"]}]}");

        assertEquals(Arrays.asList("history"), set.getQuestions().get(0).getTags());
    }

    @Test
    public void acceptsFieldsAtTheirLimits() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"name\":\"" + repeat('n', QuestionSetParser.MAX_NAME_LENGTH) + "\","
                + "\"tags\":[\"" + repeat('t', QuestionSetParser.MAX_TAG_LENGTH) + "\"],"
                + "\"questions\":[{"
                + "\"question\":\"" + repeat('q', QuestionSetParser.MAX_QUESTION_LENGTH) + "\","
                + "\"answer\":\"" + repeat('a', QuestionSetParser.MAX_ANSWER_LENGTH) + "\"}]}");

        assertEquals(QuestionSetParser.MAX_NAME_LENGTH, set.getName().length());
        assertEquals(QuestionSetParser.MAX_QUESTION_LENGTH,
                set.getQuestions().get(0).getQuestion().length());
    }

    @Test
    public void skipsByteOrderMark() throws Exception {
        assertEquals(1, QuestionSetParser.parse('\uFEFF' + VALID).getQuestions().size());
    }

    @Test
    public void acceptsWhitespaceAfterTheJsonObject() throws Exception {
        assertEquals(1, QuestionSetParser.parse(VALID + "\r\n\n  ").getQuestions().size());
    }

    @Test
    public void stripsNonBreakingSpaceAroundText() throws Exception {
        QuestionSet set = QuestionSetParser.parse("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{\"question\":\"\\u00a0Q\\u00a0\",\"answer\":\"\\u00a0A\\u00a0\"}]}");

        assertEquals("Q", set.getQuestions().get(0).getQuestion());
        assertEquals("A", set.getQuestions().get(0).getAnswer());
    }

    @Test
    public void rejectsContentAfterTheJsonObject() {
        assertReasons(VALID + " and now some prose", Reason.MALFORMED_JSON);
    }

    @Test
    public void rejectsASecondQuestionSetInTheSameFile() {
        assertReasons(VALID + VALID, Reason.MALFORMED_JSON);
    }

    @Test
    public void rejectsTagPaddedWithANonBreakingSpace() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"\\u00a0history\"],"
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.BLANK);
    }

    @Test
    public void rejectsTagSpanningTwoLines() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"his\\ntory\"],"
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.NOT_ONE_LINE);
    }

    @Test
    public void rejectsQuestionOfNonBreakingSpacesOnly() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{\"question\":\"\\u00a0\\u00a0\"}]}", Reason.BLANK);
    }

    @Test
    public void rejectsMalformedJson() {
        assertReasons("{\"formatVersion\":", Reason.MALFORMED_JSON);
    }

    @Test
    public void rejectsRootThatIsNotAnObject() {
        assertReasons("[" + VALID + "]", Reason.NOT_AN_OBJECT);
    }

    @Test
    public void rejectsMissingFormatVersion() {
        assertReasons("{\"questions\":[{\"question\":\"Q\"}]}", Reason.MISSING_FIELD);
    }

    @Test
    public void rejectsUnsupportedFormatVersion() {
        List<ValidationError> errors = assertReasons(
                "{\"formatVersion\":\"2.0\",\"questions\":[{\"question\":\"Q\"}]}",
                Reason.UNSUPPORTED_VERSION);

        assertEquals("2.0", errors.get(0).getDetail());
    }

    @Test
    public void rejectsFormatVersionThatIsNotAString() {
        assertReasons("{\"formatVersion\":1.0,\"questions\":[{\"question\":\"Q\"}]}",
                Reason.WRONG_TYPE);
    }

    @Test
    public void rejectsUnknownTopLevelField() {
        List<ValidationError> errors = assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"difficulty\":\"hard\","
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.UNKNOWN_FIELD);

        assertEquals("difficulty", errors.get(0).getLocation());
    }

    @Test
    public void rejectsUnknownQuestionField() {
        List<ValidationError> errors = assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{\"question\":\"Q\",\"source\":\"a book\"}]}",
                Reason.UNKNOWN_FIELD);

        assertEquals("questions[0].source", errors.get(0).getLocation());
    }

    @Test
    public void rejectsMissingQuestions() {
        assertReasons("{\"formatVersion\":\"1.0\"}", Reason.MISSING_FIELD);
    }

    @Test
    public void rejectsEmptyQuestions() {
        assertReasons("{\"formatVersion\":\"1.0\",\"questions\":[]}", Reason.NO_QUESTIONS);
    }

    @Test
    public void rejectsQuestionThatIsNotAnObject() {
        assertReasons("{\"formatVersion\":\"1.0\",\"questions\":[\"Q\"]}", Reason.WRONG_TYPE);
    }

    @Test
    public void rejectsMissingQuestionText() {
        List<ValidationError> errors = assertReasons(
                "{\"formatVersion\":\"1.0\",\"questions\":[{\"answer\":\"A\"}]}",
                Reason.MISSING_FIELD);

        assertEquals("questions[0].question", errors.get(0).getLocation());
    }

    @Test
    public void rejectsBlankQuestionText() {
        assertReasons("{\"formatVersion\":\"1.0\",\"questions\":[{\"question\":\"   \"}]}",
                Reason.BLANK);
    }

    @Test
    public void rejectsOverLongQuestionAndAnswer() {
        List<ValidationError> errors = assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{"
                + "\"question\":\"" + repeat('q', QuestionSetParser.MAX_QUESTION_LENGTH + 1) + "\","
                + "\"answer\":\"" + repeat('a', QuestionSetParser.MAX_ANSWER_LENGTH + 1) + "\"}]}",
                Reason.TOO_LONG, Reason.TOO_LONG);

        assertEquals(QuestionSetParser.MAX_QUESTION_LENGTH, errors.get(0).getLimit());
        assertEquals(QuestionSetParser.MAX_ANSWER_LENGTH, errors.get(1).getLimit());
    }

    @Test
    public void rejectsEmptyName() {
        assertReasons("{\"formatVersion\":\"1.0\",\"name\":\"\","
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.BLANK);
    }

    @Test
    public void rejectsDuplicateTagsInOneArray() {
        List<ValidationError> errors = assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"history\",\"history\"],"
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.DUPLICATE_TAG);

        assertEquals("tags[1]", errors.get(0).getLocation());
        assertEquals("history", errors.get(0).getDetail());
    }

    @Test
    public void rejectsBlankAndPaddedTags() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[\"\",\"  \",\" history\"],"
                + "\"questions\":[{\"question\":\"Q\"}]}",
                Reason.BLANK, Reason.BLANK, Reason.BLANK);
    }

    @Test
    public void rejectsOverLongTag() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"questions\":[{\"question\":\"Q\",\"tags\":[\""
                + repeat('t', QuestionSetParser.MAX_TAG_LENGTH + 1) + "\"]}]}", Reason.TOO_LONG);
    }

    @Test
    public void rejectsTagThatIsNotAString() {
        assertReasons("{"
                + "\"formatVersion\":\"1.0\","
                + "\"tags\":[7],"
                + "\"questions\":[{\"question\":\"Q\"}]}", Reason.WRONG_TYPE);
    }

    @Test
    public void reportsEveryViolationAtOnce() {
        assertReasons("{"
                + "\"formatVersion\":\"0.9\","
                + "\"extra\":true,"
                + "\"tags\":[\"history\",\"history\"],"
                + "\"questions\":[{\"question\":\"\"},{\"answer\":\"A\"}]}",
                Reason.UNKNOWN_FIELD,
                Reason.UNSUPPORTED_VERSION,
                Reason.DUPLICATE_TAG,
                Reason.BLANK,
                Reason.MISSING_FIELD);
    }

    private static List<ValidationError> assertReasons(String json, Reason... expected) {
        try {
            QuestionSetParser.parse(json);
            fail("expected " + Arrays.toString(expected) + " but the file was accepted");
            return null;
        } catch (QuestionSetFormatException e) {
            List<Reason> actual = new ArrayList<>();
            for (ValidationError error : e.getErrors()) {
                actual.add(error.getReason());
            }
            assertEquals(e.getErrors().toString(), Arrays.asList(expected), actual);
            return e.getErrors();
        }
    }

    /** The unit test runs from the module directory, but tolerate being started from the root. */
    private static String readTemplate(String name) throws IOException {
        File file = new File("../docs/templates/" + name);
        if (!file.exists()) {
            file = new File("docs/templates/" + name);
        }
        assertTrue(file + " not found", file.exists());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String repeat(char character, int times) {
        StringBuilder text = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            text.append(character);
        }
        return text.toString();
    }
}
