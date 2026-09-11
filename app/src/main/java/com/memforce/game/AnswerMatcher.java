package com.memforce.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Decides whether a submitted answer counts as correct.
 *
 * <p>Comparison ignores the differences a keyboard produces rather than the ones knowledge
 * produces: leading and trailing spaces, runs of spaces inside the text, and letter case. An empty
 * submission is the skip described by the game rules and is never correct.
 */
public final class AnswerMatcher {

    private AnswerMatcher() {
    }

    public static boolean matches(@Nullable String expected, @Nullable String submitted) {
        String wanted = normalize(expected);
        String given = normalize(submitted);
        return !wanted.isEmpty() && !given.isEmpty() && wanted.equals(given);
    }

    /** True when a submission carries anything other than spacing. */
    public static boolean isAnswered(@Nullable String submitted) {
        return !normalize(submitted).isEmpty();
    }

    /** True when a stored answer can be compared against, which a game requires. */
    public static boolean isAnswerable(@Nullable String storedAnswer) {
        return !normalize(storedAnswer).isEmpty();
    }

    @NonNull
    public static String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
