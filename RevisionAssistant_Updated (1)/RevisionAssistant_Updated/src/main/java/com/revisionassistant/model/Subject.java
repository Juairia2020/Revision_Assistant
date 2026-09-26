package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a subject. Contains no database or UI logic -
 * only fields, constructors, getters/setters, equals/hashCode and toString.
 */
public class Subject {

    private int id;
    private String name;
    private String color;

    public Subject() {
    }

    public Subject(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Subject(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Subject)) {
            return false;
        }
        Subject subject = (Subject) other;
        return id == subject.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Subject{id=" + id + ", name='" + name + "', color='" + color + "'}";
    }
}
