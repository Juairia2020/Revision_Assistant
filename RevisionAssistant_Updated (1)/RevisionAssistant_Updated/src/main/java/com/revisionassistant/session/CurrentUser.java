package com.revisionassistant.session;

import com.revisionassistant.model.User;

/**
 * In-memory application session for the currently signed-in user. This
 * class itself is never persisted - restoring a session across application
 * launches is handled separately by {@code UserService#tryAutoLogin()} and
 * {@code com.revisionassistant.security.RememberMeStore}, which populate
 * this class the same way an interactive login does.
 */
public final class CurrentUser {

    private static User user;

    private CurrentUser() {
    }

    public static void set(User authenticatedUser) {
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("Authenticated user cannot be null.");
        }
        // The session only needs identity details; do not retain the password hash.
        user = new User(
                authenticatedUser.getId(),
                authenticatedUser.getName(),
                authenticatedUser.getEmail(),
                null,
                authenticatedUser.isOnboardingCompleted()
        );
    }

    public static User get() {
        return user;
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    public static void clear() {
        user = null;
    }
}
