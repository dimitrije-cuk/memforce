package com.memforce.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Holds the run the game screen is showing.
 *
 * <p>A run is deliberately not stored: it belongs to the moment, not to the library, and a player
 * who leaves the game gives it up. The screen therefore asks for the current run and returns to the
 * lobby when the process was restarted and there is none.
 */
public final class CurrentGame {

    private static GameSession session;

    private CurrentGame() {
    }

    public static void start(@NonNull GameSession newSession) {
        session = newSession;
    }

    @Nullable
    public static GameSession get() {
        return session;
    }

    public static void end() {
        session = null;
    }
}
