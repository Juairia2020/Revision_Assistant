package com.revisionassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One flashcard suggested by the AI, before the user has reviewed and
 * saved it. Shaped to match the JSON the model is asked to return -
 * Jackson deserializes straight into this class, no manual string
 * parsing.
 * <p>
 * This is a preview-only object. It never touches the database itself;
 * once the user confirms which cards to keep, the controller passes
 * their front/back text into the existing {@code FlashcardService},
 * exactly as if they had been typed in by hand.
 * <p>
 * {@link #selected} is not part of the API response - it only exists
 * so the preview table in {@code FlashcardController} can track which
 * generated cards the user wants to keep.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedFlashcardDTO {

    @JsonProperty("front")
    private String front;

    @JsonProperty("back")
    private String back;

    @JsonIgnore
    private boolean selected = true;

    public GeneratedFlashcardDTO() {
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
