package com.memforce.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.databinding.ActivityMainBinding;
import com.memforce.game.Lobby;
import com.memforce.session.Session;
import com.memforce.ui.game.LobbyActivity;
import com.memforce.ui.login.LoginActivity;
import com.memforce.ui.question.QuestionEditActivity;
import com.memforce.ui.question.QuestionListActivity;
import com.memforce.ui.tag.TagListActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Lobby lobby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = new Session(this);
        if (!session.isSignedIn()) {
            openLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        lobby = new Lobby(this);
        binding.greeting.setText(getString(R.string.menu_greeting, session.getUserName()));

        binding.search.setOnOpenQuestion(question ->
                startActivity(QuestionEditActivity.editIntent(this, question.getId())));
        binding.search.setOnLobbyChanged(this::showLobbyCount);

        binding.lobbyButton.setOnClickListener(v -> startActivity(LobbyActivity.createIntent(this)));
        binding.questionsButton.setOnClickListener(v -> startActivity(QuestionListActivity.createIntent(this)));
        binding.tagsButton.setOnClickListener(v -> startActivity(TagListActivity.createIntent(this)));
        binding.signOutButton.setOnClickListener(v -> {
            session.signOut();
            openLogin();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        binding.search.refresh();
        showLobbyCount();
    }

    private void showLobbyCount() {
        binding.lobbyButton.setText(getString(R.string.menu_lobby, lobby.size()));
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
