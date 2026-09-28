package com.revisionassistant.util;

import com.revisionassistant.service.PomodoroTimerService;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * The "log this focus session?" dialog. It lives outside the Pomodoro view
 * so it can appear when a focus session finishes while the user is on any
 * other page.
 */
public final class PomodoroSessionPrompt {

    private PomodoroSessionPrompt() { }

    public static void prompt(int minutes) {
        if (!com.revisionassistant.session.CurrentUser.isLoggedIn()) return;
        com.revisionassistant.service.SubjectService subjectService = new com.revisionassistant.service.SubjectService();
        com.revisionassistant.service.TopicService topicService = new com.revisionassistant.service.TopicService();
        com.revisionassistant.service.StudySessionService studySessionService = new com.revisionassistant.service.StudySessionService();
        try {
            java.util.List<com.revisionassistant.model.Subject> subjects = subjectService.getAllSubjects();
            if (subjects.isEmpty()) {
                return; // nothing to log against yet - don't interrupt with a dialog that can't be used
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Focus session complete");
            dialog.setHeaderText("Log this " + minutes + "-minute session as study time?");
            javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst()
                    .ifPresent(dialog::initOwner);
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
                    PomodoroTimerService.getInstance().setStatus("Study session logged (" + minutes + " min).");
                } catch (IllegalArgumentException | java.sql.SQLException e) {
                    PomodoroTimerService.getInstance().setStatus("Could not log the session.");
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
}
