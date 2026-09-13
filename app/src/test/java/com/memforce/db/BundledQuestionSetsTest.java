package com.memforce.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.memforce.importer.MergedQuestion;
import com.memforce.importer.QuestionSet;
import com.memforce.importer.QuestionSetEntry;
import com.memforce.importer.QuestionSetFormatException;
import com.memforce.importer.QuestionSetParser;
import com.memforce.importer.ValidationError;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holds the question sets bundled in the application package to the import format, on the build
 * machine. A malformed bundled set is a fault in the product rather than bad input from a user, so
 * it has to fail the build; without this test it would first be noticed as a crash on the device
 * the first time the database is created.
 */
public class BundledQuestionSetsTest {

    /** Fails rather than passing vacuously if the folder is ever emptied or moved. */
    @Test
    public void bundlesAtLeastOneSet() {
        assertTrue("no bundled question set found in " + folder(), !files().isEmpty());
    }

    @Test
    public void everyBundledSetParses() throws Exception {
        for (File file : files()) {
            QuestionSet set = parse(file);

            assertEquals(file + " declares an unsupported format version",
                    QuestionSetParser.SUPPORTED_FORMAT_VERSION, set.getFormatVersion());
            assertNotNull(file + " has no name", set.getName());
            assertNotNull(file + " has no description", set.getDescription());
        }
    }

    /**
     * A repeated question is folded into one row by {@link MergedQuestion}, so a set that repeats
     * one would silently store fewer questions than it appears to hold.
     */
    @Test
    public void noBundledSetRepeatsAQuestion() throws Exception {
        for (File file : files()) {
            QuestionSet set = parse(file);
            Set<String> seen = new HashSet<>();
            for (QuestionSetEntry entry : set.getQuestions()) {
                assertTrue(file + " repeats the question " + entry.getQuestion(),
                        seen.add(entry.getQuestion().toLowerCase(Locale.ROOT)));
            }
            assertEquals(file + " stores fewer questions than it lists",
                    set.getQuestions().size(), MergedQuestion.mergeAll(set).size());
        }
    }

    /** The set level tags are what makes a bundled topic selectable in the lobby. */
    @Test
    public void everyBundledQuestionCarriesTheSetTags() throws Exception {
        for (File file : files()) {
            QuestionSet set = parse(file);
            assertTrue(file + " has no set level tag", !set.getTags().isEmpty());
            for (MergedQuestion question : MergedQuestion.mergeAll(set)) {
                assertTrue(question.getQuestion() + " of " + file + " lost the set tags",
                        question.getTags().containsAll(set.getTags()));
            }
        }
    }

    @Test
    public void bundlesTheNbaSet() throws Exception {
        QuestionSet set = parse(file("nba.json"));

        assertEquals("NBA divisions", set.getName());
        assertEquals(Arrays.asList("sport", "basketball", "nba"), set.getTags());
        assertEquals("the league has 30 teams", 30, set.getQuestions().size());
        assertEquals(new TreeSet<>(Arrays.asList("eastern-conference", "western-conference")),
                new TreeSet<>(questionTags(set)));
    }

    @Test
    public void bundlesTheUsaStatesSet() throws Exception {
        QuestionSet set = parse(file("usa-states.json"));

        assertEquals("USA state capitals", set.getName());
        assertEquals(Arrays.asList("geography", "usa", "state-capitals"), set.getTags());
        assertEquals("the union has 50 states", 50, set.getQuestions().size());
        assertEquals(new TreeSet<>(Arrays.asList("midwest", "northeast", "south", "west")),
                new TreeSet<>(questionTags(set)));

        for (QuestionSetEntry entry : set.getQuestions()) {
            assertNotNull(entry.getQuestion() + " has no answer", entry.getAnswer());
        }
    }

    private static List<String> questionTags(QuestionSet set) {
        List<String> tags = new ArrayList<>();
        for (QuestionSetEntry entry : set.getQuestions()) {
            tags.addAll(entry.getTags());
        }
        return tags;
    }

    private static QuestionSet parse(File file) throws IOException {
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        try {
            return QuestionSetParser.parse(json);
        } catch (QuestionSetFormatException e) {
            StringBuilder message = new StringBuilder(file + " is not a valid question set:");
            for (ValidationError error : e.getErrors()) {
                message.append("\n  ").append(error.getReason())
                        .append(" at ").append(error.getLocation());
            }
            fail(message.toString());
            throw new AssertionError("unreachable");
        }
    }

    private static File file(String name) {
        File file = new File(folder(), name);
        assertTrue(file + " not found", file.isFile());
        return file;
    }

    /** Every bundled set, in the order {@code BundledQuestionSets} loads them. */
    private static List<File> files() {
        File[] found = folder().listFiles((directory, name) ->
                name.endsWith(BundledQuestionSets.SUFFIX));
        assertNotNull(folder() + " is not readable", found);
        Arrays.sort(found);
        return Arrays.asList(found);
    }

    /** Run from the module by Gradle, but from the root by some development tools. */
    private static File folder() {
        String path = "src/main/assets/" + BundledQuestionSets.FOLDER;
        File folder = new File(path);
        if (!folder.isDirectory()) {
            folder = new File("app/" + path);
        }
        assertTrue(folder + " not found", folder.isDirectory());
        return folder;
    }
}
