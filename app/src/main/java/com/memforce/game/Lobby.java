package com.memforce.game;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The questions gathered for the next game.
 *
 * <p>The lobby is the one destination every selection path leads to: the search on the main menu,
 * a swipe on a tag, and a swipe on a question all put questions here, and the game reads them back
 * from here. It holds identifiers rather than questions so that editing a question between the
 * selection and the game shows the edited text, and it is kept in the same preference storage the
 * session uses so that leaving the application on the way to the game does not empty it.
 *
 * <p>A question is present at most once however often it is added.
 */
public class Lobby {

    private static final String PREFS_NAME = "memforce_lobby";
    private static final String KEY_QUESTION_IDS = "question_ids";

    private final SharedPreferences prefs;

    public Lobby(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Puts the questions into the lobby.
     *
     * @return how many of them were not in the lobby already
     */
    public int add(@NonNull Collection<Long> questionIds) {
        Set<String> stored = read();
        int added = 0;
        for (Long id : questionIds) {
            if (id != null && stored.add(String.valueOf(id))) {
                added++;
            }
        }
        if (added > 0) {
            write(stored);
        }
        return added;
    }

    /** @return true when the question was not in the lobby already */
    public boolean add(long questionId) {
        return add(Collections.singletonList(questionId)) == 1;
    }

    public void remove(long questionId) {
        Set<String> stored = read();
        if (stored.remove(String.valueOf(questionId))) {
            write(stored);
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_QUESTION_IDS).apply();
    }

    public boolean contains(long questionId) {
        return read().contains(String.valueOf(questionId));
    }

    public int size() {
        return read().size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** The identifiers in the lobby; the order questions are shown in is decided by the screen. */
    @NonNull
    public List<Long> questionIds() {
        List<Long> ids = new ArrayList<>();
        for (String id : read()) {
            try {
                ids.add(Long.parseLong(id));
            } catch (NumberFormatException ignored) {
                // A value this application did not write is not a question and is skipped.
            }
        }
        return ids;
    }

    /** Drops identifiers that name no stored question, which is how deleted questions leave. */
    public void retainAll(@NonNull Collection<Long> existingIds) {
        Set<String> keep = new HashSet<>();
        for (Long id : existingIds) {
            keep.add(String.valueOf(id));
        }
        Set<String> stored = read();
        if (stored.retainAll(keep)) {
            write(stored);
        }
    }

    /** A copy, because a set handed out by the preferences must never be changed in place. */
    @NonNull
    private Set<String> read() {
        return new LinkedHashSet<>(prefs.getStringSet(KEY_QUESTION_IDS, Collections.emptySet()));
    }

    private void write(@NonNull Set<String> ids) {
        prefs.edit().putStringSet(KEY_QUESTION_IDS, new HashSet<>(ids)).apply();
    }
}
