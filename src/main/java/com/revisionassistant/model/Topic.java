package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a topic that belongs to a subject. Contains no
 * database or UI logic - only fields, constructors, getters/setters,
 * equals/hashCode and toString.
 */
public class Topic {

    private int id;
    private int subjectId;
    private String name;
    private boolean completed;

    public Topic() {
    }

    public Topic(int subjectId, String name, boolean completed) {
        this.subjectId = subjectId;
        this.name = name;
        this.completed = completed;
    }

    public Topic(int id, int subjectId, String name, boolean completed) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
        this.completed = completed;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Topic)) {
            return false;
        }
        Topic topic = (Topic) other;
        return id == topic.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Topic{id=" + id + ", subjectId=" + subjectId
                + ", name='" + name + "', completed=" + completed + "}";
    }
}
