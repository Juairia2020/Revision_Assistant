package com.revisionassistant.dto;

import java.util.ArrayList;
import java.util.List;

public class FlashcardImportDTO {
    private String topic;
    private List<ImportedFlashcardDTO> flashcards = new ArrayList<>();

    public FlashcardImportDTO() {}

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public List<ImportedFlashcardDTO> getFlashcards() { return flashcards; }
    public void setFlashcards(List<ImportedFlashcardDTO> flashcards) { this.flashcards = flashcards; }
}
