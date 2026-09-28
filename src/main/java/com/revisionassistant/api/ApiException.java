package com.revisionassistant.api;

/**
 * A single, user-safe failure type for anything that can go wrong
 * while talking to the AI API: missing credentials, network failures,
 * timeouts, non-2xx HTTP status codes, and responses that are empty,
 * malformed, or fail validation.
 * <p>
 * The message on every instance is written to be shown to the user
 * directly (for example in an {@code Alert} or a status label) - it
 * never contains a raw stack trace or exception class name.
 */
public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
