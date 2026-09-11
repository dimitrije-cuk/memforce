package com.memforce.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.databinding.ActivityGameBinding;
import com.memforce.game.CurrentGame;
import com.memforce.game.GameQuestion;
import com.memforce.game.GameSession;

/**
 * Asks the questions of the current run and shows how far the player has got.
 *
 * <p>The screen has two states: waiting for a submission, and showing what the submission was
 * worth. The same button carries both, so the player's thumb never has to move between answering
 * and going on.
 *
 * <p>A run lives in memory only. Coming back to this screen after the process was restarted
 * therefore returns to the lobby rather than resuming a run whose progress is gone.
 */
public class GameActivity extends AppCompatActivity {

    private ActivityGameBinding binding;
    private GameSession session;
    private boolean showingFeedback;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, GameActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameSession current = CurrentGame.get();
        if (current == null || current.isFinished()) {
            finish();
            return;
        }
        session = current;
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.game_title);

        binding.primaryButton.setOnClickListener(v -> {
            if (showingFeedback) {
                askNext();
            } else {
                submit(text());
            }
        });
        binding.skipButton.setOnClickListener(v -> submit(""));
        binding.answerInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && !showingFeedback) {
                submit(text());
                return true;
            }
            return false;
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });
        askNext();
    }

    private void submit(@Nullable String answer) {
        GameQuestion asked = session.current();
        GameSession.Outcome outcome = session.submit(answer);
        showProgress();

        if (session.isFinished()) {
            announceResult();
            return;
        }
        showingFeedback = true;
        binding.feedback.setVisibility(View.VISIBLE);
        binding.feedback.setText(feedbackFor(outcome, asked));
        binding.answerLayout.setEnabled(false);
        binding.answerInput.setEnabled(false);
        binding.skipButton.setEnabled(false);
        binding.primaryButton.setText(R.string.action_next);
    }

    @NonNull
    private String feedbackFor(@NonNull GameSession.Outcome outcome, @NonNull GameQuestion asked) {
        switch (outcome) {
            case CORRECT:
                return getString(R.string.game_correct);
            case SKIPPED:
                return getString(R.string.game_skipped, asked.getAnswer());
            case INCORRECT:
            default:
                return getString(R.string.game_incorrect, asked.getAnswer());
        }
    }

    private void askNext() {
        showingFeedback = false;
        binding.prompt.setText(session.current().getPrompt());
        binding.answerInput.setText("");
        binding.answerLayout.setEnabled(true);
        binding.answerInput.setEnabled(true);
        binding.skipButton.setEnabled(true);
        binding.feedback.setVisibility(View.GONE);
        binding.primaryButton.setText(R.string.action_submit);
        binding.answerInput.requestFocus();
        showProgress();
    }

    private void showProgress() {
        binding.progress.setText(getString(R.string.game_progress,
                session.getStreak(), session.getCorrectCount(), session.getTotal()));
    }

    private void announceResult() {
        boolean perfect = session.getResult() == GameSession.Result.PERFECT_VICTORY;
        new MaterialAlertDialogBuilder(this)
                .setTitle(perfect ? R.string.game_perfect_title : R.string.game_victory_title)
                .setMessage(perfect
                        ? getString(R.string.game_perfect_message, session.getTotal())
                        : getString(R.string.game_victory_message,
                        session.getTotal(), session.getAttempts()))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> leave())
                .show();
    }

    private void confirmLeave() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.game_leave_title)
                .setMessage(R.string.game_leave_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_leave, (dialog, which) -> leave())
                .show();
    }

    private void leave() {
        CurrentGame.end();
        finish();
    }

    @NonNull
    private String text() {
        CharSequence value = binding.answerInput.getText();
        return value == null ? "" : value.toString();
    }
}
