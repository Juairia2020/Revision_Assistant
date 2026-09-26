package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a saved study resource - a link the user wants
 * to come back to later, organized by an optional subject and a
 * category. Contains no database or UI logic - only fields,
 * constructors, getters/setters, equals/hashCode and toString.
 */
public class Resource {

    private int id;
    private Integer subjectId;
    private String title;
    private String url;
    private String description;
    private String category;

    public Resource() {
    }

    public Resource(Integer subjectId, String title, String url, String description, String category) {
        this.subjectId = subjectId;
        this.title = title;
        this.url = url;
        this.description = description;
        this.category = category;
    }

    public Resource(int id, Integer subjectId, String title, String url, String description, String category) {
        this(subjectId, title, url, description, category);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) other;
        return id == resource.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Resource{id=" + id + ", subjectId=" + subjectId + ", title='" + title
                + "', category='" + category + "'}";
    }
}
