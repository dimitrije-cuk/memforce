package com.memforce.ui.tag;

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
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityTagListBinding;
import com.memforce.model.Tag;
import com.memforce.ui.common.EntityAdapter;

import java.util.List;

public class TagListActivity extends AppCompatActivity {

    private ActivityTagListBinding binding;
    private TagDao tagDao;
    private EntityAdapter<Tag> adapter;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, TagListActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.menu_tags);
        tagDao = new TagDao(this);

        adapter = new EntityAdapter<>(
                new EntityAdapter.Labels<Tag>() {
                    @NonNull
                    @Override
                    public String title(@NonNull Tag item) {
                        return item.getName();
                    }

                    @Nullable
                    @Override
                    public String subtitle(@NonNull Tag item) {
                        return null;
                    }
                },
                tag -> startActivity(TagEditActivity.editIntent(this, tag.getId())),
                this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.addButton.setOnClickListener(v -> startActivity(TagEditActivity.createIntent(this)));
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

    private void confirmDelete(@NonNull Tag tag) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.tag_delete_title, tag.getName()))
                .setMessage(R.string.tag_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    tagDao.delete(tag.getId());
                    reload();
                })
                .show();
    }

    private void reload() {
        CharSequence pattern = binding.searchInput.getText();
        List<Tag> tags = tagDao.search(pattern == null ? null : pattern.toString());
        adapter.submit(tags);
        binding.emptyView.setVisibility(tags.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
