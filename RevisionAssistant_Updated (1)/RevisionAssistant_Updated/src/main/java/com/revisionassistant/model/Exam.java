package com.revisionassistant.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Plain data holder for an exam. Contains no database or UI logic -
 * only fields, constructors, getters/setters, equals/hashCode and
 * toString. Days remaining is a derived value, computed on demand
 * rather than stored.
 */
public class Exam {

    private int id;
    private int subjectId;
    private String title;
    private LocalDate examDate;
    private int progress; // 0-100, how prepared the user feels

    public Exam() {
    }

    public Exam(int subjectId, String title, LocalDate examDate, int progress) {
        this.subjectId = subjectId;
        this.title = title;
        this.examDate = examDate;
        this.progress = progress;
    }

    public Exam(int id, int subjectId, String title, LocalDate examDate, int progress) {
        this(subjectId, title, examDate, progress);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    /**
     * Days between today and the exam date. Negative once the exam
     * date has passed.
     */
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), examDate);
    }

    public boolean isPast() {
        return examDate != null && examDate.isBefore(LocalDate.now());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Exam)) {
            return false;
        }
        Exam exam = (Exam) other;
        return id == exam.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Exam{id=" + id + ", subjectId=" + subjectId + ", title='" + title
                + "', examDate=" + examDate + ", progress=" + progress + "}";
    }
}
