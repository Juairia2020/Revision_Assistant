package com.revisionassistant.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisionassistant.dto.FlashcardImportDTO;
import com.revisionassistant.dto.ImportedFlashcardDTO;
import com.revisionassistant.dto.ImportedQuizQuestionDTO;
import com.revisionassistant.dto.QuizImportDTO;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonImportService {
    private final ObjectMapper objectMapper;

    public JsonImportService() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public FlashcardImportDTO readFlashcards(File file) throws JsonImportException {
        try {
            FlashcardImportDTO document = objectMapper.readValue(file, FlashcardImportDTO.class);
            validateFlashcards(document);
            return document;
        } catch (IOException | RuntimeException e) {
            throw new JsonImportException(messageFor("flashcard", e), e);
        }
    }

    public QuizImportDTO readQuiz(File file) throws JsonImportException {
        try {
            QuizImportDTO document = objectMapper.readValue(file, QuizImportDTO.class);
            validateQuiz(document);
            return document;
        } catch (IOException | RuntimeException e) {
            throw new JsonImportException(messageFor("quiz", e), e);
        }
    }

    public static String flashcardPrompt() {
        return """
                Generate revision flashcards as JSON only. Use exactly this structure:
                {
                  "topic": "Topic name",
                  "flashcards": [
                    {"question": "Question", "answer": "Answer"}
                  ]
                }
                Requirements: topic and every question/answer are non-empty strings.
                Do not add markdown or commentary.
                """;
    }

    public static String quizPrompt() {
        return """
                Generate multiple-choice quiz questions as JSON only. Use exactly this structure:
                {
                  "topic": "Topic name",
                  "questions": [
                    {
                      "question": "Question",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswer": "Option B"
                    }
                  ]
                }
                Requirements: topic/question are non-empty, options contains exactly four
                non-empty distinct strings, and correctAnswer exactly matches one option.
                Do not add markdown or commentary.
                """;
    }

    private void validateFlashcards(FlashcardImportDTO document) throws JsonImportException {
        if (document == null) throw new JsonImportException("The JSON document is empty.");
        if (isBlank(document.getTopic())) throw new JsonImportException("Missing or empty topic.");
        List<ImportedFlashcardDTO> cards = document.getFlashcards();
        if (cards == null || cards.isEmpty()) throw new JsonImportException("The flashcards array is missing or empty.");

        for (int i = 0; i < cards.size(); i++) {
            ImportedFlashcardDTO card = cards.get(i);
            if (card == null || isBlank(card.getQuestion()) || isBlank(card.getAnswer())) {
                throw new JsonImportException("Flashcard " + (i + 1) + " must have non-empty question and answer.");
            }
            card.setSelected(true);
        }
    }

    private void validateQuiz(QuizImportDTO document) throws JsonImportException {
        if (document == null) throw new JsonImportException("The JSON document is empty.");
        if (isBlank(document.getTopic())) throw new JsonImportException("Missing or empty topic.");
        List<ImportedQuizQuestionDTO> questions = document.getQuestions();
        if (questions == null || questions.isEmpty()) throw new JsonImportException("The questions array is missing or empty.");

        for (int i = 0; i < questions.size(); i++) {
            ImportedQuizQuestionDTO q = questions.get(i);
            if (q == null || isBlank(q.getQuestion())) {
                throw new JsonImportException("Question " + (i + 1) + " must have non-empty question text.");
            }
            if (q.getOptions() == null || q.getOptions().size() != 4) {
                throw new JsonImportException("Question " + (i + 1) + " must contain exactly four options.");
            }
            Set<String> normalized = new HashSet<>();
            for (String option : q.getOptions()) {
                if (isBlank(option)) {
                    throw new JsonImportException("Question " + (i + 1) + " has an empty option.");
                }
                if (!normalized.add(option.trim().toLowerCase())) {
                    throw new JsonImportException("Question " + (i + 1) + " has duplicate options.");
                }
            }
            if (isBlank(q.getCorrectAnswer()) || q.getCorrectOptionLetter() == null) {
                throw new JsonImportException("Question " + (i + 1)
                        + " must have a correctAnswer matching one option (or A, B, C, or D).");
            }
            q.setSelected(true);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String messageFor(String type, Exception e) {
        String detail = e.getMessage();
        if (detail == null || detail.isBlank()) {
            return "Could not read the " + type + " JSON file.";
        }
        if (detail.contains("Unexpected character") || detail.contains("JsonParseException")) {
            return "The file is not valid JSON.";
        }
        return "Could not import " + type + " JSON: " + detail;
    }
}
