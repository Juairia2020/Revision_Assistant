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

    private final com.revisionassistant.service.SubjectService subjectService =
            new com.revisionassistant.service.SubjectService();
    private final com.revisionassistant.service.TopicService topicService =
            new com.revisionassistant.service.TopicService();
    private final com.revisionassistant.service.StudySessionService studySessionService =
            new com.revisionassistant.service.StudySessionService();

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
        boolean workSessionJustFinished = currentMode == Mode.WORK;
        int finishedWorkMinutes = totalSeconds / 60;

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

        if (workSessionJustFinished && finishedWorkMinutes > 0) {
            promptRecordStudySession(finishedWorkMinutes);
        }
    }

    /**
     * Offers a compact, one-step dialog to log the work session that
     * just finished as a real {@code StudySession}, so Pomodoro
     * contributes to the same study history everything else does
     * instead of being an island. Declining is one click; recording
     * it only asks for what's actually needed (subject, optional
     * topic) since the duration is already known.
     */
    private void promptRecordStudySession(int minutes) {
        try {
            java.util.List<com.revisionassistant.model.Subject> subjects = subjectService.getAllSubjects();
            if (subjects.isEmpty()) {
                return; // nothing to log against yet - don't interrupt with a dialog that can't be used
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Focus session complete");
            dialog.setHeaderText("Log this " + minutes + "-minute session as study time?");
            if (timerLabel != null && timerLabel.getScene() != null) {
                dialog.initOwner(timerLabel.getScene().getWindow());
            }
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            com.revisionassistant.util.DialogStyler.style(dialog);

            ComboBox<com.revisionassistant.model.Subject> subjectCombo = new ComboBox<>();
            subjectCombo.setItems(javafx.collections.FXCollections.observableArrayList(subjects));
            subjectCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(com.revisionassistant.model.Subject s) { return s == null ? "" : s.getName(); }
                @Override public com.revisionassistant.model.Subject fromString(String s) { return null; }
            });
            subjectCombo.setPromptText("Subject");
            subjectCombo.setMaxWidth(Double.MAX_VALUE);

            ComboBox<com.revisionassistant.model.Topic> topicCombo = new ComboBox<>();
            topicCombo.setPromptText("Topic (optional)");
            topicCombo.setMaxWidth(Double.MAX_VALUE);
            topicCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(com.revisionassistant.model.Topic t) { return t == null ? "" : t.getName(); }
                @Override public com.revisionassistant.model.Topic fromString(String s) { return null; }
            });
            subjectCombo.valueProperty().addListener((obs, old, subject) -> {
                topicCombo.getItems().clear();
                topicCombo.setValue(null);
                if (subject != null) {
                    try {
                        topicCombo.getItems().addAll(topicService.getTopicsForSubject(subject.getId()));
                    } catch (java.sql.SQLException ignored) {
                        // leave the topic list empty; logging by subject alone still works
                    }
                }
            });

            VBox content = new VBox(10, subjectCombo, topicCombo);
            content.setPadding(new javafx.geometry.Insets(12, 4, 4, 4));
            content.setPrefWidth(320);
            dialog.getDialogPane().setContent(content);

            ButtonType logButtonType = new ButtonType("Log Session", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(logButtonType, ButtonType.CANCEL);
            javafx.scene.Node logButton = dialog.getDialogPane().lookupButton(logButtonType);
            logButton.disableProperty().bind(subjectCombo.valueProperty().isNull());

            dialog.showAndWait().ifPresent(result -> {
                if (result != logButtonType) {
                    return;
                }
                com.revisionassistant.model.Subject subject = subjectCombo.getValue();
                com.revisionassistant.model.Topic topic = topicCombo.getValue();
                try {
                    studySessionService.addSession(subject.getId(), topic == null ? null : topic.getId(),
                            java.time.LocalDate.now(), minutes, "Logged from Pomodoro");
                    if (statusLabel != null) {
                        statusLabel.setText("Study session logged (" + minutes + " min).");
                    }
                } catch (IllegalArgumentException | java.sql.SQLException e) {
                    if (statusLabel != null) {
                        statusLabel.setText("Could not log the session.");
                    }
                }
            });
        } catch (java.sql.SQLException e) {
            // Subjects couldn't be loaded - skip the prompt rather than block the timer.
        } catch (RuntimeException e) {
            // Anything unexpected in the dialog itself (styling, layout, etc.) should never
            // silently disappear on the FX thread - log it so it's actually diagnosable,
            // and still let the timer carry on into the break uninterrupted.
            e.printStackTrace();
        }
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
                timerLabel.setStyle("-fx-text-fill: -ra-text; -fx-font-size: 52px; -fx-font-weight: bold;");
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
