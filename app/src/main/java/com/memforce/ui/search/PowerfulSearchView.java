package com.memforce.ui.search;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.memforce.R;
import com.memforce.data.QuestionDao;
import com.memforce.data.TagDao;
import com.memforce.databinding.ViewPowerfulSearchBinding;
import com.memforce.game.Lobby;
import com.memforce.model.Question;
import com.memforce.model.Tag;
import com.memforce.model.TagUsage;
import com.memforce.search.SearchQuery;
import com.memforce.ui.common.SwipeActions;

import java.util.ArrayList;
import java.util.List;

/**
 * The one search the application offers, used wherever questions are looked for.
 *
 * <p>It carries the whole of a search rather than a field alone: the text, the tags chosen so far,
 * the tags worth choosing next, the questions that match, and what may be done with the ones
 * picked out. Both the home screen and the question list show this view, so a user learns the
 * search once and the two screens cannot drift apart.
 *
 * <p>Text matches question texts and tag names alike, so typing narrows by either; each tag chosen
 * on top of it narrows further. What is typed is looked for wherever it stands in the value —
 * {@link com.memforce.db.SearchPatterns} encloses it in {@code %} on the way to the query, so the
 * field goes on showing the words the user typed. The suggestions are the tags carried by the
 * questions currently found, most used first, which means a tag that has no question in common
 * with what is already chosen is not offered at all.
 *
 * <p>What the screen embedding the view decides: whether tapping a row opens the question
 * ({@link #setOnOpenQuestion}), and whether rows may be swiped
 * ({@link #setSwipeActionsEnabled}).
 */
public class PowerfulSearchView extends LinearLayout {

    /** How many tags to offer; more than this does not fit a strip the thumb can scan. */
    private static final int MAX_SUGGESTIONS = 24;

    private ViewPowerfulSearchBinding binding;
    private QuestionResultAdapter adapter;
    private QuestionDao questionDao;
    private TagDao tagDao;
    private Lobby lobby;

    private SearchQuery query = SearchQuery.empty();

    public PowerfulSearchView(@NonNull Context context) {
        this(context, null);
    }

    public PowerfulSearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        binding = ViewPowerfulSearchBinding.inflate(LayoutInflater.from(context), this);
        if (isInEditMode()) {
            return;
        }
        questionDao = new QuestionDao(context);
        tagDao = new TagDao(context);
        lobby = new Lobby(context);

        adapter = new QuestionResultAdapter(this::showSelection);
        binding.results.setLayoutManager(new LinearLayoutManager(context));
        binding.results.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = query.withText(s == null ? "" : s.toString());
                refresh();
            }
        });
        binding.selectAll.setOnClickListener(v -> adapter.selectAll(binding.selectAll.isChecked()));
        binding.selectAllLabel.setOnClickListener(v -> binding.selectAll.performClick());
        binding.addToLobbyButton.setOnClickListener(v -> addSelectionToLobby());
        showSelection();
    }

    /** Set to open a question when its row is tapped; leave unset to make rows inert. */
    public void setOnOpenQuestion(@Nullable QuestionResultAdapter.QuestionAction action) {
        adapter.setOnClick(action);
    }

    /**
     * Allows a row to be swiped left to delete the question, after a confirmation, and right to put
     * it into the lobby. Call once; the gestures cannot be taken away again.
     */
    public void setSwipeActionsEnabled(boolean enabled) {
        if (!enabled) {
            return;
        }
        SwipeActions.attach(binding.results, this::confirmDelete, this::addRowToLobby);
    }

    /** Keeps the last rows clear of anything floating over the bottom of the screen. */
    public void setResultsBottomPadding(int pixels) {
        RecyclerView results = binding.results;
        results.setPadding(results.getPaddingLeft(), results.getPaddingTop(),
                results.getPaddingRight(), pixels);
        results.setClipToPadding(false);
    }

    /** Reads the suggestions and the results again, after the stored data may have changed. */
    public void refresh() {
        List<Tag> chosen = resolveChosenTags();
        List<Question> questions = questionDao.search(query);
        adapter.submit(questions);
        binding.emptyView.setVisibility(questions.isEmpty() ? VISIBLE : GONE);
        showChosenTags(chosen);
        showSuggestions();
        showSelection();
    }

    /**
     * Resolves the chosen tags, dropping any that has been deleted since it was chosen.
     *
     * <p>Dropping it is what keeps the search usable: a criterion naming a tag no question can
     * carry matches nothing, and the chip that would let the user take it off again is the very
     * one that can no longer be drawn, so the screen would be stuck showing an empty result with
     * nothing on it to undo.
     */
    @NonNull
    private List<Tag> resolveChosenTags() {
        List<Tag> chosen = new ArrayList<>();
        SearchQuery kept = query;
        for (Long tagId : query.getTagIds()) {
            Tag tag = tagDao.findById(tagId);
            if (tag == null) {
                kept = kept.withoutTag(tagId);
            } else {
                chosen.add(tag);
            }
        }
        query = kept;
        return chosen;
    }

    private void showChosenTags(@NonNull List<Tag> chosen) {
        binding.selectedTags.removeAllViews();
        for (Tag tag : chosen) {
            Chip chip = new Chip(getContext());
            chip.setText(tag.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                query = query.withoutTag(tag.getId());
                refresh();
            });
            binding.selectedTags.addView(chip);
        }
        binding.selectedTags.setVisibility(chosen.isEmpty() ? GONE : VISIBLE);
    }

    private void showSuggestions() {
        List<TagUsage> suggestions = tagDao.suggest(query, MAX_SUGGESTIONS);
        binding.suggestions.removeAllViews();
        for (TagUsage suggestion : suggestions) {
            Chip chip = new Chip(getContext());
            chip.setText(getContext().getString(R.string.search_tag_suggestion,
                    suggestion.getName(), suggestion.getQuestionCount()));
            chip.setOnClickListener(v -> {
                query = query.withTag(suggestion.getId());
                refresh();
            });
            binding.suggestions.addView(chip);
        }
        int visibility = suggestions.isEmpty() ? GONE : VISIBLE;
        binding.suggestionsLabel.setVisibility(visibility);
        binding.suggestionsScroll.setVisibility(visibility);
        binding.suggestionsScroll.scrollTo(0, 0);
    }

    private void showSelection() {
        int count = adapter.getSelectedCount();
        binding.selectionCount.setText(getResources().getQuantityString(
                R.plurals.search_selection_count, count, count));
        binding.addToLobbyButton.setEnabled(count > 0);
        binding.selectAll.setChecked(adapter.isAllSelected());
        binding.selectAll.setEnabled(!adapter.isEmpty());
        binding.selectAllLabel.setEnabled(!adapter.isEmpty());
    }

    private void addSelectionToLobby() {
        putIntoLobby(lobby.add(adapter.getSelectedIds()));
        adapter.clearSelection();
    }

    private void addRowToLobby(int position) {
        Question question = adapter.at(position);
        adapter.notifyItemChanged(position);
        putIntoLobby(lobby.add(question.getId()) ? 1 : 0);
    }

    private void putIntoLobby(int added) {
        Snackbar.make(this, added == 0
                        ? getContext().getString(R.string.lobby_nothing_added)
                        : getResources().getQuantityString(R.plurals.lobby_added, added, added),
                Snackbar.LENGTH_SHORT).show();
    }

    private void confirmDelete(int position) {
        Question question = adapter.at(position);
        boolean[] deleted = {false};
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.question_delete_title)
                .setMessage(question.getName())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    deleted[0] = true;
                    questionDao.delete(question.getId());
                    lobby.remove(question.getId());
                    refresh();
                })
                .setOnDismissListener(dialog -> {
                    if (!deleted[0]) {
                        // The swiped row is put back when the question was kept.
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }
}
