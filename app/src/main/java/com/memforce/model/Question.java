package com.memforce.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Question implements Named {

    private final long id;
    private final String name;
    private final String answer;
    private final Long categoryId;
    private final String categoryName;
    private final String tagsLabel;

    public Question(long id,
                    @NonNull String name,
                    @Nullable String answer,
                    @Nullable Long categoryId,
                    @NonNull String categoryName,
                    @NonNull String tagsLabel) {
        this.id = id;
        this.name = name;
        this.answer = answer;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
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

    @Nullable
    public Long getCategoryId() {
        return categoryId;
    }

    /** Empty when the question has no category. */
    @NonNull
    public String getCategoryName() {
        return categoryName;
    }

    /** Comma separated names of the assigned tags, empty when none are assigned. */
    @NonNull
    public String getTagsLabel() {
        return tagsLabel;
    }
}
