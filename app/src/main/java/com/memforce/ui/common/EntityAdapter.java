package com.memforce.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.memforce.databinding.ItemEntityBinding;

import java.util.ArrayList;
import java.util.List;

/** Renders a title, an optional subtitle and a delete button for any list screen. */
public class EntityAdapter<T> extends RecyclerView.Adapter<EntityAdapter<T>.ViewHolder> {

    public interface Labels<T> {
        @NonNull
        String title(@NonNull T item);

        @Nullable
        String subtitle(@NonNull T item);
    }

    public interface ItemAction<T> {
        void invoke(@NonNull T item);
    }

    private final List<T> items = new ArrayList<>();
    private final Labels<T> labels;
    private final ItemAction<T> onEdit;
    private final ItemAction<T> onDelete;

    public EntityAdapter(@NonNull Labels<T> labels,
                         @NonNull ItemAction<T> onEdit,
                         @NonNull ItemAction<T> onDelete) {
        this.labels = labels;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    public void submit(@NonNull List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemEntityBinding.inflate(
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

        private final ItemEntityBinding binding;

        ViewHolder(ItemEntityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(T item) {
            binding.title.setText(labels.title(item));
            String subtitle = labels.subtitle(item);
            binding.subtitle.setText(subtitle);
            binding.subtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> onEdit.invoke(item));
            binding.deleteButton.setOnClickListener(v -> onDelete.invoke(item));
        }
    }
}
