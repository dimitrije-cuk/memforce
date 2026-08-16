package com.memforce.model;

import androidx.annotation.NonNull;

/** Anything that can be shown in a picker or filter by name. */
public interface Named {

    long getId();

    @NonNull
    String getName();
}
