package com.memforce.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Turns user input into a LIKE pattern. */
public final class SearchPatterns {

    private SearchPatterns() {
    }

    /**
     * Passes the input through unchanged so that {@code %} and {@code _} keep their SQL meaning,
     * which is what the search requirements ask for. Blank input matches everything.
     */
    @NonNull
    public static String like(@Nullable String input) {
        if (input == null) {
            return "%";
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? "%" : trimmed;
    }
}
