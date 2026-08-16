package com.memforce.ui.category;

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
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityCategoryListBinding;
import com.memforce.model.Category;
import com.memforce.model.Tag;
import com.memforce.ui.common.EntityAdapter;
import com.memforce.ui.common.FilterSpinner;

import java.util.List;

public class CategoryListActivity extends AppCompatActivity {

    private ActivityCategoryListBinding binding;
    private CategoryDao categoryDao;
    private TagDao tagDao;
    private EntityAdapter<Category> adapter;
    private FilterSpinner<Tag> tagFilter;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, CategoryListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.menu_categories);
        categoryDao = new CategoryDao(this);
        tagDao = new TagDao(this);

        adapter = new EntityAdapter<>(
                new EntityAdapter.Labels<Category>() {
                    @NonNull
                    @Override
                    public String title(@NonNull Category item) {
                        return item.getName();
                    }

                    @Nullable
                    @Override
                    public String subtitle(@NonNull Category item) {
                        return TextUtils.isEmpty(item.getTagsLabel()) ? null : item.getTagsLabel();
                    }
                },
                category -> startActivity(CategoryEditActivity.editIntent(this, category.getId())),
                this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        tagFilter = new FilterSpinner<>(binding.tagFilter, getString(R.string.filter_any_tag), this::reload);
        binding.addButton.setOnClickListener(v -> startActivity(CategoryEditActivity.createIntent(this)));
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
        tagFilter.submit(tagDao.search(null));
        reload();
    }

    private void confirmDelete(@NonNull Category category) {
        int affected = categoryDao.countQuestionsIn(category.getId());
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.category_delete_title, category.getName()))
                .setMessage(getResources().getQuantityString(
                        R.plurals.category_delete_message, affected, affected))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    categoryDao.delete(category.getId());
                    reload();
                })
                .show();
    }

    private void reload() {
        CharSequence pattern = binding.searchInput.getText();
        List<Category> categories = categoryDao.search(
                pattern == null ? null : pattern.toString(), tagFilter.getSelectedId());
        adapter.submit(categories);
        binding.emptyView.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
