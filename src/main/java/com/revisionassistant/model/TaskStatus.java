package com.revisionassistant.model;

/**
 * Progress status of a study task. Stored in the database as its
 * {@link #name()} (NOT_STARTED / IN_PROGRESS / COMPLETED) and
 * displayed using {@link #getLabel()}.
 * <p>
 * A task's {@code completed} flag is always kept in sync with this
 * status: a task is only ever "completed" when its status is
 * {@link #COMPLETED}.
 */
public enum TaskStatus {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    private final String label;

    TaskStatus(String label) {
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
     * Parses a stored value back into a TaskStatus, defaulting to
     * NOT_STARTED if the text is missing or unrecognised.
     */
    public static TaskStatus fromString(String value) {
        if (value == null) {
            return NOT_STARTED;
        }
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STARTED;
        }
    }
}
