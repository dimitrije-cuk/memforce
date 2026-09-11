package com.memforce.ui.game;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.memforce.R;
import com.memforce.databinding.ItemLobbyQuestionBinding;
import com.memforce.model.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * The questions gathered for the next game.
 *
 * <p>A question stored without an answer is shown too, marked as one the game leaves out, so that
 * a lobby the game will shorten does not do so silently.
 */
public class LobbyAdapter extends RecyclerView.Adapter<LobbyAdapter.ViewHolder> {

    public interface QuestionAction {
        void invoke(@NonNull Question question);
    }

    private final List<Question> items = new ArrayList<>();
    private final QuestionAction onRemove;

    public LobbyAdapter(@NonNull QuestionAction onRemove) {
        this.onRemove = onRemove;
    }

    public void submit(@NonNull List<Question> questions) {
        items.clear();
        items.addAll(questions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemLobbyQuestionBinding.inflate(
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

        private final ItemLobbyQuestionBinding binding;

        ViewHolder(ItemLobbyQuestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Question question) {
            View root = binding.getRoot();
            binding.title.setText(question.getName());
            binding.answer.setText(question.isAnswerable()
                    ? root.getContext().getString(R.string.lobby_answer, question.getAnswer())
                    : root.getContext().getString(R.string.lobby_no_answer));
            binding.removeButton.setOnClickListener(v -> onRemove.invoke(question));
        }
    }
}
