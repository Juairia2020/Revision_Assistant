package com.revisionassistant.model;

/**
 * Priority level of a revision task. Stored in the database as its
 * {@link #name()} (LOW / MEDIUM / HIGH) and displayed using
 * {@link #getLabel()}.
 */
public enum Priority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Parses a stored value back into a Priority, defaulting to MEDIUM
     * if the text is missing or unrecognised.
     */
    public static Priority fromString(String value) {
        if (value == null) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
