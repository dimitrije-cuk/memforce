package com.memforce.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Locale;

/**
 * Decides whether a submitted answer counts as correct.
 *
 * <p>Comparison ignores the differences a keyboard produces rather than the ones knowledge
 * produces: leading and trailing spaces, runs of spaces inside the text, and letter case. An empty
 * submission is the skip described by the game rules and is never correct.
 *
 * <p>A question may accept more than one wording — "4" beside "four", "CO2" beside "carbon
 * dioxide" — and every one of them is worth the same: a submission matching any accepted answer is
 * correct.
 */
public final class AnswerMatcher {

    private AnswerMatcher() {
    }

    /** True when the submission matches any of the answers the question accepts. */
    public static boolean matchesAny(@NonNull Collection<String> accepted,
                                     @Nullable String submitted) {
        String given = normalize(submitted);
        if (given.isEmpty()) {
            return false;
        }
        for (String candidate : accepted) {
            if (given.equals(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    /** True when a submission carries anything other than spacing. */
    public static boolean isAnswered(@Nullable String submitted) {
        return !normalize(submitted).isEmpty();
    }

    @NonNull
    public static String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
