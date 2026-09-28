package com.revisionassistant.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.StageStyle;

/**
 * Makes native JavaFX dialogs look like part of the app instead of an OS
 * popup. Dialogs render in their own Stage/Scene, so by default they get
 * neither the app stylesheet, nor the user's light/dark theme, nor anything
 * but the operating system's title bar. This class fixes all three:
 * it attaches the stylesheet, applies the current theme when the dialog is
 * about to show, and removes the native title bar so the rounded, themed
 * dialog card is all the user sees. Purely presentational - it never
 * changes what a dialog says or does.
 */
public final class DialogStyler {

    private static final String STYLESHEET =
            DialogStyler.class.getResource("/com/revisionassistant/css/style.css").toExternalForm();

    private DialogStyler() {
    }

    /** Styles an {@link Alert}: shared dialog chrome, a per-type accent and a matching icon badge. */
    public static void style(Alert alert) {
        applyBase(alert);
        String kind = switch (alert.getAlertType()) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case CONFIRMATION -> "confirm";
            default -> "info";
        };
        addClass(alert.getDialogPane(), "app-dialog-" + kind);
        alert.setGraphic(buildBadge(kind));
    }

    /** Styles any other dialog (TextInputDialog, a custom Dialog) with the same chrome. */
    public static void style(Dialog<?> dialog) {
        applyBase(dialog);
        addClass(dialog.getDialogPane(), "app-dialog-input");
    }

    private static void applyBase(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStylesheets().contains(STYLESHEET)) {
            pane.getStylesheets().add(STYLESHEET);
        }
        addClass(pane, "app-dialog");

        try {
            dialog.initStyle(StageStyle.TRANSPARENT);   // no native "Message" title bar
        } catch (IllegalStateException ignored) {
            // Already showing - keep the native frame rather than failing.
        }
        dialog.setOnShowing(e -> {
            Scene scene = pane.getScene();
            if (scene != null) {
                ThemeManager.applyForCurrentUser(scene);  // dark/light like the main window
                scene.setFill(Color.TRANSPARENT);         // let the rounded corners show
            }
        });
    }

    private static void addClass(DialogPane pane, String styleClass) {
        if (!pane.getStyleClass().contains(styleClass)) {
            pane.getStyleClass().add(styleClass);
        }
    }

    /** Round tinted badge with a small vector glyph, replacing the default blue image. */
    private static Node buildBadge(String kind) {
        Group glyph = new Group();
        switch (kind) {
            case "error" -> glyph.getChildren().addAll(stroke(new Line(0, 0, 11, 11)), stroke(new Line(11, 0, 0, 11)));
            case "warning" -> glyph.getChildren().addAll(stroke(new Line(5.5, 0, 5.5, 7)), dot(5.5, 10.6));
            default -> glyph.getChildren().addAll(dot(5.5, 1), stroke(new Line(5.5, 4.5, 5.5, 11)));
        }
        StackPane badge = new StackPane(glyph);
        badge.setMinSize(38, 38);
        badge.setPrefSize(38, 38);
        badge.setMaxSize(38, 38);
        badge.getStyleClass().addAll("dialog-badge", "dialog-badge-" + kind);
        return badge;
    }

    private static Line stroke(Line line) {
        line.setStrokeWidth(2.2);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.getStyleClass().add("dialog-badge-stroke");
        return line;
    }

    private static Circle dot(double x, double y) {
        Circle dot = new Circle(x, y, 1.4);
        dot.getStyleClass().add("dialog-badge-dot");
        return dot;
    }
}