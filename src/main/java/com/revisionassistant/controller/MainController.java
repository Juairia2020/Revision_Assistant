package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for MainView.fxml. Only handles navigation between the
 * Dashboard, Subjects and Topics views - it has no SQL and no business
 * rules of its own.
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        contentArea.getChildren().setAll(buildDashboard());
    }

    @FXML
    private void showSubjects() {
        loadView("/com/revisionassistant/fxml/SubjectView.fxml");
    }

    @FXML
    private void showTopics() {
        loadView("/com/revisionassistant/fxml/TopicView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Unable to load view: " + e.getMessage());
        }
    }

    /**
     * Builds a small summary panel showing subject/topic counts. Built
     * in code rather than its own FXML file since it is just a handful
     * of read-only labels.
     */
    private VBox buildDashboard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("dashboard");

        Label title = new Label("Dashboard");
        title.getStyleClass().add("dashboard-title");
        box.getChildren().add(title);

        try {
            SubjectService subjectService = new SubjectService();
            TopicService topicService = new TopicService();

            List<Subject> subjects = subjectService.getAllSubjects();
            int totalTopics = 0;
            int completedTopics = 0;

            for (Subject subject : subjects) {
                List<Topic> topics = topicService.getTopicsForSubject(subject.getId());
                totalTopics += topics.size();
                for (Topic topic : topics) {
                    if (topic.isCompleted()) {
                        completedTopics++;
                    }
                }
            }

            box.getChildren().add(new Label("Subjects: " + subjects.size()));
            box.getChildren().add(new Label("Topics: " + totalTopics));
            box.getChildren().add(new Label("Completed: " + completedTopics + " / " + totalTopics));
            box.getChildren().add(new Label("Use the sidebar to manage subjects and topics."));
        } catch (SQLException e) {
            box.getChildren().add(new Label("Could not load dashboard data: " + e.getMessage()));
        }

        return box;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }
}
