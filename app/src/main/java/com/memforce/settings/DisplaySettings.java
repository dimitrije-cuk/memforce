package com.memforce.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * What the user has asked to see, as distinct from what the application has stored.
 *
 * <p>A display choice changes nothing a question carries, so it belongs beside the database rather
 * than in it, in its own preference store. It is kept apart from the session because it describes
 * the device rather than the account: signing out clears an identity, and must not also undo a
 * choice the user made about how the lists read.
 *
 * <p>One store serves every screen, which is what makes a choice made in the home screen's menu
 * hold on the question list as well.
 */
public class DisplaySettings {

    private static final String PREFS_NAME = "memforce_settings";
    private static final String KEY_QUESTION_TAGS_SHOWN = "question_tags_shown";

    private final SharedPreferences prefs;

    public DisplaySettings(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Whether a question in a result list shows the tags it carries.
     *
     * <p>Shown until the user says otherwise, because the tags are what tells one similarly worded
     * question from another; a setting that hid them by default would hide them from every user
     * who never opens the menu.
     */
    public boolean areQuestionTagsShown() {
        return prefs.getBoolean(KEY_QUESTION_TAGS_SHOWN, true);
    }

    public void setQuestionTagsShown(boolean shown) {
        prefs.edit().putBoolean(KEY_QUESTION_TAGS_SHOWN, shown).apply();
    }
}
