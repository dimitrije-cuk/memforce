package com.memforce.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A validated question set file, as described by {@code docs/question-import-format.md}.
 * Produced by {@link QuestionSetParser}; never built from unchecked input.
 */
public final class QuestionSet {

    private final String formatVersion;
    private final String name;
    private final String description;
    private final List<String> tags;
    private final List<QuestionSetEntry> questions;

    QuestionSet(@NonNull String formatVersion,
                @Nullable String name,
                @Nullable String description,
                @NonNull List<String> tags,
                @NonNull List<QuestionSetEntry> questions) {
        this.formatVersion = formatVersion;
        this.name = name;
        this.description = description;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
    }

    @NonNull
    public String getFormatVersion() {
        return formatVersion;
    }

    /** Label for the set; shown while importing but not stored. */
    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    /** Set level tags, inherited by every question in {@link #getQuestions()}. */
    @NonNull
    public List<String> getTags() {
        return tags;
    }

    /** Never empty. */
    @NonNull
    public List<QuestionSetEntry> getQuestions() {
        return questions;
    }
}
