package com.memforce.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Checks the rules of a run: the queue that never shortens, the two counters, and the two ways a
 * run can be won.
 */
public class GameSessionTest {

    @Test
    public void needsAQuestionToStart() {
        try {
            GameSession.start(new ArrayList<>(), new Random(1));
            fail("A game without questions was started");
        } catch (IllegalArgumentException expected) {
            // The lobby screen keeps this from happening; the rule is stated here.
        }
    }

    @Test
    public void countsTheQuestionsItWasStartedWith() {
        GameSession game = start(5);

        assertEquals(5, game.getTotal());
        assertEquals(0, game.getStreak());
        assertEquals(0, game.getCorrectCount());
        assertEquals(0, game.getAttempts());
        assertFalse(game.isFinished());
        assertEquals(GameSession.Result.IN_PROGRESS, game.getResult());
    }

    @Test
    public void asksTheQuestionsInAShuffledOrder() {
        List<GameQuestion> questions = questions(10);

        List<Long> asked = askedOrder(GameSession.start(questions, new Random(7)), 10);
        List<Long> againFromTheSameSource = askedOrder(GameSession.start(questions, new Random(7)), 10);

        assertEquals("The same source must give the same run", asked, againFromTheSameSource);
        assertNotEquals("The questions were not shuffled", idsOf(questions), asked);
        assertEquals("No question may be lost by the shuffle",
                new HashSet<>(idsOf(questions)), new HashSet<>(asked));
    }

    @Test
    public void asksEveryOtherQuestionBeforeAskingOneAgain() {
        GameSession game = start(3);
        long first = game.current().getId();

        game.submit("wrong");
        assertNotEquals(first, game.current().getId());
        game.submit("wrong");
        assertNotEquals(first, game.current().getId());
        game.submit("wrong");

        assertEquals("The question asked first must come round again", first, game.current().getId());
    }

    @Test
    public void sendsACorrectlyAnsweredQuestionToTheBackOfTheQueueAsWell() {
        GameSession game = start(3);
        long first = game.current().getId();

        answerCorrectly(game);
        game.submit("wrong");
        game.submit("wrong");

        assertEquals(first, game.current().getId());
        assertEquals(1, game.getCorrectCount());
    }

    @Test
    public void countsACorrectAnswer() {
        GameSession game = start(3);

        assertEquals(GameSession.Outcome.CORRECT, answerCorrectly(game));
        assertEquals(1, game.getStreak());
        assertEquals(1, game.getCorrectCount());
        assertEquals(1, game.getAttempts());
    }

    @Test
    public void countsAWrongAnswer() {
        GameSession game = start(3);

        assertEquals(GameSession.Outcome.INCORRECT, game.submit("no idea"));
        assertEquals(0, game.getStreak());
        assertEquals(0, game.getCorrectCount());
        assertEquals(1, game.getAttempts());
    }

    @Test
    public void treatsAnEmptySubmissionAsASkipThatCountsAsWrong() {
        GameSession game = start(3);
        answerCorrectly(game);

        assertEquals(GameSession.Outcome.SKIPPED, game.submit(""));
        assertEquals("A skip breaks the streak", 0, game.getStreak());
        assertEquals("A skip masters nothing", 1, game.getCorrectCount());
        assertEquals(GameSession.Outcome.SKIPPED, game.submit("   "));
        assertEquals(GameSession.Outcome.SKIPPED, game.submit(null));
    }

    @Test
    public void breaksTheStreakOnAWrongAnswerAndBuildsItAgain() {
        GameSession game = start(4);

        answerCorrectly(game);
        answerCorrectly(game);
        assertEquals(2, game.getStreak());

        game.submit("wrong");
        assertEquals(0, game.getStreak());

        answerCorrectly(game);
        assertEquals(1, game.getStreak());
    }

    @Test
    public void countsAQuestionAnsweredCorrectlyTwiceOnlyOnce() {
        GameSession game = start(2);
        long first = game.current().getId();

        answerCorrectly(game);
        game.submit("wrong");
        assertEquals(first, game.current().getId());
        answerCorrectly(game);

        assertEquals("Only different questions count towards the total", 1, game.getCorrectCount());
        assertEquals(1, game.getStreak());
        assertFalse(game.isFinished());
    }

    @Test
    public void isWonWhenEveryQuestionHasBeenAnsweredCorrectlyOnce() {
        GameSession game = start(3);

        // The streak must be broken inside the final round for the win to be an ordinary one:
        // getting a question wrong that was already answered correctly does exactly that, and
        // takes nothing away from the total.
        answerCorrectly(game);
        game.submit("wrong");
        answerCorrectly(game);
        game.submit("wrong");
        assertFalse(game.isFinished());
        answerCorrectly(game);

        assertTrue(game.isFinished());
        assertEquals(3, game.getCorrectCount());
        assertEquals(1, game.getStreak());
        assertEquals(GameSession.Result.VICTORY, game.getResult());
    }

    @Test
    public void isWonPerfectlyWhenNothingWasMissedAlongTheWay() {
        GameSession game = start(4);

        for (int i = 0; i < 4; i++) {
            answerCorrectly(game);
        }

        assertTrue(game.isFinished());
        assertEquals(4, game.getStreak());
        assertEquals(4, game.getCorrectCount());
        assertEquals(GameSession.Result.PERFECT_VICTORY, game.getResult());
    }

    @Test
    public void isWonPerfectlyWhenTheStreakItselfCoversEveryQuestion() {
        GameSession game = start(3);

        // A streak as long as the run is made of that many different questions, because each one
        // goes to the back of the queue as it is asked. A question missed at the start therefore
        // does not rule a perfect victory out: answering the whole set correctly afterwards, one
        // question after another, is the unbroken run REQ-GAME-120 asks for.
        game.submit("wrong");
        answerCorrectly(game);
        answerCorrectly(game);
        answerCorrectly(game);

        assertEquals(3, game.getStreak());
        assertEquals(4, game.getAttempts());
        assertEquals(GameSession.Result.PERFECT_VICTORY, game.getResult());
    }

    @Test
    public void isWonPerfectlyBySingleQuestionAnsweredCorrectly() {
        GameSession game = start(1);

        answerCorrectly(game);

        assertEquals(GameSession.Result.PERFECT_VICTORY, game.getResult());
    }

    @Test
    public void keepsAskingASingleQuestionUntilItIsAnswered() {
        GameSession game = start(1);

        game.submit("wrong");
        assertFalse(game.isFinished());
        assertEquals(1, game.current().getId());
    }

    @Test
    public void countsEveryAttemptIncludingSkips() {
        GameSession game = start(3);

        game.submit("wrong");
        game.submit("");
        answerCorrectly(game);

        assertEquals(3, game.getAttempts());
    }

    @Test
    public void remembersWhichQuestionsAreDone() {
        GameSession game = start(3);
        long first = game.current().getId();

        answerCorrectly(game);

        assertTrue(game.isMastered(first));
        assertFalse(game.isMastered(game.current().getId()));
    }

    @Test
    public void takesNoFurtherSubmissionOnceWon() {
        GameSession game = start(2);
        answerCorrectly(game);
        answerCorrectly(game);

        try {
            game.submit("anything");
            fail("A decided game took another answer");
        } catch (IllegalStateException expected) {
            // The game screen closes at this point.
        }
    }

    private static GameSession start(int count) {
        return GameSession.start(questions(count), new Random(count * 31L));
    }

    private static List<GameQuestion> questions(int count) {
        List<GameQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            questions.add(new GameQuestion(i, "Question " + i, "Answer " + i));
        }
        return questions;
    }

    /** Answers whatever is being asked, so a test does not depend on the shuffled order. */
    private static GameSession.Outcome answerCorrectly(GameSession game) {
        return game.submit(game.current().getAnswer());
    }

    private static List<Long> askedOrder(GameSession game, int attempts) {
        List<Long> asked = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            asked.add(game.current().getId());
            game.submit("wrong");
        }
        return asked;
    }

    private static List<Long> idsOf(List<GameQuestion> questions) {
        List<Long> ids = new ArrayList<>();
        for (GameQuestion question : questions) {
            ids.add(question.getId());
        }
        return ids;
    }
}
