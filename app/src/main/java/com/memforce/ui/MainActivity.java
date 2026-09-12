package com.memforce.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;

import com.memforce.R;
import com.memforce.databinding.ActivityMainBinding;
import com.memforce.game.Lobby;
import com.memforce.session.Session;
import com.memforce.ui.game.LobbyActivity;
import com.memforce.ui.login.LoginActivity;
import com.memforce.ui.question.QuestionEditActivity;
import com.memforce.ui.question.QuestionListActivity;
import com.memforce.ui.tag.TagListActivity;

/**
 * The screen a signed-in user lands on: the search over every question, and nothing else.
 *
 * <p>The heading, the search and its results have the screen to themselves, because browsing is
 * what a user comes here to do. The ways off it — the lobby, the question list, the tag list and
 * sign-out — and the name of the signed-in user sit behind the menu button, one press away and
 * taking no room from the questions.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Session session;
    private Lobby lobby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new Session(this);
        if (!session.isSignedIn()) {
            openLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        lobby = new Lobby(this);

        binding.search.setOnOpenQuestion(question ->
                startActivity(QuestionEditActivity.editIntent(this, question.getId())));
        binding.menuButton.setOnClickListener(this::showMenu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        binding.search.refresh();
    }

    /**
     * Builds the menu afresh on every press, so the lobby count it names and the user it greets
     * are the ones that hold now rather than the ones that held when the screen was drawn.
     */
    private void showMenu(@NonNull View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.inflate(R.menu.main);
        MenuCompat.setGroupDividerEnabled(menu.getMenu(), true);
        menu.getMenu().findItem(R.id.menuSignedInAs)
                .setTitle(getString(R.string.menu_greeting, session.getUserName()));
        menu.getMenu().findItem(R.id.menuLobby)
                .setTitle(getString(R.string.menu_lobby, lobby.size()));
        menu.setOnMenuItemClickListener(this::onMenuItemSelected);
        menu.show();
    }

    private boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuLobby) {
            startActivity(LobbyActivity.createIntent(this));
        } else if (id == R.id.menuQuestions) {
            startActivity(QuestionListActivity.createIntent(this));
        } else if (id == R.id.menuTags) {
            startActivity(TagListActivity.createIntent(this));
        } else if (id == R.id.menuSignOut) {
            session.signOut();
            openLogin();
        } else {
            return false;
        }
        return true;
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
