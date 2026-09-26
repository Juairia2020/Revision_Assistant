package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a flashcard. Contains no database or UI logic -
 * only fields, constructors, getters/setters, equals/hashCode and
 * toString.
 * <p>
 * A flashcard always belongs to a subject. The topic is optional (a
 * card can be about a subject in general, not one specific topic),
 * matching how {@link Task} and {@link StudySession} handle topics.
 */
public class Flashcard {

    private int id;
    private int subjectId;
    private Integer topicId;
    private String front;
    private String back;
    private boolean difficult;
    private RevisionStatus revisionStatus;

    public Flashcard() {
    }

    public Flashcard(int subjectId, Integer topicId, String front, String back,
                      boolean difficult, RevisionStatus revisionStatus) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.front = front;
        this.back = back;
        this.difficult = difficult;
        this.revisionStatus = revisionStatus;
    }

    public Flashcard(int id, int subjectId, Integer topicId, String front, String back,
                      boolean difficult, RevisionStatus revisionStatus) {
        this(subjectId, topicId, front, back, difficult, revisionStatus);
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

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public boolean isDifficult() {
        return difficult;
    }

    public void setDifficult(boolean difficult) {
        this.difficult = difficult;
    }

    public RevisionStatus getRevisionStatus() {
        return revisionStatus;
    }

    public void setRevisionStatus(RevisionStatus revisionStatus) {
        this.revisionStatus = revisionStatus;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Flashcard)) {
            return false;
        }
        Flashcard flashcard = (Flashcard) other;
        return id == flashcard.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Flashcard{id=" + id + ", subjectId=" + subjectId + ", topicId=" + topicId
                + ", difficult=" + difficult + ", revisionStatus=" + revisionStatus + "}";
    }
}
