package com.revisionassistant.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revisionassistant.dto.ApiDemoResponseDTO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Service for the small Milestone 4B public API demonstration.
 * No secrets or user configuration are required: the endpoint is public.
 */
public class ApiDemoService {
    public static final String DEMO_ENDPOINT = "https://jsonplaceholder.typicode.com/todos/1";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiDemoService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Performs the HTTP request and converts the JSON response into a Java DTO.
     * This method is deliberately blocking and must be called from a background Task.
     */
    public ApiDemoResponseDTO loadSample() throws ApiDemoException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEMO_ENDPOINT))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ApiDemoException("The demo API request timed out. Please try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ApiDemoException("Could not reach the demo API. Check your network connection and try again.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiDemoException("The demo API returned HTTP " + response.statusCode() + ".");
        }

        String body = response.body();
        if (body == null || body.isBlank()) {
            throw new ApiDemoException("The demo API returned an empty response.");
        }

        try {
            ApiDemoResponseDTO result = objectMapper.readValue(body, ApiDemoResponseDTO.class);
            validate(result);
            return result;
        } catch (IOException | RuntimeException e) {
            throw new ApiDemoException("The demo API returned invalid JSON or an unexpected response.", e);
        }
    }

    private void validate(ApiDemoResponseDTO result) throws ApiDemoException {
        if (result == null
                || result.getId() == null
                || result.getUserId() == null
                || result.getTitle() == null
                || result.getTitle().isBlank()
                || result.getCompleted() == null) {
            throw new ApiDemoException("The demo API returned an unexpected response structure.");
        }
    }
}
