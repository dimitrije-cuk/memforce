package com.memforce.ui.deck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.data.DeckDao;
import com.memforce.data.QuestionDao;
import com.memforce.databinding.ActivityDeckEditBinding;
import com.memforce.model.Deck;
import com.memforce.model.Question;
import com.memforce.session.Session;

import java.util.ArrayList;
import java.util.List;

public class DeckEditActivity extends AppCompatActivity {

    private static final String EXTRA_DECK_ID = "deck_id";
    private static final long NO_ID = -1L;

    private ActivityDeckEditBinding binding;
    private DeckDao deckDao;
    private QuestionDao questionDao;
    private long deckId = NO_ID;
    private final List<Long> selectedQuestionIds = new ArrayList<>();

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, DeckEditActivity.class);
    }

    public static Intent editIntent(@NonNull Context context, long deckId) {
        return createIntent(context).putExtra(EXTRA_DECK_ID, deckId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeckEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        deckDao = new DeckDao(this, new Session(this).getUserId());
        questionDao = new QuestionDao(this);
        deckId = getIntent().getLongExtra(EXTRA_DECK_ID, NO_ID);

        if (deckId == NO_ID) {
            setTitle(R.string.deck_title_new);
        } else {
            setTitle(R.string.deck_title_edit);
            Deck deck = deckDao.findById(deckId);
            if (deck == null) {
                finish();
                return;
            }
            binding.nameInput.setText(deck.getName());
            selectedQuestionIds.addAll(deckDao.questionIdsOf(deckId));
        }
        showSelectedQuestions();

        binding.selectQuestionsButton.setOnClickListener(v -> pickQuestions());
        binding.saveButton.setOnClickListener(v -> save());
    }

    private void pickQuestions() {
        List<Question> questions = questionDao.search(null, null, null);
        if (questions.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.deck_no_questions_defined)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] labels = new String[questions.size()];
        boolean[] checked = new boolean[questions.size()];
        for (int i = 0; i < questions.size(); i++) {
            labels[i] = questions.get(i).getName();
            checked[i] = selectedQuestionIds.contains(questions.get(i).getId());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.deck_select_questions)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedQuestionIds.clear();
                    for (int i = 0; i < questions.size(); i++) {
                        if (checked[i]) {
                            selectedQuestionIds.add(questions.get(i).getId());
                        }
                    }
                    showSelectedQuestions();
                })
                .show();
    }

    private void showSelectedQuestions() {
        binding.selectedQuestions.setText(getResources().getQuantityString(
                R.plurals.deck_selected_questions,
                selectedQuestionIds.size(),
                selectedQuestionIds.size()));
    }

    private void save() {
        CharSequence input = binding.nameInput.getText();
        String name = input == null ? "" : input.toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.error_required));
            return;
        }

        boolean saved = deckId == NO_ID
                ? deckDao.insert(name, selectedQuestionIds) != -1
                : deckDao.update(deckId, name, selectedQuestionIds);
        if (saved) {
            finish();
        } else {
            binding.nameLayout.setError(getString(R.string.deck_name_taken));
        }
    }
}
