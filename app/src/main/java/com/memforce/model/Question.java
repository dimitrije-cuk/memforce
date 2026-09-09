package com.memforce.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Question implements Named {

    private final long id;
    private final String name;
    private final String answer;
    private final String tagsLabel;

    public Question(long id,
                    @NonNull String name,
                    @Nullable String answer,
                    @NonNull String tagsLabel) {
        this.id = id;
        this.name = name;
        this.answer = answer;
        this.tagsLabel = tagsLabel;
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

    /** Comma separated names of the assigned tags, empty when none are assigned. */
    @NonNull
    public String getTagsLabel() {
        return tagsLabel;
    }
}
