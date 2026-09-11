package com.memforce.ui.tag;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.memforce.R;
import com.memforce.databinding.ItemTagBinding;
import com.memforce.model.TagUsage;

import java.util.ArrayList;
import java.util.List;

/**
 * The tag list: a name, and how many questions carry it.
 *
 * <p>The row holds no button. Deleting a tag and sending its questions to the lobby are both
 * swipes, which keeps the count — the reason to look at this list — the only thing competing for
 * the eye.
 */
public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {

    public interface TagAction {
        void invoke(@NonNull TagUsage tag);
    }

    private final List<TagUsage> items = new ArrayList<>();

    @Nullable
    private final TagAction onClick;

    public TagAdapter(@Nullable TagAction onClick) {
        this.onClick = onClick;
    }

    public void submit(@NonNull List<TagUsage> tags) {
        items.clear();
        items.addAll(tags);
        notifyDataSetChanged();
    }

    @NonNull
    public TagUsage at(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTagBinding.inflate(
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

        private final ItemTagBinding binding;

        ViewHolder(ItemTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull TagUsage tag) {
            binding.title.setText(tag.getName());
            binding.questionCount.setText(binding.getRoot().getResources().getQuantityString(
                    R.plurals.tag_question_count, tag.getQuestionCount(), tag.getQuestionCount()));
            binding.getRoot().setOnClickListener(onClick == null
                    ? null
                    : view -> onClick.invoke(tag));
            binding.getRoot().setClickable(onClick != null);
        }
    }
}
