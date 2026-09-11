package com.memforce.model;

import androidx.annotation.NonNull;

/** A tag together with the number of questions counted for it. */
public class TagUsage implements Named {

    private final long id;
    private final String name;
    private final int questionCount;

    public TagUsage(long id, @NonNull String name, int questionCount) {
        this.id = id;
        this.name = name;
        this.questionCount = questionCount;
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

    public int getQuestionCount() {
        return questionCount;
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + questionCount + ')';
    }
}
