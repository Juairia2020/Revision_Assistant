package com.revisionassistant.util;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

/**
 * Gives an existing empty-state message a small icon above its text.
 * Purely presentational: the label keeps the same text it always had.
 */
public final class EmptyStates {

    private EmptyStates() {
    }

    public static void decorate(Label label) {
        if (label == null || label.getGraphic() != null) {
            return;
        }
        Circle disc = new Circle(20);
        disc.getStyleClass().add("empty-state-disc");

        // Simple "tray" glyph drawn as a vector path (no image asset needed).
        SVGPath tray = new SVGPath();
        tray.setContent("M-9 -1 L-5 -8 L5 -8 L9 -1 L9 7 L-9 7 Z M-9 -1 L-3 -1 L-2 2 L2 2 L3 -1 L9 -1");
        tray.getStyleClass().add("empty-state-glyph");

        StackPane icon = new StackPane(disc, tray);
        label.setGraphic(icon);
        label.setContentDisplay(ContentDisplay.TOP);
        label.setGraphicTextGap(10);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    }
}
