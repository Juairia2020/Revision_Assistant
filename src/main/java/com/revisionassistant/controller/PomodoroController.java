package com.revisionassistant.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

/**
 * Pomodoro timer controller. Handles start, pause, resume, reset,
 * session counting, and visual states (work / short break / long break).
 * All UI state; no database persistence needed for the timer itself.
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

    private enum Mode { WORK, SHORT_BREAK, LONG_BREAK }

    private Timeline timeline;
    private int secondsRemaining;
    private int totalSeconds;
    private int sessionCount = 0;
    private boolean running = false;
    private Mode currentMode = Mode.WORK;

    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;

    @FXML
    public void initialize() {
        reset();
    }

    @FXML
    private void handleStartPause() {
        if (running) {
            timeline.pause();
            running = false;
            startPauseButton.setText("▶  Resume");
            statusLabel.setText("Paused");
        } else {
            startTimer();
            startPauseButton.setText("⏸  Pause");
        }
    }

    @FXML
    private void handleReset() {
        if (timeline != null) timeline.stop();
        running = false;
        sessionCount = 0;
        currentMode = Mode.WORK;
        reset();
    }

    private void startTimer() {
        if (secondsRemaining <= 0) {
            secondsRemaining = getDurationSeconds(currentMode);
            totalSeconds = secondsRemaining;
        }
        running = true;
        statusLabel.setText(currentMode == Mode.WORK ? "Focus time!" : "Break time!");

        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tick() {
        secondsRemaining--;
        updateDisplay();
        if (secondsRemaining <= 0) {
            timeline.stop();
            running = false;
            onSessionComplete();
        }
    }

    private void onSessionComplete() {
        if (currentMode == Mode.WORK) {
            sessionCount++;
            sessionCountLabel.setText(sessionCount + " session" + (sessionCount == 1 ? "" : "s") + " completed");
            if (sessionCount % SESSIONS_BEFORE_LONG_BREAK == 0) {
                currentMode = Mode.LONG_BREAK;
                statusLabel.setText("Great work! Time for a long break.");
            } else {
                currentMode = Mode.SHORT_BREAK;
                statusLabel.setText("Nice! Take a short break.");
            }
        } else {
            currentMode = Mode.WORK;
            statusLabel.setText("Break done! Ready for another session?");
        }
        startPauseButton.setText("▶  Start");
        secondsRemaining = getDurationSeconds(currentMode);
        totalSeconds = secondsRemaining;
        updateDisplay();
        highlightMode();
    }

    @FXML
    private void reset() {
        if (timeline != null) timeline.stop();
        running = false;
        currentMode = Mode.WORK;
        secondsRemaining = getDurationSeconds(Mode.WORK);
        totalSeconds = secondsRemaining;
        startPauseButton.setText("▶  Start");
        statusLabel.setText("Ready to focus?");
        sessionCountLabel.setText("0 sessions completed");
        updateDisplay();
        highlightMode();
    }

    private void updateDisplay() {
        int min = secondsRemaining / 60;
        int sec = secondsRemaining % 60;
        timerLabel.setText(String.format("%02d:%02d", min, sec));
        double fraction = totalSeconds == 0 ? 0.0 : (double) (totalSeconds - secondsRemaining) / totalSeconds;
        if (timerBar != null) timerBar.setProgress(fraction);
        if (timerArc != null) timerArc.setLength(-360.0 * fraction);

        // Colour the timer label based on urgency
        if (timerLabel != null) {
            if (secondsRemaining <= 60 && running) {
                timerLabel.setStyle("-fx-text-fill: #FB7185; -fx-font-size: 52px; -fx-font-weight: bold;");
            } else if (secondsRemaining <= 180 && running) {
                timerLabel.setStyle("-fx-text-fill: #FFB020; -fx-font-size: 52px; -fx-font-weight: bold;");
            } else {
                timerLabel.setStyle("-fx-text-fill: #F1F4FB; -fx-font-size: 52px; -fx-font-weight: bold;");
            }
        }
    }

    private void highlightMode() {
        if (modeLabel == null) return;
        switch (currentMode) {
            case WORK -> modeLabel.setText("Focus Session");
            case SHORT_BREAK -> modeLabel.setText("Short Break");
            case LONG_BREAK -> modeLabel.setText("Long Break");
        }
        if (workCard != null) workCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), currentMode == Mode.WORK);
        if (shortBreakCard != null) shortBreakCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), currentMode == Mode.SHORT_BREAK);
        if (longBreakCard != null) longBreakCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), currentMode == Mode.LONG_BREAK);
    }

    private int getDurationSeconds(Mode mode) {
        try {
            return switch (mode) {
                case WORK -> Integer.parseInt(workDurationField.getText().trim()) * 60;
                case SHORT_BREAK -> Integer.parseInt(shortBreakField.getText().trim()) * 60;
                case LONG_BREAK -> Integer.parseInt(longBreakField.getText().trim()) * 60;
            };
        } catch (Exception e) {
            return switch (mode) {
                case WORK -> 25 * 60;
                case SHORT_BREAK -> 5 * 60;
                case LONG_BREAK -> 15 * 60;
            };
        }
    }
}
