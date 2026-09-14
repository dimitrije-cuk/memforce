package com.memforce.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * One run of the game over the questions taken from the lobby.
 *
 * <p>The questions are shuffled once, at the start, and then asked from a queue that never
 * shortens: whatever the player submits, the question that was just asked goes to the back, so a
 * question answered wrongly returns after every other question has been seen, and a question
 * answered correctly returns too. The run ends when every question has been answered correctly at
 * least once.
 *
 * <p>Three numbers describe the progress at any moment:
 *
 * <ul>
 *   <li><em>streak</em> — correct submissions in a row, reset to zero by any other submission;
 *   <li><em>correct</em> — how many different questions have been answered correctly at least
 *       once, which never decreases;
 *   <li><em>total</em> — how many questions the run was started with.
 * </ul>
 *
 * <p>Reaching {@code correct == total} is a victory. Because a question moves to the back of the
 * queue after every attempt, a streak as long as the run itself can only be made of that many
 * different questions, so {@code streak == total} means every question was answered correctly
 * without a single wrong or skipped submission in between: a perfect victory.
 *
 * <p>The class holds no Android type, so the rules can be exercised without a device.
 */
public final class GameSession {

    /** What the player's submission turned out to be. */
    public enum Outcome {
        CORRECT,
        INCORRECT,
        /** An empty submission. It counts exactly as an incorrect one. */
        SKIPPED
    }

    /** How far the run has got. */
    public enum Result {
        IN_PROGRESS,
        VICTORY,
        PERFECT_VICTORY
    }

    private final Deque<GameQuestion> queue = new ArrayDeque<>();
    private final Set<Long> mastered = new HashSet<>();
    private final int total;

    private int streak;
    private int attempts;
    private Result result = Result.IN_PROGRESS;

    private GameSession(@NonNull List<GameQuestion> shuffled) {
        this.queue.addAll(shuffled);
        this.total = shuffled.size();
    }

    /**
     * Starts a run over the given questions.
     *
     * @param random the source of the starting order; a fixed one makes a run repeatable
     * @throws IllegalArgumentException when there is no question to ask
     */
    @NonNull
    public static GameSession start(@NonNull List<GameQuestion> questions, @NonNull Random random) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("A game needs at least one question");
        }
        List<GameQuestion> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled, random);
        return new GameSession(shuffled);
    }

    @NonNull
    public static GameSession start(@NonNull List<GameQuestion> questions) {
        return start(questions, new Random());
    }

    /** The question being asked. */
    @NonNull
    public GameQuestion current() {
        GameQuestion question = queue.peekFirst();
        if (question == null) {
            throw new IllegalStateException("The queue of a started game is never empty");
        }
        return question;
    }

    /**
     * Marks a submission for the current question and moves that question to the back of the
     * queue.
     *
     * @param answer what the player wrote; null or blank is the skip the rules describe
     * @throws IllegalStateException when the run is already decided
     */
    @NonNull
    public Outcome submit(@Nullable String answer) {
        if (isFinished()) {
            throw new IllegalStateException("The game is over");
        }
        GameQuestion asked = queue.removeFirst();
        queue.addLast(asked);
        attempts++;

        boolean correct = AnswerMatcher.matchesAny(asked.getAcceptedAnswers(), answer);
        if (correct) {
            streak++;
            mastered.add(asked.getId());
        } else {
            streak = 0;
        }
        if (mastered.size() == total) {
            result = streak == total ? Result.PERFECT_VICTORY : Result.VICTORY;
        }
        if (correct) {
            return Outcome.CORRECT;
        }
        return AnswerMatcher.isAnswered(answer) ? Outcome.INCORRECT : Outcome.SKIPPED;
    }

    /** Correct submissions in a row. */
    public int getStreak() {
        return streak;
    }

    /** Different questions answered correctly at least once. */
    public int getCorrectCount() {
        return mastered.size();
    }

    /** Questions the run was started with. */
    public int getTotal() {
        return total;
    }

    /** Submissions made so far, skips included. */
    public int getAttempts() {
        return attempts;
    }

    public boolean isMastered(long questionId) {
        return mastered.contains(questionId);
    }

    public boolean isFinished() {
        return result != Result.IN_PROGRESS;
    }

    @NonNull
    public Result getResult() {
        return result;
    }
}
