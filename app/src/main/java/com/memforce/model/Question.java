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
    private final List<String> tagNames;

    public Question(long id,
                    @NonNull String name,
                    @Nullable String answer,
                    @NonNull List<String> tagNames) {
        this.id = id;
        this.name = name;
        this.answer = answer;
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

    /** True when the question carries an answer a game can mark a submission against. */
    public boolean isAnswerable() {
        return answer != null && !answer.trim().isEmpty();
    }
}
