package com.memforce.ui.question;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.data.CategoryDao;
import com.memforce.data.QuestionDao;
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityQuestionEditBinding;
import com.memforce.model.Category;
import com.memforce.model.Question;
import com.memforce.model.Tag;
import com.memforce.ui.common.FilterSpinner;
import com.memforce.ui.common.TagPicker;

import java.util.ArrayList;
import java.util.List;

public class QuestionEditActivity extends AppCompatActivity {

    private static final String EXTRA_QUESTION_ID = "question_id";
    private static final long NO_ID = -1L;

    private ActivityQuestionEditBinding binding;
    private QuestionDao questionDao;
    private CategoryDao categoryDao;
    private TagDao tagDao;
    private FilterSpinner<Category> categorySpinner;
    private long questionId = NO_ID;
    private final List<Long> selectedTagIds = new ArrayList<>();

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, QuestionEditActivity.class);
    }

    public static Intent editIntent(@NonNull Context context, long questionId) {
        return createIntent(context).putExtra(EXTRA_QUESTION_ID, questionId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        questionDao = new QuestionDao(this);
        categoryDao = new CategoryDao(this);
        tagDao = new TagDao(this);
        questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, NO_ID);

        categorySpinner = new FilterSpinner<>(
                binding.categorySpinner, getString(R.string.question_no_category), () -> {
        });
        categorySpinner.submit(categoryDao.search(null, null));

        if (questionId == NO_ID) {
            setTitle(R.string.question_title_new);
        } else {
            setTitle(R.string.question_title_edit);
            Question question = questionDao.findById(questionId);
            if (question == null) {
                finish();
                return;
            }
            binding.nameInput.setText(question.getName());
            binding.answerInput.setText(question.getAnswer());
            selectCategory(question.getCategoryId());
            selectedTagIds.addAll(questionDao.tagIdsOf(questionId));
        }
        showSelectedTags();

        binding.selectTagsButton.setOnClickListener(v ->
                TagPicker.show(this, tagDao.search(null), selectedTagIds, tagIds -> {
                    selectedTagIds.clear();
                    selectedTagIds.addAll(tagIds);
                    showSelectedTags();
                }));
        binding.saveButton.setOnClickListener(v -> save());
    }

    private void selectCategory(@Nullable Long categoryId) {
        if (categoryId == null) {
            return;
        }
        List<Category> categories = categoryDao.search(null, null);
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == categoryId) {
                binding.categorySpinner.setSelection(i + 1);
                return;
            }
        }
    }

    private void showSelectedTags() {
        List<String> names = new ArrayList<>();
        for (Tag tag : tagDao.search(null)) {
            if (selectedTagIds.contains(tag.getId())) {
                names.add(tag.getName());
            }
        }
        binding.selectedTags.setText(names.isEmpty()
                ? getString(R.string.tags_none_selected)
                : getString(R.string.tags_selected, TextUtils.join(", ", names)));
    }

    private void save() {
        String name = text(binding.nameInput.getText());
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.error_required));
            return;
        }
        String answer = text(binding.answerInput.getText());
        Long categoryId = categorySpinner.getSelectedId();

        if (questionId == NO_ID) {
            questionDao.insert(name, answer.isEmpty() ? null : answer, categoryId, selectedTagIds);
        } else {
            questionDao.update(questionId, name, answer.isEmpty() ? null : answer, categoryId, selectedTagIds);
        }
        finish();
    }

    private static String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
