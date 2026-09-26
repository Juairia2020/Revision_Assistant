package com.revisionassistant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Plain data holder for a recorded study session. Contains no
 * database or UI logic - only fields, constructors, getters/setters,
 * equals/hashCode and toString.
 */
public class StudySession {

    private int id;
    private int subjectId;
    private Integer topicId;
    private LocalDate date;
    private int durationMinutes;
    private String notes;

    public StudySession() {
    }

    public StudySession(int subjectId, Integer topicId, LocalDate date,
                         int durationMinutes, String notes) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    public StudySession(int id, int subjectId, Integer topicId, LocalDate date,
                         int durationMinutes, String notes) {
        this(subjectId, topicId, date, durationMinutes, notes);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudySession)) {
            return false;
        }
        StudySession session = (StudySession) other;
        return id == session.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StudySession{id=" + id + ", subjectId=" + subjectId + ", topicId=" + topicId
                + ", date=" + date + ", durationMinutes=" + durationMinutes + "}";
    }
}
