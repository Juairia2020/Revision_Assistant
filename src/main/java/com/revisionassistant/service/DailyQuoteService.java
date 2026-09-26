package com.revisionassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Small JSON-over-HTTP example used by the Dashboard Daily Quote card. */
public class DailyQuoteService {
    public record Quote(String text, String author) {}

    private static final URI ENDPOINT = URI.create("https://zenquotes.io/api/random");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Quote fetchToday() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IOException("Quote service returned HTTP " + response.statusCode());
        JsonNode root = mapper.readTree(response.body());
        if (!root.isArray() || root.isEmpty()) throw new IOException("Quote response was empty.");
        JsonNode first = root.get(0);
        String text = first.path("q").asText("").trim();
        String author = first.path("a").asText("Unknown").trim();
        if (text.isEmpty()) throw new IOException("Quote response did not contain text.");
        return new Quote(text, author.isEmpty() ? "Unknown" : author);
    }
}
