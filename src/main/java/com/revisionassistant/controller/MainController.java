package com.revisionassistant.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controller for MainView.fxml. Only handles navigation between the
 * Dashboard, Subjects, Topics, Tasks, Exams and Study Sessions views -
 * it has no SQL and no business rules of its own.
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button dashboardButton;
    @FXML
    private Button subjectsButton;
    @FXML
    private Button topicsButton;
    @FXML
    private Button tasksButton;
    @FXML
    private Button examsButton;
    @FXML
    private Button studySessionsButton;
    @FXML
    private Button flashcardsButton;
    @FXML
    private Button quizButton;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        loadView("/com/revisionassistant/fxml/DashboardView.fxml", dashboardButton);
    }

    @FXML
    private void showSubjects() {
        loadView("/com/revisionassistant/fxml/SubjectView.fxml", subjectsButton);
    }

    @FXML
    private void showTopics() {
        loadView("/com/revisionassistant/fxml/TopicView.fxml", topicsButton);
    }

    @FXML
    private void showTasks() {
        loadView("/com/revisionassistant/fxml/TaskView.fxml", tasksButton);
    }

    @FXML
    private void showExams() {
        loadView("/com/revisionassistant/fxml/ExamView.fxml", examsButton);
    }

    @FXML
    private void showStudySessions() {
        loadView("/com/revisionassistant/fxml/StudySessionView.fxml", studySessionsButton);
    }

    @FXML
    private void showFlashcards() {
        loadView("/com/revisionassistant/fxml/FlashcardView.fxml", flashcardsButton);
    }

    @FXML
    private void showQuiz() {
        loadView("/com/revisionassistant/fxml/QuizView.fxml", quizButton);
    }

    private void loadView(String fxmlPath, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            markActive(activeButton);
        } catch (IOException e) {
            showError("Unable to load view: " + e.getMessage());
        }
    }

    /** Highlights the sidebar button for the view currently on screen. */
    private void markActive(Button activeButton) {
        for (Button button : new Button[]{dashboardButton, subjectsButton, topicsButton,
                tasksButton, examsButton, studySessionsButton, flashcardsButton, quizButton}) {
            button.getStyleClass().remove("sidebar-button-active");
        }
        activeButton.getStyleClass().add("sidebar-button-active");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }
}
