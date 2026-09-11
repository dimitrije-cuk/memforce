package com.memforce.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The criteria one search is made of: a text pattern and the tags that narrow the result.
 *
 * <p>The value is immutable, so a screen holds the current criteria in one field and replaces it
 * whenever the user changes something. Tags keep the order in which they were chosen, which is the
 * order the chips are shown in.
 */
public final class SearchQuery {

    private static final SearchQuery EMPTY = new SearchQuery("", Collections.emptyList());

    private final String text;
    private final List<Long> tagIds;

    private SearchQuery(@NonNull String text, @NonNull List<Long> tagIds) {
        this.text = text;
        this.tagIds = Collections.unmodifiableList(tagIds);
    }

    @NonNull
    public static SearchQuery empty() {
        return EMPTY;
    }

    @NonNull
    public String getText() {
        return text;
    }

    /** The chosen tags, in the order they were chosen. */
    @NonNull
    public List<Long> getTagIds() {
        return tagIds;
    }

    /** True when no criterion is given, which is the request to see every question. */
    public boolean isEmpty() {
        return text.trim().isEmpty() && tagIds.isEmpty();
    }

    public boolean hasTag(long tagId) {
        return tagIds.contains(tagId);
    }

    @NonNull
    public SearchQuery withText(@Nullable String newText) {
        String value = newText == null ? "" : newText;
        return value.equals(text) ? this : new SearchQuery(value, new ArrayList<>(tagIds));
    }

    /** Adds the tag as a further criterion; a tag already chosen is not added a second time. */
    @NonNull
    public SearchQuery withTag(long tagId) {
        if (hasTag(tagId)) {
            return this;
        }
        List<Long> ids = new ArrayList<>(tagIds);
        ids.add(tagId);
        return new SearchQuery(text, ids);
    }

    @NonNull
    public SearchQuery withoutTag(long tagId) {
        if (!hasTag(tagId)) {
            return this;
        }
        List<Long> ids = new ArrayList<>(tagIds);
        ids.remove(Long.valueOf(tagId));
        return new SearchQuery(text, ids);
    }

    @NonNull
    public SearchQuery toggleTag(long tagId) {
        return hasTag(tagId) ? withoutTag(tagId) : withTag(tagId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchQuery)) {
            return false;
        }
        SearchQuery that = (SearchQuery) other;
        return text.equals(that.text) && tagIds.equals(that.tagIds);
    }

    @Override
    public int hashCode() {
        return 31 * text.hashCode() + tagIds.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "SearchQuery{text='" + text + "', tagIds=" + tagIds + '}';
    }
}
