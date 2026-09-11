package com.memforce.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.memforce.R;
import com.memforce.databinding.ItemQuestionResultBinding;
import com.memforce.model.Question;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The result list of the search: one row per question, with its tags and a selection box.
 *
 * <p>The tags are laid out in a strip that scrolls sideways, so a question carrying a dozen tags
 * takes exactly as much height as one carrying none and the question text stays the first thing
 * read.
 *
 * <p>A selection is made of question identifiers rather than positions, so it survives the list
 * being read again; identifiers that the new result no longer contains are dropped, because a
 * question the user can no longer see must not be added to the lobby behind their back.
 */
public class QuestionResultAdapter extends RecyclerView.Adapter<QuestionResultAdapter.ViewHolder> {

    public interface QuestionAction {
        void invoke(@NonNull Question question);
    }

    private final List<Question> items = new ArrayList<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private final Runnable onSelectionChanged;

    @Nullable
    private QuestionAction onClick;

    public QuestionResultAdapter(@NonNull Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOnClick(@Nullable QuestionAction onClick) {
        this.onClick = onClick;
    }

    public void submit(@NonNull List<Question> questions) {
        items.clear();
        items.addAll(questions);

        Set<Long> stillShown = new LinkedHashSet<>();
        for (Question question : questions) {
            if (selectedIds.contains(question.getId())) {
                stillShown.add(question.getId());
            }
        }
        boolean selectionChanged = !stillShown.equals(selectedIds);
        selectedIds.clear();
        selectedIds.addAll(stillShown);

        notifyDataSetChanged();
        if (selectionChanged) {
            onSelectionChanged.run();
        }
    }

    @NonNull
    public Question at(int position) {
        return items.get(position);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** The questions ticked, in the order they were ticked. */
    @NonNull
    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    /** True when every shown question is ticked, which an empty result never is. */
    public boolean isAllSelected() {
        return !items.isEmpty() && selectedIds.size() == items.size();
    }

    /** Ticks or unticks every question the current result shows. */
    public void selectAll(boolean selected) {
        selectedIds.clear();
        if (selected) {
            for (Question question : items) {
                selectedIds.add(question.getId());
            }
        }
        notifyDataSetChanged();
        onSelectionChanged.run();
    }

    public void clearSelection() {
        if (selectedIds.isEmpty()) {
            return;
        }
        selectedIds.clear();
        notifyDataSetChanged();
        onSelectionChanged.run();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemQuestionResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemQuestionResultBinding binding;

        ViewHolder(ItemQuestionResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Question question) {
            binding.title.setText(question.getName());
            bindTags(question);

            binding.selected.setOnCheckedChangeListener(null);
            binding.selected.setChecked(selectedIds.contains(question.getId()));
            binding.selected.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(question.getId());
                } else {
                    selectedIds.remove(question.getId());
                }
                onSelectionChanged.run();
            });

            binding.getRoot().setOnClickListener(onClick == null
                    ? null
                    : view -> onClick.invoke(question));
            binding.getRoot().setClickable(onClick != null);
        }

        private void bindTags(@NonNull Question question) {
            binding.tags.removeAllViews();
            binding.tagScroll.setVisibility(
                    question.getTagNames().isEmpty() ? View.GONE : View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
            for (String tag : question.getTagNames()) {
                TextView label = (TextView) inflater.inflate(
                        R.layout.item_tag_label, binding.tags, false);
                label.setText(tag);
                binding.tags.addView(label);
            }
            binding.tagScroll.scrollTo(0, 0);
        }
    }
}
