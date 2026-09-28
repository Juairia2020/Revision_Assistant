package com.revisionassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One multiple-choice question suggested by the AI, before the user
 * has reviewed and saved it. Shaped to match the JSON the model is
 * asked to return - Jackson deserializes straight into this class, no
 * manual string parsing.
 * <p>
 * This is a preview-only object. It never touches the database itself;
 * once the user confirms which questions to keep, the controller
 * passes their fields into the existing {@code QuizService}, exactly
 * as if they had been typed in by hand.
 * <p>
 * By the time {@code ApiService} hands a list of these back to a
 * controller, {@link #correctOption} is guaranteed to be exactly one
 * of {@code "A"}, {@code "B"}, {@code "C"} or {@code "D"} - any entry
 * that failed that check was already dropped during validation.
 * <p>
 * {@link #selected} is not part of the API response - it only exists
 * so the preview table in {@code QuizController} can track which
 * generated questions the user wants to keep.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedQuestionDTO {

    @JsonProperty("question")
    private String question;

    @JsonProperty("optionA")
    private String optionA;

    @JsonProperty("optionB")
    private String optionB;

    @JsonProperty("optionC")
    private String optionC;

    @JsonProperty("optionD")
    private String optionD;

    @JsonProperty("correctOption")
    private String correctOption;

    @JsonIgnore
    private boolean selected = true;

    public GeneratedQuestionDTO() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
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

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
