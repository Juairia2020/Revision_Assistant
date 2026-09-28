package com.revisionassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImportedFlashcardDTO {
    private String question;
    private String answer;

    @JsonIgnore
    private boolean selected = true;

    public ImportedFlashcardDTO() {}

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
