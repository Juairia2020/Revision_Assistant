package com.revisionassistant.util;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

/**
 * Applies the application's dark design system to native JavaFX
 * {@link Alert} dialogs. Alerts render in their own Stage/Scene and do not
 * automatically pick up the main window's stylesheet, so without this they
 * fall back to the default light "Modena" look - a jarring white popup on
 * top of the app's dark theme. This is a pure presentation concern: it does
 * not change what a dialog says or does, only how it is painted.
 */
public final class DialogStyler {

    private static final String STYLESHEET =
            DialogStyler.class.getResource("/com/revisionassistant/css/style.css").toExternalForm();

    private DialogStyler() {
    }

    /** Attaches the app stylesheet and a type-specific accent class to the alert's dialog pane. */
    public static void style(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        if (!pane.getStylesheets().contains(STYLESHEET)) {
            pane.getStylesheets().add(STYLESHEET);
        }
        pane.getStyleClass().add("app-dialog");
        String accentClass = switch (alert.getAlertType()) {
            case ERROR -> "app-dialog-error";
            case WARNING -> "app-dialog-warning";
            case CONFIRMATION -> "app-dialog-confirm";
            default -> "app-dialog-info";
        };
        pane.getStyleClass().add(accentClass);
    }
}
