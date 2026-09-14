package com.memforce.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A question of a question set as it will be stored: the set level tags and the question's own
 * tags merged into one list, and entries that repeat a question within one file folded together.
 */
public final class MergedQuestion {

    private final String question;
    private final List<String> tags = new ArrayList<>();
    private final Set<String> seenTags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final List<String> alternativeAnswers = new ArrayList<>();
    private final Set<String> seenAnswers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private String answer;

    private MergedQuestion(String question) {
        this.question = question;
    }

    /**
     * Merges a set into the questions to store, keeping the order of the file. A question repeated
     * within the file is stored once, with the tags of every entry, the first answer given, and
     * the alternative answers of every entry.
     */
    @NonNull
    public static List<MergedQuestion> mergeAll(@NonNull QuestionSet set) {
        Map<String, MergedQuestion> byQuestion = new LinkedHashMap<>();
        for (QuestionSetEntry entry : set.getQuestions()) {
            String key = entry.getQuestion().toLowerCase(Locale.ROOT);
            MergedQuestion merged = byQuestion.get(key);
            if (merged == null) {
                merged = new MergedQuestion(entry.getQuestion());
                byQuestion.put(key, merged);
            }
            if (merged.answer == null) {
                merged.answer = entry.getAnswer();
                if (merged.answer != null) {
                    merged.seenAnswers.add(merged.answer);
                }
            }
            merged.addAlternativeAnswers(entry.getAlternativeAnswers());
            merged.addTags(set.getTags());
            merged.addTags(entry.getTags());
        }
        return Collections.unmodifiableList(new ArrayList<>(byQuestion.values()));
    }

    private void addTags(List<String> candidates) {
        for (String tag : candidates) {
            if (seenTags.add(tag)) {
                tags.add(tag);
            }
        }
    }

    /**
     * An alternative repeating the answer, or one already collected, adds nothing to what a game
     * would accept, so it is dropped rather than stored twice — the rule that folds repeated tags,
     * applied to answers.
     */
    private void addAlternativeAnswers(List<String> candidates) {
        for (String candidate : candidates) {
            if (seenAnswers.add(candidate)) {
                alternativeAnswers.add(candidate);
            }
        }
    }

    @NonNull
    public String getQuestion() {
        return question;
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

    /**
     * Further wordings a game accepts beside {@link #getAnswer()}, in the order of the file and
     * without one repeating the answer or another, compared without regard to letter case.
     */
    @NonNull
    public List<String> getAlternativeAnswers() {
        return Collections.unmodifiableList(alternativeAnswers);
    }

    /** Set level tags first, then the question's own, without a tag repeating in either case. */
    @NonNull
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }
}
