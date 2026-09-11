package com.memforce.ui.tag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.memforce.R;
import com.memforce.data.QuestionDao;
import com.memforce.data.TagDao;
import com.memforce.databinding.ActivityTagListBinding;
import com.memforce.game.Lobby;
import com.memforce.model.TagSort;
import com.memforce.model.TagUsage;
import com.memforce.ui.common.SwipeActions;

import java.util.List;

public class TagListActivity extends AppCompatActivity {

    /** The orders offered, in the order of the labels the spinner shows. */
    private static final TagSort[] SORTS = {
            TagSort.MOST_USED, TagSort.ALPHABETICAL, TagSort.LEAST_USED};

    private ActivityTagListBinding binding;
    private TagDao tagDao;
    private QuestionDao questionDao;
    private Lobby lobby;
    private TagAdapter adapter;

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
        questionDao = new QuestionDao(this);
        lobby = new Lobby(this);

        adapter = new TagAdapter(tag -> startActivity(TagEditActivity.editIntent(this, tag.getId())));
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        SwipeActions.attach(binding.list, this::confirmDelete, this::addToLobby);

        ArrayAdapter<String> sortLabels = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{
                getString(R.string.tag_sort_most_used),
                getString(R.string.tag_sort_alphabetical),
                getString(R.string.tag_sort_least_used)});
        sortLabels.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sortSpinner.setAdapter(sortLabels);
        binding.sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reload();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

    private void confirmDelete(int position) {
        TagUsage tag = adapter.at(position);
        boolean[] deleted = {false};
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.tag_delete_title, tag.getName()))
                .setMessage(R.string.tag_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    deleted[0] = true;
                    tagDao.delete(tag.getId());
                    reload();
                })
                .setOnDismissListener(dialog -> {
                    if (!deleted[0]) {
                        // The swiped row is put back when the tag was kept.
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    /** Puts every question carrying the tag into the lobby, which is the swipe to the right. */
    private void addToLobby(int position) {
        TagUsage tag = adapter.at(position);
        adapter.notifyItemChanged(position);
        List<Long> questionIds = questionDao.idsWithTag(tag.getId());
        if (questionIds.isEmpty()) {
            show(getString(R.string.lobby_tag_empty, tag.getName()));
            return;
        }
        int added = lobby.add(questionIds);
        show(added == 0
                ? getString(R.string.lobby_nothing_added)
                : getResources().getQuantityString(R.plurals.lobby_added, added, added));
    }

    private void show(@NonNull String message) {
        Snackbar.make(binding.list, message, Snackbar.LENGTH_SHORT).show();
    }

    private void reload() {
        CharSequence pattern = binding.searchInput.getText();
        List<TagUsage> tags = tagDao.searchWithCounts(
                pattern == null ? null : pattern.toString(), selectedSort());
        adapter.submit(tags);
        binding.emptyView.setVisibility(tags.isEmpty() ? View.VISIBLE : View.GONE);

        int total = tagDao.countAll();
        binding.totalCount.setText(
                getResources().getQuantityString(R.plurals.tag_total_count, total, total));
    }

    @NonNull
    private TagSort selectedSort() {
        int position = binding.sortSpinner.getSelectedItemPosition();
        return position >= 0 && position < SORTS.length ? SORTS[position] : TagSort.MOST_USED;
    }
}
