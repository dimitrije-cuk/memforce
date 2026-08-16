package com.memforce.ui.common;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.model.Named;

import java.util.ArrayList;
import java.util.List;

/** Spinner that filters a list by one entity, with a leading "any" entry meaning no filter. */
public class FilterSpinner<T extends Named> {

    private final Spinner spinner;
    private final String anyLabel;
    private final List<T> items = new ArrayList<>();

    public FilterSpinner(@NonNull Spinner spinner, @NonNull String anyLabel, @NonNull Runnable onChanged) {
        this.spinner = spinner;
        this.anyLabel = anyLabel;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onChanged.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void submit(@NonNull List<T> newItems) {
        Long previous = getSelectedId();
        items.clear();
        items.addAll(newItems);

        List<String> labels = new ArrayList<>();
        labels.add(anyLabel);
        for (T item : items) {
            labels.add(item.getName());
        }

        Context context = spinner.getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(indexOf(previous));
    }

    @Nullable
    public Long getSelectedId() {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position > items.size()) {
            return null;
        }
        return items.get(position - 1).getId();
    }

    private int indexOf(@Nullable Long id) {
        if (id == null) {
            return 0;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                return i + 1;
            }
        }
        return 0;
    }
}
