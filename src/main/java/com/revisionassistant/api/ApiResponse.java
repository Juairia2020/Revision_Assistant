package com.revisionassistant.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Shape of the JSON envelope returned by the AI Messages API
 * ({@code https://api.anthropic.com/v1/messages}). Jackson deserializes
 * the raw HTTP response body straight into this class - no manual
 * string parsing.
 * <p>
 * This class only models the envelope (who replied, and the list of
 * content blocks). The actual revision content - flashcards or quiz
 * questions - is plain text inside one of those blocks, written by the
 * model as a JSON array; {@link com.revisionassistant.service.ApiService}
 * is responsible for parsing that inner JSON into DTOs and validating it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse {

    @JsonProperty("content")
    private List<ContentBlock> content;

    @JsonProperty("stop_reason")
    private String stopReason;

    public ApiResponse() {
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    /**
     * The text of the first {@code "text"} content block, or
     * {@code null} if the response had no content blocks or none of
     * them were text (for example, if generation was cut off before
     * anything was written).
     */
    public String getFirstText() {
        if (content == null) {
            return null;
        }
        for (ContentBlock block : content) {
            if (block != null && "text".equals(block.getType()) && block.getText() != null) {
                return block.getText();
            }
        }
        return null;
    }

    /** One block of the model's reply. Only the plain-text shape is used by this application. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentBlock {

        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        public ContentBlock() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
