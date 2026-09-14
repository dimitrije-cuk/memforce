package com.memforce.game;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One question as the game uses it: what is asked, and what counts as the answer.
 *
 * <p>The answer is the one wording shown when a submission was wrong or skipped; the alternatives
 * are further wordings that are marked correct just as readily, so a question may be known in more
 * than one form without either form being the "real" one.
 */
public final class GameQuestion {

    private final long id;
    private final String prompt;
    private final String answer;
    private final List<String> alternativeAnswers;

    public GameQuestion(long id, @NonNull String prompt, @NonNull String answer) {
        this(id, prompt, answer, Collections.<String>emptyList());
    }

    public GameQuestion(long id,
                        @NonNull String prompt,
                        @NonNull String answer,
                        @NonNull List<String> alternativeAnswers) {
        this.id = id;
        this.prompt = prompt;
        this.answer = answer;
        this.alternativeAnswers =
                Collections.unmodifiableList(new ArrayList<>(alternativeAnswers));
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getPrompt() {
        return prompt;
    }

    /** The wording shown as the answer; the first of {@link #getAcceptedAnswers()}. */
    @NonNull
    public String getAnswer() {
        return answer;
    }

    /** Further wordings marked correct as well, empty when the question has none. */
    @NonNull
    public List<String> getAlternativeAnswers() {
        return alternativeAnswers;
    }

    /** Every wording a submission may match: the answer first, then its alternatives. */
    @NonNull
    public List<String> getAcceptedAnswers() {
        List<String> accepted = new ArrayList<>(alternativeAnswers.size() + 1);
        accepted.add(answer);
        accepted.addAll(alternativeAnswers);
        return Collections.unmodifiableList(accepted);
    }
}
