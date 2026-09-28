package com.revisionassistant.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Reads and writes the small local file that lets the desktop app skip the
 * login screen on its next launch, and provides the selector/validator
 * helpers used to keep that file safe to leave on disk.
 * <p>
 * A classic "selector + validator" pattern is used, the same one common
 * web frameworks use for persistent login cookies:
 * <ul>
 *     <li>The <b>selector</b> is a random lookup key. It is stored in
 *     plain text both in the local file and in the database, so a stored
 *     token can be found quickly - but a selector alone proves nothing.</li>
 *     <li>The <b>validator</b> is a second, independent random value. Its
 *     plain form is written only to the local file (never to the
 *     database); only a one-way hash of it is stored in the database.
 *     Reading the database therefore never reveals a value that would let
 *     someone sign in.</li>
 * </ul>
 * The local file this class writes ({@value #FILE_NAME}, in the
 * application's working directory next to the SQLite database) is
 * excluded from version control the same way {@code revision_assistant.db}
 * is - see {@code .gitignore}.
 */
public final class RememberMeStore {

    private static final String FILE_NAME = "remember_me.token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private RememberMeStore() {
    }

    /** A new, random selector - safe to store in plain text. */
    public static String newSelector() {
        return randomToken();
    }

    /** A new, random validator - the plain value is only ever written to the local file, never to the database. */
    public static String newValidator() {
        return randomToken();
    }

    /** One-way hash of a validator, safe to store in the database. */
    public static String hashValidator(String validator) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(validator.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    /** Constant-time comparison between a freshly-hashed validator and the one stored in the database. */
    public static boolean matches(String validator, String storedHash) {
        if (validator == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hashValidator(validator).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /** Writes "selector:validator" to the local token file, replacing any existing one. */
    public static void save(String selector, String validator) {
        try {
            Files.writeString(Path.of(FILE_NAME), selector + ":" + validator, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Persisting the session is a convenience, not a requirement - if the
            // file cannot be written, the user simply logs in again next time.
        }
    }

    /** The selector/validator pair from the local token file, or {@code null} if there isn't one (or it's unreadable). */
    public static StoredToken read() {
        Path path = Path.of(FILE_NAME);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8).trim();
            int separator = content.indexOf(':');
            if (separator <= 0 || separator == content.length() - 1) {
                return null;
            }
            return new StoredToken(content.substring(0, separator), content.substring(separator + 1));
        } catch (IOException e) {
            return null;
        }
    }

    /** Deletes the local token file, if present. Safe to call even when no session is remembered. */
    public static void clear() {
        try {
            Files.deleteIfExists(Path.of(FILE_NAME));
        } catch (IOException e) {
            // Nothing more useful to do - the file simply stays until the next successful clear.
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The selector/validator pair read back from the local token file. */
    public static final class StoredToken {
        private final String selector;
        private final String validator;

        public StoredToken(String selector, String validator) {
            this.selector = selector;
            this.validator = validator;
        }

        public String getSelector() {
            return selector;
        }

        public String getValidator() {
            return validator;
        }
    }
}
