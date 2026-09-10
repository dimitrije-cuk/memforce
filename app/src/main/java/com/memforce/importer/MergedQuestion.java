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
    private String answer;

    private MergedQuestion(String question) {
        this.question = question;
    }

    /**
     * Merges a set into the questions to store, keeping the order of the file. A question repeated
     * within the file is stored once, with the tags of every entry and the first answer given.
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
            }
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

    @NonNull
    public String getQuestion() {
        return question;
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

    /** Set level tags first, then the question's own, without a tag repeating in either case. */
    @NonNull
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }
}
