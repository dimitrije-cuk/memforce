package com.memforce.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.data.QuestionDao;
import com.memforce.databinding.ActivityLobbyBinding;
import com.memforce.game.CurrentGame;
import com.memforce.game.GameQuestion;
import com.memforce.game.GameSession;
import com.memforce.game.Lobby;
import com.memforce.model.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * The gameplay lobby: what has been gathered from the search, the tags and the question list, and
 * the place a game is started from.
 *
 * <p>The lobby stores identifiers, so every visit reads the questions again. That is also where a
 * question deleted since it was added drops out.
 */
public class LobbyActivity extends AppCompatActivity {

    private ActivityLobbyBinding binding;
    private QuestionDao questionDao;
    private Lobby lobby;
    private LobbyAdapter adapter;
    private final List<Question> questions = new ArrayList<>();

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, LobbyActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.lobby_title);
        questionDao = new QuestionDao(this);
        lobby = new Lobby(this);

        adapter = new LobbyAdapter(question -> {
            lobby.remove(question.getId());
            reload();
        });
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.clearButton.setOnClickListener(v -> confirmClear());
        binding.startButton.setOnClickListener(v -> startGame());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        questions.clear();
        questions.addAll(questionDao.findByIds(lobby.questionIds()));

        List<Long> stillStored = new ArrayList<>();
        for (Question question : questions) {
            stillStored.add(question.getId());
        }
        lobby.retainAll(stillStored);

        adapter.submit(questions);
        binding.emptyView.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
        binding.summary.setText(getResources().getQuantityString(
                R.plurals.lobby_summary, questions.size(), questions.size()));

        int unanswerable = questions.size() - answerable().size();
        binding.unanswerableNote.setVisibility(unanswerable == 0 ? View.GONE : View.VISIBLE);
        binding.unanswerableNote.setText(getString(R.string.lobby_unanswerable_note, unanswerable));

        binding.startButton.setEnabled(!questions.isEmpty());
        binding.clearButton.setEnabled(!questions.isEmpty());
    }

    @NonNull
    private List<Question> answerable() {
        List<Question> playable = new ArrayList<>();
        for (Question question : questions) {
            if (question.isAnswerable()) {
                playable.add(question);
            }
        }
        return playable;
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lobby_clear_title)
                .setMessage(R.string.lobby_clear_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear_lobby, (dialog, which) -> {
                    lobby.clear();
                    reload();
                })
                .show();
    }

    private void startGame() {
        List<Question> playable = answerable();
        if (playable.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.game_none_answerable_title)
                    .setMessage(R.string.game_none_answerable_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        int excluded = questions.size() - playable.size();
        if (excluded == 0) {
            play(playable);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.game_excluded_title)
                .setMessage(getString(R.string.game_excluded_message, excluded, playable.size()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_start_game, (dialog, which) -> play(playable))
                .show();
    }

    private void play(@NonNull List<Question> playable) {
        List<GameQuestion> asked = new ArrayList<>(playable.size());
        for (Question question : playable) {
            String answer = question.getAnswer();
            if (answer != null) {
                asked.add(new GameQuestion(question.getId(), question.getName(), answer,
                        question.getAlternativeAnswers()));
            }
        }
        CurrentGame.start(GameSession.start(asked));
        startActivity(GameActivity.createIntent(this));
    }
}
