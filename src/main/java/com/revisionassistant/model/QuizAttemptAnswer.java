package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a single answered question within a
 * {@link QuizAttempt}. Contains no database or UI logic - only fields,
 * constructors, getters/setters, equals/hashCode and toString.
 * <p>
 * Only the question id is stored (not a copy of its text) so that the
 * question stays the single source of truth; the service layer looks
 * the question back up when it needs to display or aggregate details.
 */
public class QuizAttemptAnswer {

    private int id;
    private int attemptId;
    private int questionId;
    private QuizOption selectedOption;
    private boolean correct;

    public QuizAttemptAnswer() {
    }

    public QuizAttemptAnswer(int attemptId, int questionId, QuizOption selectedOption, boolean correct) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    public QuizAttemptAnswer(int id, int attemptId, int questionId, QuizOption selectedOption, boolean correct) {
        this(attemptId, questionId, selectedOption, correct);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public QuizOption getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(QuizOption selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuizAttemptAnswer)) {
            return false;
        }
        QuizAttemptAnswer answer = (QuizAttemptAnswer) other;
        return id == answer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QuizAttemptAnswer{id=" + id + ", attemptId=" + attemptId + ", questionId=" + questionId
                + ", selectedOption=" + selectedOption + ", correct=" + correct + "}";
    }
}
