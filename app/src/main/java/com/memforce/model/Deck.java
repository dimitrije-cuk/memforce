package com.memforce.model;

import androidx.annotation.NonNull;

public class Deck implements Named {

    private final long id;
    private final String name;
    private final int questionCount;

    public Deck(long id, @NonNull String name, int questionCount) {
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
}
