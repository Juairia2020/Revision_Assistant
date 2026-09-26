package com.revisionassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class ImportedQuizQuestionDTO {
    private String question;
    private List<String> options = new ArrayList<>();
    private String correctAnswer;

    @JsonIgnore
    private boolean selected = true;

    public ImportedQuizQuestionDTO() {}

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getOptionA() { return option(0); }
    public String getOptionB() { return option(1); }
    public String getOptionC() { return option(2); }
    public String getOptionD() { return option(3); }

    private String option(int index) {
        return options != null && options.size() > index ? options.get(index) : "";
    }

    public String getCorrectOptionLetter() {
        if (options == null || correctAnswer == null) return null;
        String value = correctAnswer.trim();
        if (value.matches("(?i)[ABCD]")) return value.toUpperCase();
        for (int i = 0; i < options.size(); i++) {
            if (value.equals(options.get(i).trim())) {
                return String.valueOf((char) ('A' + i));
            }
        }
        return null;
    }
}
