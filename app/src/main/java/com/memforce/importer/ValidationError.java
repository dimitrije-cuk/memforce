package com.memforce.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One rule of {@code docs/schemas/question-set.schema.json} that a question set file breaks.
 * Carries no user facing text so that the wording stays in the string resources.
 */
public final class ValidationError {

    public enum Reason {
        /** The file is not JSON at all. */
        MALFORMED_JSON,
        /** The file parses, but its top level value is not a JSON object. */
        NOT_AN_OBJECT,
        /** A required field is absent. */
        MISSING_FIELD,
        /** A field that this format does not define is present. */
        UNKNOWN_FIELD,
        /** A field has a type the format does not allow. */
        WRONG_TYPE,
        /** A string is empty, blank, or padded with whitespace where that is not allowed. */
        BLANK,
        /** A tag runs over more than one line, which its schema pattern does not allow. */
        NOT_ONE_LINE,
        /** A string is longer than the format allows. */
        TOO_LONG,
        /** The same tag appears twice in one tag array. */
        DUPLICATE_TAG,
        /** The same answer appears twice in one {@code alternativeAnswers} array. */
        DUPLICATE_ANSWER,
        /** {@code alternativeAnswers} is given for a question that carries no answer to vary. */
        ALTERNATIVES_WITHOUT_ANSWER,
        /** {@code formatVersion} names a version this app cannot read. */
        UNSUPPORTED_VERSION,
        /** {@code questions} is present but empty. */
        NO_QUESTIONS
    }

    private final Reason reason;
    private final String location;
    private final String detail;
    private final int limit;

    private ValidationError(Reason reason, String location, @Nullable String detail, int limit) {
        this.reason = reason;
        this.location = location;
        this.detail = detail;
        this.limit = limit;
    }

    /** @param location the offending field, e.g. {@code questions[3].answer}, or "" for the file itself */
    @NonNull
    static ValidationError at(@NonNull Reason reason, @NonNull String location) {
        return new ValidationError(reason, location, null, 0);
    }

    @NonNull
    static ValidationError detailed(@NonNull Reason reason,
                                    @NonNull String location,
                                    @Nullable String detail) {
        return new ValidationError(reason, location, detail, 0);
    }

    @NonNull
    static ValidationError tooLong(@NonNull String location, int limit) {
        return new ValidationError(Reason.TOO_LONG, location, null, limit);
    }

    @NonNull
    public Reason getReason() {
        return reason;
    }

    @NonNull
    public String getLocation() {
        return location;
    }

    /** The offending value, when repeating it helps: the unreadable version, the duplicated tag or answer, ... */
    @Nullable
    public String getDetail() {
        return detail;
    }

    /** The exceeded limit for {@link Reason#TOO_LONG}, otherwise 0. */
    public int getLimit() {
        return limit;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(reason.name());
        if (!location.isEmpty()) {
            text.append(" at ").append(location);
        }
        if (detail != null) {
            text.append(" (").append(detail).append(')');
        }
        if (limit > 0) {
            text.append(" [max ").append(limit).append(']');
        }
        return text.toString();
    }
}
