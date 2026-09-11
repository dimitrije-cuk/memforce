package com.memforce.model;

/** The orders the tag list can be shown in. */
public enum TagSort {

    /** Most questions first; tags with the same count are ordered by name. */
    MOST_USED,

    /** By name, ascending, without regard to letter case. */
    ALPHABETICAL,

    /** Fewest questions first; tags with the same count are ordered by name. */
    LEAST_USED
}
