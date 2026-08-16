package com.memforce.ui.deck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.data.DeckDao;
import com.memforce.databinding.ActivityDeckListBinding;
import com.memforce.model.Deck;
import com.memforce.session.Session;
import com.memforce.ui.common.EntityAdapter;

import java.util.List;

public class DeckListActivity extends AppCompatActivity {

    private ActivityDeckListBinding binding;
    private DeckDao deckDao;
    private EntityAdapter<Deck> adapter;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, DeckListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeckListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.menu_decks);
        deckDao = new DeckDao(this, new Session(this).getUserId());

        adapter = new EntityAdapter<>(
                new EntityAdapter.Labels<Deck>() {
                    @NonNull
                    @Override
                    public String title(@NonNull Deck item) {
                        return item.getName();
                    }

                    @Nullable
                    @Override
                    public String subtitle(@NonNull Deck item) {
                        return getResources().getQuantityString(
                                R.plurals.deck_question_count, item.getQuestionCount(), item.getQuestionCount());
                    }
                },
                deck -> startActivity(DeckEditActivity.editIntent(this, deck.getId())),
                this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.addButton.setOnClickListener(v -> startActivity(DeckEditActivity.createIntent(this)));
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                reload();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void confirmDelete(@NonNull Deck deck) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.deck_delete_title, deck.getName()))
                .setMessage(R.string.deck_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    deckDao.delete(deck.getId());
                    reload();
                })
                .show();
    }

    private void reload() {
        CharSequence pattern = binding.searchInput.getText();
        List<Deck> decks = deckDao.search(pattern == null ? null : pattern.toString());
        adapter.submit(decks);
        binding.emptyView.setVisibility(decks.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
