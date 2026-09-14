package com.memforce.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One question of a question set file, with the tags that apply to it alone. */
public final class QuestionSetEntry {

    private final String question;
    private final String answer;
    private final List<String> alternativeAnswers;
    private final List<String> tags;

    QuestionSetEntry(@NonNull String question,
                     @Nullable String answer,
                     @NonNull List<String> alternativeAnswers,
                     @NonNull List<String> tags) {
        this.question = question;
        this.answer = answer;
        this.alternativeAnswers =
                Collections.unmodifiableList(new ArrayList<>(alternativeAnswers));
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    @NonNull
    public String getQuestion() {
        return question;
    }

    /** Null when the file leaves the answer out or sets it to null. */
    @Nullable
    public String getAnswer() {
        return answer;
    }

    /**
     * Further wordings of {@link #getAnswer()} that a game accepts as well, in the order the file
     * lists them; empty when the file gives none.
     */
    @NonNull
    public List<String> getAlternativeAnswers() {
        return alternativeAnswers;
    }

    /** Tags of this question alone; the set level tags come on top of these at import time. */
    @NonNull
    public List<String> getTags() {
        return tags;
    }
}
