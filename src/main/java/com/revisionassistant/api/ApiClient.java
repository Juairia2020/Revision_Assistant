package com.revisionassistant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * All HTTP communication with the AI Messages API lives here, and
 * nowhere else - no other class opens a socket or reads
 * {@link ApiConfig}'s network-facing settings directly.
 * <p>
 * This class only knows how to send one system+user prompt and hand
 * back the raw text the model replied with. It does not know or care
 * whether that text is a flashcard, a quiz question, or anything else
 * - shaping the prompt and interpreting the reply is
 * {@link com.revisionassistant.service.ApiService}'s job. Every
 * failure - a missing key, no network, a timeout, a non-2xx status, an
 * unreadable response body - is reported as a single {@link ApiException}
 * carrying a message that is safe to show directly to the user.
 */
public class ApiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2048;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends one request to the AI API and returns the plain text of
     * the model's reply.
     *
     * @param systemPrompt instructions describing the task and the exact JSON shape expected back
     * @param userPrompt   the specific request (subject, topic, how many items)
     * @return the model's reply text, never {@code null} or blank
     * @throws ApiException if no key is configured, the network call fails, the API
     *                       returns a non-2xx status, or the response body is empty or unreadable
     */
    public String sendMessage(String systemPrompt, String userPrompt) throws ApiException {
        if (!ApiConfig.hasApiKey()) {
            throw new ApiException("AI generation is unavailable because no API key is configured. "
                    + "Set the REVISION_ASSISTANT_API_KEY environment variable, or add api.key to config.properties, to enable it.");
        }

        String requestJson = buildRequestBody(systemPrompt, userPrompt);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("content-type", "application/json")
                    .header("x-api-key", ApiConfig.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ApiException("Could not build the AI request.", e);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ApiException("The AI request timed out. Please try again.", e);
        } catch (IOException e) {
            throw new ApiException("Could not reach the AI service. Check your internet connection and try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("The AI request was interrupted.", e);
        }

        checkStatus(response.statusCode());

        String body = response.body();
        if (body == null || body.isBlank()) {
            throw new ApiException("The AI service returned an empty response.");
        }

        ApiResponse parsed;
        try {
            parsed = objectMapper.readValue(body, ApiResponse.class);
        } catch (IOException e) {
            throw new ApiException("The AI service returned a response that could not be understood.", e);
        }

        String text = parsed.getFirstText();
        if (text == null || text.isBlank()) {
            throw new ApiException("The AI service did not return any usable content.");
        }
        return text;
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) throws ApiException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", ApiConfig.getModel());
        root.put("max_tokens", MAX_TOKENS);
        root.put("system", systemPrompt);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new ApiException("Could not build the AI request.", e);
        }
    }

    private void checkStatus(int statusCode) throws ApiException {
        if (statusCode == 401 || statusCode == 403) {
            throw new ApiException("The configured API key was rejected. Check your credential and try again.");
        }
        if (statusCode == 429) {
            throw new ApiException("The AI service is currently rate-limiting requests. Please try again shortly.");
        }
        if (statusCode / 100 == 5) {
            throw new ApiException("The AI service is temporarily unavailable (HTTP " + statusCode + "). Please try again later.");
        }
        if (statusCode / 100 != 2) {
            throw new ApiException("The AI service returned an error (HTTP " + statusCode + ").");
        }
    }
}
