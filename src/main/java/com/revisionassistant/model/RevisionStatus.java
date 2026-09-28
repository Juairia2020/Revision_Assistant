package com.revisionassistant.model;

/**
 * How far along a flashcard is in revision. Stored in the database as
 * its {@link #name()} (NOT_STARTED / LEARNING / REVISED) and displayed
 * using {@link #getLabel()}.
 */
public enum RevisionStatus {
    NOT_STARTED("Not started"),
    LEARNING("Learning"),
    REVISED("Revised");

    private final String label;

    RevisionStatus(String label) {
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
     * Parses a stored value back into a RevisionStatus, defaulting to
     * NOT_STARTED if the text is missing or unrecognised.
     */
    public static RevisionStatus fromString(String value) {
        if (value == null) {
            return NOT_STARTED;
        }
        try {
            return RevisionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STARTED;
        }
    }
}
