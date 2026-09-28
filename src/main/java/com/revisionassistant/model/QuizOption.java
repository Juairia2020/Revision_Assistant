package com.revisionassistant.model;

/**
 * The four possible choices on a multiple-choice quiz question. Stored
 * in the database as its {@link #name()} (A / B / C / D).
 */
public enum QuizOption {
    A, B, C, D;

    /**
     * Parses a stored value back into a QuizOption, defaulting to A
     * if the text is missing or unrecognised.
     */
    public static QuizOption fromString(String value) {
        if (value == null) {
            return A;
        }
        try {
            return QuizOption.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return A;
        }
    }
}
