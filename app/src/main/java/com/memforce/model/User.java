package com.memforce.model;

import androidx.annotation.NonNull;

public class User {

    private final long id;
    private final String name;

    public User(long id, @NonNull String name) {
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
}
