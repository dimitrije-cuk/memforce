package com.memforce.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.importer.ValidationError.Reason;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Reads a question set file into a {@link QuestionSet}, enforcing every rule of
 * {@code docs/schemas/question-set.schema.json}. A file is either taken as a whole or rejected as
 * a whole: parsing collects all violations before it fails, so the user sees everything that is
 * wrong with the file at once.
 */
public final class QuestionSetParser {

    /** The newest {@code formatVersion}, the one the template and the carried sets declare. */
    public static final String CURRENT_FORMAT_VERSION = "1.1";

    /**
     * Every {@code formatVersion} this app can read, oldest first. A minor version only adds
     * optional fields, so an older file stays readable and the fields of the newest version are
     * accepted whichever of these a file declares.
     */
    public static final List<String> SUPPORTED_FORMAT_VERSIONS =
            Collections.unmodifiableList(Arrays.asList("1.0", CURRENT_FORMAT_VERSION));

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_TAG_LENGTH = 50;
    public static final int MAX_QUESTION_LENGTH = 1000;
    public static final int MAX_ANSWER_LENGTH = 2000;

    private static final String FORMAT_VERSION = "formatVersion";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String TAGS = "tags";
    private static final String QUESTIONS = "questions";
    private static final String QUESTION = "question";
    private static final String ANSWER = "answer";
    private static final String ALTERNATIVE_ANSWERS = "alternativeAnswers";

    private static final Set<String> SET_FIELDS =
            fields(FORMAT_VERSION, NAME, DESCRIPTION, TAGS, QUESTIONS);
    private static final Set<String> QUESTION_FIELDS =
            fields(QUESTION, ANSWER, ALTERNATIVE_ANSWERS, TAGS);

    /** A byte order mark survives a UTF-8 read and would make the JSON unparseable. */
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private final List<ValidationError> errors = new ArrayList<>();

    private QuestionSetParser() {
    }

    @NonNull
    public static QuestionSet parse(@NonNull String json) throws QuestionSetFormatException {
        return new QuestionSetParser().parseSet(json);
    }

    private QuestionSet parseSet(String json) throws QuestionSetFormatException {
        JSONObject root = readRoot(json);
        rejectUnknownFields(root, SET_FIELDS, "");

        String formatVersion = readFormatVersion(root);
        String name = readString(root, NAME, NAME, MAX_NAME_LENGTH, true);
        String description = readString(root, DESCRIPTION, DESCRIPTION, MAX_DESCRIPTION_LENGTH, false);
        List<String> tags = readTags(root, TAGS);
        List<QuestionSetEntry> questions = readQuestions(root);

        if (!errors.isEmpty()) {
            throw new QuestionSetFormatException(errors);
        }
        return new QuestionSet(formatVersion, name, description, tags, questions);
    }

    /** Nothing else can be reported when the file is not a JSON object, so these fail on their own. */
    private JSONObject readRoot(String json) throws QuestionSetFormatException {
        String text = json.isEmpty() || json.charAt(0) != BYTE_ORDER_MARK ? json : json.substring(1);
        JSONTokener tokener = new JSONTokener(text);
        Object root;
        try {
            root = tokener.nextValue();
            if (tokener.nextClean() != 0) {
                throw new JSONException("Unexpected content after the question set");
            }
        } catch (JSONException e) {
            throw new QuestionSetFormatException(Collections.singletonList(
                    ValidationError.detailed(Reason.MALFORMED_JSON, "", e.getMessage())));
        }
        if (!(root instanceof JSONObject)) {
            throw new QuestionSetFormatException(Collections.singletonList(
                    ValidationError.at(Reason.NOT_AN_OBJECT, "")));
        }
        return (JSONObject) root;
    }

    /** The schema sets {@code additionalProperties: false}, so an invented field is an error. */
    private void rejectUnknownFields(JSONObject object, Set<String> allowed, String path) {
        List<String> unknown = new ArrayList<>();
        for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
            String key = keys.next();
            if (!allowed.contains(key)) {
                unknown.add(key);
            }
        }
        Collections.sort(unknown);
        for (String key : unknown) {
            errors.add(ValidationError.at(Reason.UNKNOWN_FIELD, path(path, key)));
        }
    }

    private String readFormatVersion(JSONObject root) {
        if (!root.has(FORMAT_VERSION)) {
            errors.add(ValidationError.at(Reason.MISSING_FIELD, FORMAT_VERSION));
            return CURRENT_FORMAT_VERSION;
        }
        Object raw = root.opt(FORMAT_VERSION);
        if (!(raw instanceof String)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, FORMAT_VERSION));
            return CURRENT_FORMAT_VERSION;
        }
        String version = (String) raw;
        if (!SUPPORTED_FORMAT_VERSIONS.contains(version)) {
            errors.add(ValidationError.detailed(Reason.UNSUPPORTED_VERSION, FORMAT_VERSION, version));
        }
        return version;
    }

    /**
     * @param rejectEmpty when the schema gives the field a {@code minLength} of one; whitespace
     *                    around a value is trimmed either way, and a value left blank by the
     *                    trimming counts as absent
     */
    @Nullable
    private String readString(JSONObject owner,
                              String field,
                              String path,
                              int maxLength,
                              boolean rejectEmpty) {
        if (!owner.has(field)) {
            return null;
        }
        Object raw = owner.opt(field);
        if (!(raw instanceof String)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, path));
            return null;
        }
        String value = (String) raw;
        if (value.length() > maxLength) {
            errors.add(ValidationError.tooLong(path, maxLength));
            return null;
        }
        if (rejectEmpty && value.isEmpty()) {
            errors.add(ValidationError.at(Reason.BLANK, path));
            return null;
        }
        String stripped = strip(value);
        return stripped.isEmpty() ? null : stripped;
    }

    @NonNull
    private List<String> readTags(JSONObject owner, String path) {
        List<String> tags = new ArrayList<>();
        if (!owner.has(TAGS)) {
            return tags;
        }
        Object raw = owner.opt(TAGS);
        if (!(raw instanceof JSONArray)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, path));
            return tags;
        }
        JSONArray array = (JSONArray) raw;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String location = path + "[" + i + "]";
            Object item = array.opt(i);
            if (!(item instanceof String)) {
                errors.add(ValidationError.at(Reason.WRONG_TYPE, location));
                continue;
            }
            String tag = (String) item;
            if (tag.length() > MAX_TAG_LENGTH) {
                errors.add(ValidationError.tooLong(location, MAX_TAG_LENGTH));
            } else if (tag.isEmpty() || !tag.equals(strip(tag))) {
                errors.add(ValidationError.at(Reason.BLANK, location));
            } else if (spansLines(tag)) {
                errors.add(ValidationError.at(Reason.NOT_ONE_LINE, location));
            } else if (!seen.add(tag)) {
                errors.add(ValidationError.detailed(Reason.DUPLICATE_TAG, location, tag));
            } else {
                tags.add(tag);
            }
        }
        return tags;
    }

    @NonNull
    private List<QuestionSetEntry> readQuestions(JSONObject root) {
        List<QuestionSetEntry> questions = new ArrayList<>();
        if (!root.has(QUESTIONS)) {
            errors.add(ValidationError.at(Reason.MISSING_FIELD, QUESTIONS));
            return questions;
        }
        Object raw = root.opt(QUESTIONS);
        if (!(raw instanceof JSONArray)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, QUESTIONS));
            return questions;
        }
        JSONArray array = (JSONArray) raw;
        if (array.length() == 0) {
            errors.add(ValidationError.at(Reason.NO_QUESTIONS, QUESTIONS));
            return questions;
        }
        for (int i = 0; i < array.length(); i++) {
            String path = QUESTIONS + "[" + i + "]";
            Object item = array.opt(i);
            if (!(item instanceof JSONObject)) {
                errors.add(ValidationError.at(Reason.WRONG_TYPE, path));
                continue;
            }
            QuestionSetEntry entry = readQuestion((JSONObject) item, path);
            if (entry != null) {
                questions.add(entry);
            }
        }
        return questions;
    }

    @Nullable
    private QuestionSetEntry readQuestion(JSONObject object, String path) {
        rejectUnknownFields(object, QUESTION_FIELDS, path);
        String text = readQuestionText(object, path);
        int errorsBeforeAnswer = errors.size();
        String answer = readAnswer(object, path);
        // A faulty answer has already been reported; treating it as given keeps the alternatives
        // from adding a second complaint about the same field.
        boolean answered = answer != null || errors.size() > errorsBeforeAnswer;
        List<String> alternatives = readAlternativeAnswers(object, path, answered);
        List<String> tags = readTags(object, path(path, TAGS));
        return text == null ? null : new QuestionSetEntry(text, answer, alternatives, tags);
    }

    @Nullable
    private String readQuestionText(JSONObject object, String path) {
        String location = path(path, QUESTION);
        if (!object.has(QUESTION)) {
            errors.add(ValidationError.at(Reason.MISSING_FIELD, location));
            return null;
        }
        Object raw = object.opt(QUESTION);
        if (!(raw instanceof String)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, location));
            return null;
        }
        String value = (String) raw;
        if (value.length() > MAX_QUESTION_LENGTH) {
            errors.add(ValidationError.tooLong(location, MAX_QUESTION_LENGTH));
            return null;
        }
        String stripped = strip(value);
        if (stripped.isEmpty()) {
            errors.add(ValidationError.at(Reason.BLANK, location));
            return null;
        }
        return stripped;
    }

    /** An omitted, null or blank answer all mean "this question has no stored answer". */
    @Nullable
    private String readAnswer(JSONObject object, String path) {
        String location = path(path, ANSWER);
        if (!object.has(ANSWER) || object.isNull(ANSWER)) {
            return null;
        }
        Object raw = object.opt(ANSWER);
        if (!(raw instanceof String)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, location));
            return null;
        }
        String value = (String) raw;
        if (value.length() > MAX_ANSWER_LENGTH) {
            errors.add(ValidationError.tooLong(location, MAX_ANSWER_LENGTH));
            return null;
        }
        String stripped = strip(value);
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * Reads the further wordings a game accepts beside the answer. They only mean something
     * against an answer to vary, so the schema makes the field depend on {@code answer} and this
     * extends that to an answer left blank, which counts as absent everywhere else too.
     *
     * @param answered whether the question carries an answer these can be alternatives to
     */
    @NonNull
    private List<String> readAlternativeAnswers(JSONObject object, String path, boolean answered) {
        List<String> answers = new ArrayList<>();
        String location = path(path, ALTERNATIVE_ANSWERS);
        if (!object.has(ALTERNATIVE_ANSWERS)) {
            return answers;
        }
        if (!answered) {
            errors.add(ValidationError.at(Reason.ALTERNATIVES_WITHOUT_ANSWER, location));
        }
        Object raw = object.opt(ALTERNATIVE_ANSWERS);
        if (!(raw instanceof JSONArray)) {
            errors.add(ValidationError.at(Reason.WRONG_TYPE, location));
            return answers;
        }
        JSONArray array = (JSONArray) raw;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String itemLocation = location + "[" + i + "]";
            Object item = array.opt(i);
            if (!(item instanceof String)) {
                errors.add(ValidationError.at(Reason.WRONG_TYPE, itemLocation));
                continue;
            }
            String value = (String) item;
            if (value.length() > MAX_ANSWER_LENGTH) {
                errors.add(ValidationError.tooLong(itemLocation, MAX_ANSWER_LENGTH));
                continue;
            }
            String stripped = strip(value);
            if (stripped.isEmpty()) {
                errors.add(ValidationError.at(Reason.BLANK, itemLocation));
            } else if (!seen.add(stripped)) {
                errors.add(ValidationError.detailed(Reason.DUPLICATE_ANSWER, itemLocation, stripped));
            } else {
                answers.add(stripped);
            }
        }
        return answers;
    }

    private static String path(String prefix, String field) {
        return prefix.isEmpty() ? field : prefix + "." + field;
    }

    /**
     * The schema's tag pattern is an ECMA regular expression, whose {@code \s} covers more than
     * {@link String#trim()} does: a tag padded with a non-breaking space has to be rejected rather
     * than stored as a lookalike of an existing tag.
     */
    private static boolean isWhitespace(char character) {
        return Character.isWhitespace(character)
                || Character.isSpaceChar(character)
                || character == BYTE_ORDER_MARK;
    }

    private static String strip(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /** The schema's tag pattern uses {@code .}, which never matches a line terminator. */
    private static boolean spansLines(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '\n' || character == '\r'
                    || character == '\u2028' || character == '\u2029') {
                return true;
            }
        }
        return false;
    }

    private static Set<String> fields(String... names) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }
}
