package com.memforce.ui.category;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.data.CategoryDao;
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityCategoryEditBinding;
import com.memforce.model.Category;
import com.memforce.model.Tag;
import com.memforce.ui.common.TagPicker;

import java.util.ArrayList;
import java.util.List;

public class CategoryEditActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORY_ID = "category_id";
    private static final long NO_ID = -1L;

    private ActivityCategoryEditBinding binding;
    private CategoryDao categoryDao;
    private TagDao tagDao;
    private long categoryId = NO_ID;
    private final List<Long> selectedTagIds = new ArrayList<>();

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, CategoryEditActivity.class);
    }

    public static Intent editIntent(@NonNull Context context, long categoryId) {
        return createIntent(context).putExtra(EXTRA_CATEGORY_ID, categoryId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        categoryDao = new CategoryDao(this);
        tagDao = new TagDao(this);
        categoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, NO_ID);

        if (categoryId == NO_ID) {
            setTitle(R.string.category_title_new);
        } else {
            setTitle(R.string.category_title_edit);
            Category category = categoryDao.findById(categoryId);
            if (category == null) {
                finish();
                return;
            }
            binding.nameInput.setText(category.getName());
            selectedTagIds.addAll(categoryDao.tagIdsOf(categoryId));
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
        CharSequence input = binding.nameInput.getText();
        String name = input == null ? "" : input.toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.error_required));
            return;
        }

        boolean saved = categoryId == NO_ID
                ? categoryDao.insert(name, selectedTagIds) != -1
                : categoryDao.update(categoryId, name, selectedTagIds);
        if (saved) {
            finish();
        } else {
            binding.nameLayout.setError(getString(R.string.category_name_taken));
        }
    }
}
