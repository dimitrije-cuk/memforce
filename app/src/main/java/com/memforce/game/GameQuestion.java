package com.memforce.game;

import androidx.annotation.NonNull;

/** One question as the game uses it: what is asked, and what counts as the answer. */
public final class GameQuestion {

    private final long id;
    private final String prompt;
    private final String answer;

    public GameQuestion(long id, @NonNull String prompt, @NonNull String answer) {
        this.id = id;
        this.prompt = prompt;
        this.answer = answer;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getPrompt() {
        return prompt;
    }

    @NonNull
    public String getAnswer() {
        return answer;
    }
}
