package com.memforce.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.memforce.model.User;

/** Remembers which user is signed in; decks are scoped to this user. */
public class Session {

    private static final String PREFS_NAME = "memforce_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final long NO_USER = -1L;

    private final SharedPreferences prefs;

    public Session(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void signIn(@NonNull User user) {
        prefs.edit()
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getName())
                .apply();
    }

    public void signOut() {
        prefs.edit().clear().apply();
    }

    public boolean isSignedIn() {
        return prefs.getLong(KEY_USER_ID, NO_USER) != NO_USER;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, NO_USER);
    }

    @Nullable
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
}
