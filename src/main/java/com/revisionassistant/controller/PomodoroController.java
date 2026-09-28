package com.revisionassistant.controller;

import com.revisionassistant.service.PomodoroTimerService;
import com.revisionassistant.service.PomodoroTimerService.Mode;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;

/**
 * View for the Pomodoro timer. All timer state lives in
 * {@link PomodoroTimerService}; this controller only mirrors it, so leaving
 * and re-opening this page never resets or pauses the countdown.
 */
public class PomodoroController {

    @FXML private Label timerLabel;
    @FXML private Label modeLabel;
    @FXML private Label sessionCountLabel;
    @FXML private Label statusLabel;
    @FXML private Button startPauseButton;
    @FXML private Button resetButton;
    @FXML private TextField workDurationField;
    @FXML private TextField shortBreakField;
    @FXML private TextField longBreakField;
    @FXML private Arc timerArc;
    @FXML private ProgressBar timerBar;
    @FXML private VBox workCard;
    @FXML private VBox shortBreakCard;
    @FXML private VBox longBreakCard;

    private final PomodoroTimerService timer = PomodoroTimerService.getInstance();
    private final InvalidationListener refresh = obs -> updateDisplay();

    @FXML
    public void initialize() {
        workDurationField.setText(String.valueOf(timer.getWorkMinutes()));
        shortBreakField.setText(String.valueOf(timer.getShortBreakMinutes()));
        longBreakField.setText(String.valueOf(timer.getLongBreakMinutes()));
        InvalidationListener durations = obs -> pushDurations();
        workDurationField.textProperty().addListener(durations);
        shortBreakField.textProperty().addListener(durations);
        longBreakField.textProperty().addListener(durations);

        timer.secondsRemainingProperty().addListener(refresh);
        timer.totalSecondsProperty().addListener(refresh);
        timer.runningProperty().addListener(refresh);
        timer.modeProperty().addListener(refresh);
        timer.sessionCountProperty().addListener(refresh);
        timer.statusProperty().addListener(refresh);

        // The service outlives this view: drop our listeners when the page is replaced.
        timerLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) detach();
        });
        updateDisplay();
    }

    private void detach() {
        timer.secondsRemainingProperty().removeListener(refresh);
        timer.totalSecondsProperty().removeListener(refresh);
        timer.runningProperty().removeListener(refresh);
        timer.modeProperty().removeListener(refresh);
        timer.sessionCountProperty().removeListener(refresh);
        timer.statusProperty().removeListener(refresh);
    }

    @FXML
    private void handleStartPause() {
        pushDurations();
        timer.startPause();
    }

    @FXML
    private void handleReset() {
        pushDurations();
        timer.reset();
    }

    private void pushDurations() {
        timer.setDurations(parse(workDurationField, timer.getWorkMinutes()),
                parse(shortBreakField, timer.getShortBreakMinutes()),
                parse(longBreakField, timer.getLongBreakMinutes()));
    }

    private int parse(TextField field, int fallback) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void updateDisplay() {
        int remaining = timer.secondsRemainingProperty().get();
        int total = timer.totalSecondsProperty().get();
        boolean running = timer.isRunning();
        Mode mode = timer.getMode();

        timerLabel.setText(String.format("%02d:%02d", remaining / 60, remaining % 60));
        double fraction = total == 0 ? 0.0 : (double) (total - remaining) / total;
        if (timerBar != null) timerBar.setProgress(fraction);
        if (timerArc != null) timerArc.setLength(-360.0 * fraction);

        if (remaining <= 60 && running) {
            timerLabel.setStyle("-fx-text-fill: #FB7185; -fx-font-size: 52px; -fx-font-weight: bold;");
        } else if (remaining <= 180 && running) {
            timerLabel.setStyle("-fx-text-fill: #FFB020; -fx-font-size: 52px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: -ra-text; -fx-font-size: 52px; -fx-font-weight: bold;");
        }

        startPauseButton.setText(running ? "⏸  Pause" : (remaining < total ? "▶  Resume" : "▶  Start"));
        statusLabel.setText(timer.statusProperty().get());
        int sessions = timer.sessionCountProperty().get();
        sessionCountLabel.setText(sessions + " session" + (sessions == 1 ? "" : "s") + " completed");

        if (modeLabel != null) {
            modeLabel.setText(switch (mode) {
                case WORK -> "Focus Session";
                case SHORT_BREAK -> "Short Break";
                case LONG_BREAK -> "Long Break";
            });
        }
        PseudoClass active = PseudoClass.getPseudoClass("active");
        if (workCard != null) workCard.pseudoClassStateChanged(active, mode == Mode.WORK);
        if (shortBreakCard != null) shortBreakCard.pseudoClassStateChanged(active, mode == Mode.SHORT_BREAK);
        if (longBreakCard != null) longBreakCard.pseudoClassStateChanged(active, mode == Mode.LONG_BREAK);
    }
}
