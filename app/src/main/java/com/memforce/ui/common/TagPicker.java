package com.memforce.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.model.Tag;

import java.util.ArrayList;
import java.util.List;

/** Multi choice dialog used wherever tags are assigned. */
public final class TagPicker {

    public interface OnTagsSelected {
        void onTagsSelected(@NonNull List<Long> tagIds);
    }

    private TagPicker() {
    }

    public static void show(@NonNull Context context,
                            @NonNull List<Tag> tags,
                            @NonNull List<Long> selectedTagIds,
                            @NonNull OnTagsSelected callback) {
        if (tags.isEmpty()) {
            new MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.tags_none_defined)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] names = new String[tags.size()];
        boolean[] checked = new boolean[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            names[i] = tags.get(i).getName();
            checked[i] = selectedTagIds.contains(tags.get(i).getId());
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.tags_select)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    List<Long> result = new ArrayList<>();
                    for (int i = 0; i < tags.size(); i++) {
                        if (checked[i]) {
                            result.add(tags.get(i).getId());
                        }
                    }
                    callback.onTagsSelected(result);
                })
                .show();
    }
}
