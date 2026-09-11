package com.memforce.ui.question;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.databinding.ActivityQuestionListBinding;

public class QuestionListActivity extends AppCompatActivity {

    /** Question set files are JSON, but document providers label them inconsistently. */
    private static final String[] IMPORT_MIME_TYPES = {
            "application/json", "text/plain", "application/octet-stream"};

    /** Leaves the last rows reachable above the two buttons floating over the list. */
    private static final int LIST_BOTTOM_PADDING_DP = 88;

    private final ActivityResultLauncher<String[]> questionSetPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    new QuestionSetImportFlow(this, this::refreshSearch).start(uri);
                }
            });

    private ActivityQuestionListBinding binding;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, QuestionListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.menu_questions);

        binding.search.setOnOpenQuestion(question ->
                startActivity(QuestionEditActivity.editIntent(this, question.getId())));
        binding.search.setSwipeActionsEnabled(true);
        binding.search.setResultsBottomPadding(Math.round(
                LIST_BOTTOM_PADDING_DP * getResources().getDisplayMetrics().density));

        binding.addButton.setOnClickListener(v -> startActivity(QuestionEditActivity.createIntent(this)));
        binding.importButton.setOnClickListener(v -> questionSetPicker.launch(IMPORT_MIME_TYPES));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSearch();
    }

    private void refreshSearch() {
        binding.search.refresh();
    }
}
