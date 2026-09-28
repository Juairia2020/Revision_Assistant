package com.revisionassistant.dto;

import java.util.ArrayList;
import java.util.List;

public class QuizImportDTO {
    private String topic;
    private List<ImportedQuizQuestionDTO> questions = new ArrayList<>();

    public QuizImportDTO() {}

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public List<ImportedQuizQuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<ImportedQuizQuestionDTO> questions) { this.questions = questions; }
}
