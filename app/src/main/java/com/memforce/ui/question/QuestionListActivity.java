package com.memforce.ui.question;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.data.CategoryDao;
import com.memforce.data.QuestionDao;
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityQuestionListBinding;
import com.memforce.model.Category;
import com.memforce.model.Question;
import com.memforce.model.Tag;
import com.memforce.ui.common.EntityAdapter;
import com.memforce.ui.common.FilterSpinner;

import java.util.ArrayList;
import java.util.List;

public class QuestionListActivity extends AppCompatActivity {

    private ActivityQuestionListBinding binding;
    private QuestionDao questionDao;
    private CategoryDao categoryDao;
    private TagDao tagDao;
    private EntityAdapter<Question> adapter;
    private FilterSpinner<Category> categoryFilter;
    private FilterSpinner<Tag> tagFilter;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, QuestionListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.menu_questions);
        questionDao = new QuestionDao(this);
        categoryDao = new CategoryDao(this);
        tagDao = new TagDao(this);

        adapter = new EntityAdapter<>(
                new EntityAdapter.Labels<Question>() {
                    @NonNull
                    @Override
                    public String title(@NonNull Question item) {
                        return item.getName();
                    }

                    @Nullable
                    @Override
                    public String subtitle(@NonNull Question item) {
                        return describe(item);
                    }
                },
                question -> startActivity(QuestionEditActivity.editIntent(this, question.getId())),
                this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        categoryFilter = new FilterSpinner<>(
                binding.categoryFilter, getString(R.string.filter_any_category), this::reload);
        tagFilter = new FilterSpinner<>(
                binding.tagFilter, getString(R.string.filter_any_tag), this::reload);

        binding.addButton.setOnClickListener(v -> startActivity(QuestionEditActivity.createIntent(this)));
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
        categoryFilter.submit(categoryDao.search(null, null));
        tagFilter.submit(tagDao.search(null));
        reload();
    }

    @Nullable
    private String describe(@NonNull Question question) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(question.getCategoryName())) {
            parts.add(question.getCategoryName());
        }
        if (!TextUtils.isEmpty(question.getTagsLabel())) {
            parts.add(question.getTagsLabel());
        }
        return parts.isEmpty() ? null : TextUtils.join(" | ", parts);
    }

    private void confirmDelete(@NonNull Question question) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.question_delete_title)
                .setMessage(question.getName())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    questionDao.delete(question.getId());
                    reload();
                })
                .show();
    }

    private void reload() {
        CharSequence pattern = binding.searchInput.getText();
        List<Question> questions = questionDao.search(
                pattern == null ? null : pattern.toString(),
                categoryFilter.getSelectedId(),
                tagFilter.getSelectedId());
        adapter.submit(questions);
        binding.emptyView.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
