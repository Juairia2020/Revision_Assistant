package com.revisionassistant.model;

import java.util.Objects;

/**
 * Plain data holder for a stored multiple-choice question. Contains no
 * database or UI logic - only fields, constructors, getters/setters,
 * equals/hashCode and toString.
 */
public class QuizQuestion {

    private int id;
    private int subjectId;
    private Integer topicId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private QuizOption correctOption;

    public QuizQuestion() {
    }

    public QuizQuestion(int subjectId, Integer topicId, String questionText,
                         String optionA, String optionB, String optionC, String optionD,
                         QuizOption correctOption) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
    }

    public QuizQuestion(int id, int subjectId, Integer topicId, String questionText,
                         String optionA, String optionB, String optionC, String optionD,
                         QuizOption correctOption) {
        this(subjectId, topicId, questionText, optionA, optionB, optionC, optionD, correctOption);
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public QuizOption getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(QuizOption correctOption) {
        this.correctOption = correctOption;
    }

    /** Returns the text of the given option (A/B/C/D) for this question. */
    public String getOptionText(QuizOption option) {
        switch (option) {
            case A:
                return optionA;
            case B:
                return optionB;
            case C:
                return optionC;
            case D:
                return optionD;
            default:
                return "";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuizQuestion)) {
            return false;
        }
        QuizQuestion question = (QuizQuestion) other;
        return id == question.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QuizQuestion{id=" + id + ", subjectId=" + subjectId + ", topicId=" + topicId
                + ", correctOption=" + correctOption + "}";
    }
}
