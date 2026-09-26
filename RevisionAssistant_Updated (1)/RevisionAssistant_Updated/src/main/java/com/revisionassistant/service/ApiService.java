package com.revisionassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisionassistant.api.ApiClient;
import com.revisionassistant.api.ApiConfig;
import com.revisionassistant.api.ApiException;
import com.revisionassistant.dto.GeneratedFlashcardDTO;
import com.revisionassistant.dto.GeneratedQuestionDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a topic into AI-generated flashcards or quiz questions.
 * <p>
 * Sits between the controllers and {@link ApiClient} in the API
 * workflow: it builds the prompt, asks {@code ApiClient} for the raw
 * reply text, deserializes that text into DTOs with Jackson, and
 * validates every entry before handing the list back. Nothing coming
 * out of this class is ever saved automatically - controllers always
 * show the returned list to the user for confirmation first, then
 * save each accepted entry through the existing
 * {@link FlashcardService} / {@link QuizService}, exactly as if it
 * had been typed in by hand.
 * <p>
 * Every failure - missing key, network problem, malformed JSON, a
 * reply with no usable entries - surfaces as a single
 * {@link ApiException} with a message that is safe to show to the
 * user. Nothing here ever executes code contained in an API response;
 * the reply is only ever read as data.
 */
public class ApiService {

    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 15;
    private static final List<String> VALID_OPTIONS = List.of("A", "B", "C", "D");

    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this(new ApiClient());
    }

    public ApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
    }

    /** True while an API key is configured; does not confirm the key is valid or the service is reachable. */
    public boolean isAvailable() {
        return ApiConfig.hasApiKey();
    }

    /**
     * Asks the AI for revision flashcards on a subject (and, optionally, one topic within it).
     *
     * @return a validated, non-empty list of generated flashcards, ready to preview
     * @throws ApiException if the API is unavailable, the request fails, or the reply
     *                       contains no usable flashcards
     */
    public List<GeneratedFlashcardDTO> generateFlashcards(String subjectName, String topicName, int count)
            throws ApiException {
        int requested = clampCount(count);
        String topicPhrase = topicPhrase(topicName);

        String system = "You are a study assistant that writes concise exam revision flashcards. "
                + "Reply with ONLY a JSON array and nothing else - no markdown formatting, no code fences, "
                + "no commentary before or after it. "
                + "Each element of the array must be a JSON object with exactly two string fields: "
                + "\"front\" (a short question or prompt) and \"back\" (a concise, correct answer).";
        String user = "Generate " + requested + " revision flashcard(s) for the subject \"" + subjectName + "\""
                + topicPhrase + ". Keep each front and back short enough to fit on a single flashcard.";

        String rawText = apiClient.sendMessage(system, user);
        List<GeneratedFlashcardDTO> parsed = parseJsonArray(rawText, new TypeReference<List<GeneratedFlashcardDTO>>() {
        });

        List<GeneratedFlashcardDTO> valid = new ArrayList<>();
        for (GeneratedFlashcardDTO card : parsed) {
            if (card == null) {
                continue;
            }
            String front = trimToNull(card.getFront());
            String back = trimToNull(card.getBack());
            if (front == null || back == null) {
                continue;
            }
            card.setFront(front);
            card.setBack(back);
            card.setSelected(true);
            valid.add(card);
            if (valid.size() >= requested) {
                break;
            }
        }

        if (valid.isEmpty()) {
            throw new ApiException("The AI did not return any usable flashcards. Please try again.");
        }
        return valid;
    }

    /**
     * Asks the AI for multiple-choice quiz questions on a subject (and, optionally, one topic within it).
     * Every returned entry is guaranteed to have four non-empty options and a
     * {@code correctOption} of exactly "A", "B", "C" or "D".
     *
     * @return a validated, non-empty list of generated questions, ready to preview
     * @throws ApiException if the API is unavailable, the request fails, or the reply
     *                       contains no usable questions
     */
    public List<GeneratedQuestionDTO> generateQuizQuestions(String subjectName, String topicName, int count)
            throws ApiException {
        int requested = clampCount(count);
        String topicPhrase = topicPhrase(topicName);

        String system = "You are a study assistant that writes exam-style multiple-choice quiz questions. "
                + "Reply with ONLY a JSON array and nothing else - no markdown formatting, no code fences, "
                + "no commentary before or after it. "
                + "Each element of the array must be a JSON object with exactly six string fields: "
                + "\"question\", \"optionA\", \"optionB\", \"optionC\", \"optionD\", and \"correctOption\" "
                + "(which must be exactly one of the letters \"A\", \"B\", \"C\" or \"D\"). "
                + "Exactly one option must be correct.";
        String user = "Generate " + requested + " multiple-choice quiz question(s) for the subject \""
                + subjectName + "\"" + topicPhrase + ".";

        String rawText = apiClient.sendMessage(system, user);
        List<GeneratedQuestionDTO> parsed = parseJsonArray(rawText, new TypeReference<List<GeneratedQuestionDTO>>() {
        });

        List<GeneratedQuestionDTO> valid = new ArrayList<>();
        for (GeneratedQuestionDTO question : parsed) {
            if (question == null) {
                continue;
            }
            String questionText = trimToNull(question.getQuestion());
            String optionA = trimToNull(question.getOptionA());
            String optionB = trimToNull(question.getOptionB());
            String optionC = trimToNull(question.getOptionC());
            String optionD = trimToNull(question.getOptionD());
            String correctOption = normalizeOption(question.getCorrectOption());

            if (questionText == null || optionA == null || optionB == null || optionC == null
                    || optionD == null || correctOption == null) {
                continue;
            }

            question.setQuestion(questionText);
            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectOption(correctOption);
            question.setSelected(true);
            valid.add(question);
            if (valid.size() >= requested) {
                break;
            }
        }

        if (valid.isEmpty()) {
            throw new ApiException("The AI did not return any usable quiz questions. Please try again.");
        }
        return valid;
    }

    // ----- Parsing / validation helpers ---------------------------------

    private <T> List<T> parseJsonArray(String rawText, TypeReference<List<T>> typeReference) throws ApiException {
        String jsonArray = extractJsonArray(stripCodeFences(rawText));
        try {
            List<T> result = objectMapper.readValue(jsonArray, typeReference);
            return result == null ? List.of() : result;
        } catch (IOException e) {
            throw new ApiException("The AI response was not valid JSON and could not be used.", e);
        }
    }

    /** Strips a leading/trailing ```` ```json ```` style code fence, if the model added one despite being asked not to. */
    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence >= 0) {
                trimmed = trimmed.substring(0, closingFence);
            }
        }
        return trimmed.trim();
    }

    /** Narrows the reply down to the {@code [ ... ]} JSON array, in case the model added stray text around it. */
    private String extractJsonArray(String text) throws ApiException {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0 || end < start) {
            throw new ApiException("The AI response did not contain a JSON list and could not be used.");
        }
        return text.substring(start, end + 1);
    }

    private String topicPhrase(String topicName) {
        String trimmed = trimToNull(topicName);
        return trimmed == null ? "" : (", focused specifically on the topic \"" + trimmed + "\"");
    }

    private String normalizeOption(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase();
        return VALID_OPTIONS.contains(upper) ? upper : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampCount(int count) {
        if (count < MIN_COUNT) {
            return MIN_COUNT;
        }
        return Math.min(count, MAX_COUNT);
    }
}
