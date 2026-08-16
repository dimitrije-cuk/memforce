package com.memforce.ui.tag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityTagEditBinding;
import com.memforce.model.Tag;

public class TagEditActivity extends AppCompatActivity {

    private static final String EXTRA_TAG_ID = "tag_id";
    private static final long NO_ID = -1L;

    private ActivityTagEditBinding binding;
    private TagDao tagDao;
    private long tagId = NO_ID;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, TagEditActivity.class);
    }

    public static Intent editIntent(@NonNull Context context, long tagId) {
        return createIntent(context).putExtra(EXTRA_TAG_ID, tagId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tagDao = new TagDao(this);
        tagId = getIntent().getLongExtra(EXTRA_TAG_ID, NO_ID);

        if (tagId == NO_ID) {
            setTitle(R.string.tag_title_new);
        } else {
            setTitle(R.string.tag_title_edit);
            Tag tag = tagDao.findById(tagId);
            if (tag == null) {
                finish();
                return;
            }
            binding.nameInput.setText(tag.getName());
        }
        binding.saveButton.setOnClickListener(v -> save());
    }

    private void save() {
        CharSequence input = binding.nameInput.getText();
        String name = input == null ? "" : input.toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.error_required));
            return;
        }

        boolean saved = tagId == NO_ID ? tagDao.insert(name) != -1 : tagDao.update(tagId, name);
        if (saved) {
            finish();
        } else {
            binding.nameLayout.setError(getString(R.string.tag_name_taken));
        }
    }
}
