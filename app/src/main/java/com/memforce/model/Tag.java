package com.memforce.model;

import androidx.annotation.NonNull;

public class Tag {

    private final long id;
    private final String name;

    public Tag(long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
