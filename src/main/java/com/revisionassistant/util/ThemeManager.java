package com.revisionassistant.util;

import com.revisionassistant.dao.UserPreferencesDAO;
import com.revisionassistant.model.UserPreferences;
import com.revisionassistant.session.CurrentUser;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.sql.SQLException;

/** Applies the selected application theme to a scene. Light is the default. */
public final class ThemeManager {
    private ThemeManager() {}

    public static boolean isDark(String theme) {
        return "DARK".equalsIgnoreCase(theme);
    }

    public static void apply(Scene scene, String theme) {
        if (scene == null) return;
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        String normalized = isDark(theme) ? "theme-dark" : "theme-light";
        root.getStyleClass().add(normalized);
        scene.setFill(isDark(theme) ? Color.web("#0B1220") : Color.WHITE);
    }

    public static void applyForCurrentUser(Scene scene) {
        String theme = "LIGHT";
        if (CurrentUser.get() != null) {
            try {
                UserPreferences p = new UserPreferencesDAO().findByUserId(CurrentUser.get().getId());
                theme = p.getTheme();
            } catch (SQLException ignored) {
                // Keep the safe white default if preferences cannot be read.
            }
        }
        apply(scene, theme);
    }
}
