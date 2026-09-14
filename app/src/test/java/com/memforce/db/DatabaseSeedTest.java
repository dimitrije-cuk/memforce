package com.memforce.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the demo data of {@code assets/seed/memforce_seed.sql} to the rules the schema and the
 * game impose on it, on the build machine. The seed is executed while the database is being
 * created, so a row naming a question that does not exist, or an alternative answer the question
 * already accepts, would first be noticed as a crash or a silent loss on the first launch.
 */
public class DatabaseSeedTest {

    /** One row of an {@code INSERT}, e.g. {@code (9, 'Which gas...', 'Carbon dioxide')}. */
    private static final Pattern ROW = Pattern.compile("\\(([^()]*)\\)");

    /** One value of a row: a number, or a quoted literal in which a quote is doubled. */
    private static final Pattern VALUE =
            Pattern.compile("\\s*(?:(\\d+)|'((?:[^']|'')*)')\\s*,?");

    @Test
    public void seedsTheDemoQuestions() throws IOException {
        assertEquals(25, rows("questions").size());
        assertEquals(9, rows("tags").size());
    }

    @Test
    public void assignsTagsOnlyToSeededQuestions() throws IOException {
        Set<String> questions = identifiers(rows("questions"));
        Set<String> tags = identifiers(rows("tags"));

        for (List<String> row : rows("question_tags")) {
            assertTrue("question_tags names the unknown question " + row.get(0),
                    questions.contains(row.get(0)));
            assertTrue("question_tags names the unknown tag " + row.get(1),
                    tags.contains(row.get(1)));
        }
    }

    /**
     * An alternative repeating the answer, or another alternative of the same question, would be
     * refused by {@code idx_alternative_answers_answer} and would in any case add nothing a game
     * would mark differently.
     */
    @Test
    public void seedsAlternativeAnswersThatWidenWhatIsAccepted() throws IOException {
        Map<String, List<String>> questions = byIdentifier(rows("questions"));
        Map<String, Set<String>> accepted = new LinkedHashMap<>();
        List<List<String>> alternatives = rows("alternative_answers");
        Set<String> identifiers = new TreeSet<>();

        for (List<String> row : alternatives) {
            String id = row.get(0);
            String questionId = row.get(1);
            String alternative = row.get(2);

            assertTrue("alternative_answers repeats the identifier " + id, identifiers.add(id));
            List<String> question = questions.get(questionId);
            assertNotNull("alternative answer " + id + " names the unknown question " + questionId,
                    question);

            Set<String> wordings = accepted.get(questionId);
            if (wordings == null) {
                wordings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                wordings.add(question.get(2));
                accepted.put(questionId, wordings);
            }
            assertTrue("\"" + question.get(1) + "\" already accepts " + alternative,
                    wordings.add(alternative));
        }
    }

    /** The rows of one {@code INSERT} statement, each as the list of its column values. */
    private static List<List<String>> rows(String table) throws IOException {
        String statement = statement(table);
        List<List<String>> rows = new ArrayList<>();
        Matcher row = ROW.matcher(statement.substring(statement.indexOf(" VALUES") + 7));
        while (row.find()) {
            rows.add(values(row.group(1)));
        }
        assertTrue(table + " has no rows", !rows.isEmpty());
        return rows;
    }

    private static List<String> values(String row) {
        List<String> values = new ArrayList<>();
        Matcher value = VALUE.matcher(row);
        int at = 0;
        while (at < row.length() && value.find(at) && value.start() == at) {
            values.add(value.group(1) != null ? value.group(1) : value.group(2).replace("''", "'"));
            at = value.end();
        }
        assertTrue("cannot read the seeded row (" + row + ")", !values.isEmpty());
        return values;
    }

    private static Set<String> identifiers(List<List<String>> rows) {
        Set<String> identifiers = new TreeSet<>();
        for (List<String> row : rows) {
            assertTrue("the seed repeats the identifier " + row.get(0),
                    identifiers.add(row.get(0)));
        }
        return identifiers;
    }

    private static Map<String, List<String>> byIdentifier(List<List<String>> rows) {
        Map<String, List<String>> byIdentifier = new LinkedHashMap<>();
        for (List<String> row : rows) {
            byIdentifier.put(row.get(0), row);
        }
        return byIdentifier;
    }

    private static String statement(String table) throws IOException {
        String prefix = "INSERT INTO " + table + " ";
        for (String statement : text().split(";")) {
            String trimmed = statement.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed;
            }
        }
        throw new AssertionError("the seed holds no " + prefix + "statement");
    }

    /** Comments and blank lines are dropped exactly as {@code DatabaseSeeder} drops them. */
    private static String text() throws IOException {
        StringBuilder sql = new StringBuilder();
        for (String line : new String(Files.readAllBytes(file().toPath()),
                StandardCharsets.UTF_8).split("\r\n|\r|\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                sql.append(trimmed).append('\n');
            }
        }
        return sql.toString();
    }

    /** Run from the module by Gradle, but from the root by some development tools. */
    private static File file() {
        String path = "src/main/assets/seed/memforce_seed.sql";
        File file = new File(path);
        if (!file.isFile()) {
            file = new File("app/" + path);
        }
        assertTrue(file + " not found", file.isFile());
        return file;
    }
}
