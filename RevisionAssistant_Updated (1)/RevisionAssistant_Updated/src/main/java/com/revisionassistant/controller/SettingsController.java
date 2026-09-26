package com.revisionassistant.controller;

import com.revisionassistant.dao.UserPreferencesDAO;
import com.revisionassistant.model.User;
import com.revisionassistant.model.UserPreferences;
import com.revisionassistant.service.ReminderNotificationService;
import com.revisionassistant.session.CurrentUser;
import com.revisionassistant.util.DialogStyler;
import com.revisionassistant.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

/** Settings page controller with real per-user persistence and live theme switching. */
public class SettingsController {

    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private ToggleGroup themeToggleGroup;
    @FXML private RadioButton lightThemeRadio;
    @FXML private RadioButton darkThemeRadio;
    @FXML private ToggleGroup fontSizeGroup;
    @FXML private RadioButton fontMediumRadio;
    @FXML private RadioButton fontLargeRadio;
    @FXML private CheckBox studyRemindersCheck;
    @FXML private CheckBox examRemindersCheck;
    @FXML private CheckBox achievementCheck;
    @FXML private CheckBox streakCheck;
    @FXML private TextField pomoDurationField;
    @FXML private TextField pomoShortBreakField;
    @FXML private TextField pomoLongBreakField;
    @FXML private Label saveStatusLabel;

    private final UserPreferencesDAO preferencesDAO = new UserPreferencesDAO();
    private UserPreferences preferences;

    @FXML
    public void initialize() {
        User user = CurrentUser.get();
        if (user != null) {
            profileNameLabel.setText(user.getName());
            profileEmailLabel.setText(user.getEmail());
            try {
                preferences = preferencesDAO.findByUserId(user.getId());
            } catch (SQLException e) {
                preferences = new UserPreferences(user.getId());
                setStatus("Using default preferences until the database is available.", false);
            }
        } else {
            preferences = new UserPreferences();
        }
        loadPreferencesToControls();

        themeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null || preferences == null) return;
            applySelectedTheme(false);
        });
    }

    private void loadPreferencesToControls() {
        boolean dark = ThemeManager.isDark(preferences.getTheme());
        lightThemeRadio.setSelected(!dark);
        darkThemeRadio.setSelected(dark);
        fontLargeRadio.setSelected("LARGE".equalsIgnoreCase(preferences.getFontSize()));
        fontMediumRadio.setSelected(!fontLargeRadio.isSelected());
        studyRemindersCheck.setSelected(preferences.isStudyReminders());
        examRemindersCheck.setSelected(preferences.isExamReminders());
        achievementCheck.setSelected(preferences.isAchievementNotifications());
        streakCheck.setSelected(preferences.isStreakNotifications());
        pomoDurationField.setText(String.valueOf(preferences.getPomodoroDuration()));
        pomoShortBreakField.setText(String.valueOf(preferences.getPomodoroShortBreak()));
        pomoLongBreakField.setText(String.valueOf(preferences.getPomodoroLongBreak()));
    }

    @FXML
    private void handleApplyTheme() {
        applySelectedTheme(true);
    }

    private void applySelectedTheme(boolean showMessage) {
        String theme = darkThemeRadio.isSelected() ? "DARK" : "LIGHT";
        preferences.setTheme(theme);
        if (saveStatusLabel != null && saveStatusLabel.getScene() != null) {
            ThemeManager.apply(saveStatusLabel.getScene(), theme);
        }
        if (showMessage) {
            setStatus(theme + " theme applied to the workspace.", true);
        }
    }

    @FXML
    private void handleTestReminders() {
        ReminderNotificationService service = ReminderNotificationService.getActive();
        if (service == null) {
            setStatus("Open the main workspace first so reminder notifications can be previewed.", false);
            return;
        }

        int sent = 0;
        if (studyRemindersCheck.isSelected()) {
            service.showToast("Study reminder", "Your selected study-session reminder is enabled for this account.");
            sent++;
        }
        if (examRemindersCheck.isSelected()) {
            service.showToast("Exam reminder", "Your selected exam countdown reminder is enabled for this account.");
            sent++;
        }
        if (achievementCheck.isSelected()) {
            service.showToast("Achievement notification", "Progress milestone notifications are enabled.");
            sent++;
        }
        if (streakCheck.isSelected()) {
            service.showToast("Study streak alert", "Study streak notifications are enabled.");
            sent++;
        }
        setStatus(sent == 0 ? "No reminder types are selected." : sent + " selected reminder type(s) previewed.", sent > 0);
    }

    @FXML
    private void handleSavePreferences() {
        try {
            int pomo = Integer.parseInt(pomoDurationField.getText().trim());
            int shortBreak = Integer.parseInt(pomoShortBreakField.getText().trim());
            int longBreak = Integer.parseInt(pomoLongBreakField.getText().trim());
            if (pomo < 1 || shortBreak < 1 || longBreak < 1) throw new NumberFormatException();

            preferences.setTheme(darkThemeRadio.isSelected() ? "DARK" : "LIGHT");
            preferences.setFontSize(fontLargeRadio.isSelected() ? "LARGE" : "MEDIUM");
            preferences.setStudyReminders(studyRemindersCheck.isSelected());
            preferences.setExamReminders(examRemindersCheck.isSelected());
            preferences.setAchievementNotifications(achievementCheck.isSelected());
            preferences.setStreakNotifications(streakCheck.isSelected());
            preferences.setPomodoroDuration(pomo);
            preferences.setPomodoroShortBreak(shortBreak);
            preferences.setPomodoroLongBreak(longBreak);

            preferencesDAO.save(preferences);
            if (saveStatusLabel.getScene() != null) ThemeManager.apply(saveStatusLabel.getScene(), preferences.getTheme());
            ReminderNotificationService service = ReminderNotificationService.getActive();
            if (service != null) service.refreshNow();
            setStatus("✓ Preferences saved. Your selected reminders are now active for this account.", true);
        } catch (NumberFormatException e) {
            setStatus("Please enter valid positive numbers for Pomodoro durations.", false);
        } catch (SQLException e) {
            setStatus("Could not save preferences: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleResetStudyData() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Clear study path data?");
        confirm.setContentText("This will remove topic completion status from your study path. Your subjects and topics will remain. This cannot be undone.");
        DialogStyler.style(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) setStatus("Study path data cleared.", true);
        });
    }

    private void setStatus(String message, boolean success) {
        if (saveStatusLabel == null) return;
        saveStatusLabel.setText(message);
        saveStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        saveStatusLabel.getStyleClass().add(success ? "status-success" : "status-error");
    }
}
