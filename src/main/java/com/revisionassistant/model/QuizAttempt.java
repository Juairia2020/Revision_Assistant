package com.revisionassistant.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Plain data holder for one completed quiz attempt (a stored result).
 * Contains no database or UI logic - only fields, constructors,
 * getters/setters, equals/hashCode and toString.
 */
public class QuizAttempt {

    private int id;
    private int subjectId;
    private Integer topicId;
    private LocalDate attemptDate;
    private int totalQuestions;
    private int correctAnswers;
    private int scorePercent;

    public QuizAttempt() {
    }

    public QuizAttempt(int subjectId, Integer topicId, LocalDate attemptDate,
                        int totalQuestions, int correctAnswers, int scorePercent) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.attemptDate = attemptDate;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.scorePercent = scorePercent;
    }

    public QuizAttempt(int id, int subjectId, Integer topicId, LocalDate attemptDate,
                        int totalQuestions, int correctAnswers, int scorePercent) {
        this(subjectId, topicId, attemptDate, totalQuestions, correctAnswers, scorePercent);
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

    public LocalDate getAttemptDate() {
        return attemptDate;
    }

    public void setAttemptDate(LocalDate attemptDate) {
        this.attemptDate = attemptDate;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(int scorePercent) {
        this.scorePercent = scorePercent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuizAttempt)) {
            return false;
        }
        QuizAttempt attempt = (QuizAttempt) other;
        return id == attempt.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QuizAttempt{id=" + id + ", subjectId=" + subjectId + ", topicId=" + topicId
                + ", attemptDate=" + attemptDate + ", score=" + correctAnswers + "/" + totalQuestions + "}";
    }
}
