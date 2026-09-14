package com.memforce.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question implements Named {

    private final long id;
    private final String name;
    private final String answer;
    private final List<String> alternativeAnswers;
    private final List<String> tagNames;

    public Question(long id,
                    @NonNull String name,
                    @Nullable String answer,
                    @NonNull List<String> alternativeAnswers,
                    @NonNull List<String> tagNames) {
        this.id = id;
        this.name = name;
        this.answer = answer;
        this.alternativeAnswers =
                Collections.unmodifiableList(new ArrayList<>(alternativeAnswers));
        this.tagNames = Collections.unmodifiableList(new ArrayList<>(tagNames));
    }

    @Override
    public long getId() {
        return id;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

    /**
     * Further wordings of the answer that count as correct as well, in the order they were
     * stored; empty when the question carries none.
     */
    @NonNull
    public List<String> getAlternativeAnswers() {
        return alternativeAnswers;
    }

    /**
     * Everything a game accepts for this question: the answer first, then its alternatives. Empty
     * when the question carries no answer, because then nothing can be marked against it.
     */
    @NonNull
    public List<String> getAcceptedAnswers() {
        if (!isAnswerable()) {
            return Collections.emptyList();
        }
        List<String> accepted = new ArrayList<>(alternativeAnswers.size() + 1);
        accepted.add(answer);
        accepted.addAll(alternativeAnswers);
        return Collections.unmodifiableList(accepted);
    }

    /** Names of the assigned tags, ordered by name, empty when none are assigned. */
    @NonNull
    public List<String> getTagNames() {
        return tagNames;
    }

    /** Comma separated names of the assigned tags, empty when none are assigned. */
    @NonNull
    public String getTagsLabel() {
        StringBuilder label = new StringBuilder();
        for (String tag : tagNames) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(tag);
        }
        return label.toString();
    }

    /** Comma separated alternative answers, empty when the question carries none. */
    @NonNull
    public String getAlternativeAnswersLabel() {
        StringBuilder label = new StringBuilder();
        for (String alternative : alternativeAnswers) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(alternative);
        }
        return label.toString();
    }

    /** True when the question carries an answer a game can mark a submission against. */
    public boolean isAnswerable() {
        return answer != null && !answer.trim().isEmpty();
    }
}
