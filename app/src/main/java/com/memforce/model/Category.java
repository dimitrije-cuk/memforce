package com.memforce.model;

import androidx.annotation.NonNull;

public class Category implements Named {

    private final long id;
    private final String name;
    private final String tagsLabel;

    public Category(long id, @NonNull String name, @NonNull String tagsLabel) {
        this.id = id;
        this.name = name;
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

    /** Comma separated names of the assigned tags, empty when none are assigned. */
    @NonNull
    public String getTagsLabel() {
        return tagsLabel;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
