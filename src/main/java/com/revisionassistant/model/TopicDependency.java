package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for one prerequisite relationship: {@code
 * prerequisiteId} must be studied before {@code topicId}. Contains no
 * database or UI logic - only fields, constructors, getters/setters,
 * equals/hashCode and toString.
 * <p>
 * This is the edge type behind the topic dependency graph built by
 * {@code TopicDependencyService} - one row here becomes one directed
 * edge in the {@code algorithm.Graph} used for cycle detection,
 * topological sort and shortest paths.
 */
public class TopicDependency {

    private int id;
    private int topicId;
    private int prerequisiteId;

    public TopicDependency() {
    }

    public TopicDependency(int topicId, int prerequisiteId) {
        this.topicId = topicId;
        this.prerequisiteId = prerequisiteId;
    }

    public TopicDependency(int id, int topicId, int prerequisiteId) {
        this.id = id;
        this.topicId = topicId;
        this.prerequisiteId = prerequisiteId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public int getPrerequisiteId() {
        return prerequisiteId;
    }

    public void setPrerequisiteId(int prerequisiteId) {
        this.prerequisiteId = prerequisiteId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TopicDependency)) {
            return false;
        }
        TopicDependency dependency = (TopicDependency) other;
        return id == dependency.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TopicDependency{id=" + id + ", topicId=" + topicId
                + ", prerequisiteId=" + prerequisiteId + "}";
    }
}
