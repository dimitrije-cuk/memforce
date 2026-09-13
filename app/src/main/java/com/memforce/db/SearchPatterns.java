package com.memforce.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Turns user input into a LIKE pattern. */
public final class SearchPatterns {

    private SearchPatterns() {
    }

    /**
     * Wraps the input in {@code %} so that what a user types is looked for anywhere in the value,
     * which is what an ordinary search means and what spares the user from typing the wildcards
     * themselves. A {@code %} the user typed at either end is not doubled: the ends are stripped
     * first and the one pair is put back, so {@code %history}, {@code history%} and
     * {@code %history%} all ask the same question as {@code history}.
     *
     * <p>Nothing else is altered. A {@code %} inside the input keeps its place, and {@code _} is
     * never inserted, so it still matches exactly one character wherever the user typed it. Blank
     * input matches everything.
     */
    @NonNull
    public static String like(@Nullable String input) {
        if (input == null) {
            return "%";
        }
        String core = trimWildcards(input.trim());
        return core.isEmpty() ? "%" : '%' + core + '%';
    }

    /** Drops the leading and trailing {@code %}, however many of them the user typed. */
    @NonNull
    private static String trimWildcards(@NonNull String trimmed) {
        int start = 0;
        int end = trimmed.length();
        while (start < end && trimmed.charAt(start) == '%') {
            start++;
        }
        while (end > start && trimmed.charAt(end - 1) == '%') {
            end--;
        }
        return trimmed.substring(start, end);
    }
}
