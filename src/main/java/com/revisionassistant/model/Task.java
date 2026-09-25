package com.revisionassistant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Plain data holder for a revision task. Contains no database or UI
 * logic - only fields, constructors, getters/setters, equals/hashCode
 * and toString.
 * <p>
 * A task always belongs to a subject. The topic is optional (a task
 * can be about a subject in general, not one specific topic), as is
 * the deadline.
 */
public class Task {

    private int id;
    private int subjectId;
    private Integer topicId;
    private String title;
    private int estimatedMinutes;
    private Priority priority;
    private LocalDate deadline;
    private boolean completed;

    public Task() {
    }

    public Task(int subjectId, Integer topicId, String title, int estimatedMinutes,
                Priority priority, LocalDate deadline, boolean completed) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.title = title;
        this.estimatedMinutes = estimatedMinutes;
        this.priority = priority;
        this.deadline = deadline;
        this.completed = completed;
    }

    public Task(int id, int subjectId, Integer topicId, String title, int estimatedMinutes,
                Priority priority, LocalDate deadline, boolean completed) {
        this(subjectId, topicId, title, estimatedMinutes, priority, deadline, completed);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /** True when the task has a deadline that has passed and is not yet done. */
    public boolean isOverdue() {
        return !completed && deadline != null && deadline.isBefore(LocalDate.now());
    }

    /** True when the task's deadline is today. */
    public boolean isDueToday() {
        return deadline != null && deadline.isEqual(LocalDate.now());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Task)) {
            return false;
        }
        Task task = (Task) other;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", subjectId=" + subjectId + ", topicId=" + topicId
                + ", title='" + title + "', priority=" + priority
                + ", deadline=" + deadline + ", completed=" + completed + "}";
    }
}
