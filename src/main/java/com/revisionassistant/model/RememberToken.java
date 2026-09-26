package com.revisionassistant.model;

import java.time.Instant;

/**
 * A persisted "remember me" login token record. Contains no database or
 * UI logic - only fields, constructor and getters, matching the style of
 * the other plain model classes in this package.
 * <p>
 * Only a hash of the validator is ever stored (in the database and in
 * {@link #validatorHash}); the plain validator itself lives only in the
 * local token file on disk, never in the database. See
 * {@code com.revisionassistant.security.RememberMeStore} for how the
 * selector/validator pair is generated, stored and checked.
 */
public class RememberToken {

    private final int userId;
    private final String selector;
    private final String validatorHash;
    private final Instant expiresAt;

    public RememberToken(int userId, String selector, String validatorHash, Instant expiresAt) {
        this.userId = userId;
        this.selector = selector;
        this.validatorHash = validatorHash;
        this.expiresAt = expiresAt;
    }

    public int getUserId() {
        return userId;
    }

    public String getSelector() {
        return selector;
    }

    public String getValidatorHash() {
        return validatorHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }
}
