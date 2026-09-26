package com.revisionassistant.controller;

import com.revisionassistant.model.User;
import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import com.revisionassistant.session.CurrentUser;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Application shell controller. Navigation delegates to the existing views;
 * business logic remains inside the existing controllers and services.
 */
public class MainController {

    @FXML
    private StackPane contentArea;
    @FXML
    private VBox sidebar;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private StackPane tourOverlay;

    @FXML private Button dashboardButton;
    @FXML private Button subjectsButton;
    @FXML private Button topicsButton;
    @FXML private Button tasksButton;
    @FXML private Button examsButton;
    @FXML private Button studySessionsButton;
    @FXML private Button flashcardsButton;
    @FXML private Button quizButton;
    @FXML private Button studyToolsButton;

    @FXML
    public void initialize() {
        if (!CurrentUser.isLoggedIn()) {
            javafx.application.Platform.runLater(() -> {
                try {
                    AppNavigator.showLogin((javafx.stage.Stage) contentArea.getScene().getWindow());
                } catch (Exception ignored) {
                    // The application entry point normally prevents this path.
                }
            });
            return;
        }

        updateProfile();
        if (sidebar != null) {
            contentArea.sceneProperty().addListener((obs, oldScene, scene) -> {
                if (scene != null) {
                    updateResponsiveLayout(scene.getWidth());
                    scene.widthProperty().addListener((o, oldWidth, newWidth) ->
                            updateResponsiveLayout(newWidth.doubleValue()));
                }
            });
        }
        showDashboard();
        scheduleFirstLaunchTour();
    }

    private void updateResponsiveLayout(double width) {
        if (sidebar == null) return;
        if (width < 900) {
            sidebar.setPrefWidth(188);
            sidebar.setMinWidth(178);
            sidebar.setMaxWidth(198);
        } else if (width < 1050) {
            sidebar.setPrefWidth(202);
            sidebar.setMinWidth(188);
            sidebar.setMaxWidth(220);
        } else {
            sidebar.setPrefWidth(214);
            sidebar.setMinWidth(190);
            sidebar.setMaxWidth(238);
        }
    }

    private void scheduleFirstLaunchTour() {
        User user = CurrentUser.get();
        if (user == null || user.isOnboardingCompleted()) {
            return;
        }
        // The shell is loaded by AppNavigator before the scene is attached.
        // Scheduling after the current pulse guarantees that the overlay has
        // a real scene/window before the tour is inserted.
        javafx.application.Platform.runLater(() -> {
            if (CurrentUser.get() != null && !CurrentUser.get().isOnboardingCompleted()) {
                showFeatureTour();
            }
        });
    }

    private void updateProfile() {
        User user = CurrentUser.get();
        if (user != null) {
            profileNameLabel.setText(user.getName());
            profileEmailLabel.setText(user.getEmail());
        }
    }

    @FXML
    private void handleLogout() {
        new UserService().logout();
        try {
            AppNavigator.showLogin((javafx.stage.Stage) contentArea.getScene().getWindow());
        } catch (Exception e) {
            showError("Could not return to the login screen.");
        }
    }

    @FXML
    void showDashboard() {
        loadView("/com/revisionassistant/fxml/DashboardView.fxml", dashboardButton,
                "Dashboard", "A calm place to plan, practise, and review.");
    }

    @FXML
    private void showSubjects() {
        loadView("/com/revisionassistant/fxml/SubjectView.fxml", subjectsButton,
                "Subjects", "Organise the areas you are currently studying.");
    }

    @FXML
    private void showTopics() {
        loadView("/com/revisionassistant/fxml/TopicView.fxml", topicsButton,
                "Topics", "Build the topic structure behind your study plan.");
    }

    @FXML
    private void showTasks() {
        loadView("/com/revisionassistant/fxml/TaskView.fxml", tasksButton,
                "Study Planner", "Turn your study goals into manageable tasks.");
    }

    @FXML
    private void showExams() {
        loadView("/com/revisionassistant/fxml/ExamView.fxml", examsButton,
                "Exams", "Keep upcoming assessments and exam preparation together.");
    }

    @FXML
    private void showStudySessions() {
        loadView("/com/revisionassistant/fxml/StudySessionView.fxml", studySessionsButton,
                "Study Sessions", "Record focused study time and review your sessions.");
    }

    @FXML
    private void showFlashcards() {
        loadView("/com/revisionassistant/fxml/FlashcardView.fxml", flashcardsButton,
                "Flashcards", "Practise active recall with your saved cards.");
    }

    @FXML
    private void showQuiz() {
        loadView("/com/revisionassistant/fxml/QuizView.fxml", quizButton,
                "Quiz", "Test your understanding and revisit difficult questions.");
    }

    // Dashboard navigation callbacks. Kept separate from the FXML actions so
    // dashboard quick actions do not need to know how the application shell works.
    public void showSubjectsFromDashboard() { showSubjects(); }
    public void showTopicsFromDashboard() { showTopics(); }
    public void showTasksFromDashboard() { showTasks(); }
    public void showExamsFromDashboard() { showExams(); }
    public void showFlashcardsFromDashboard() { showFlashcards(); }
    public void showQuizFromDashboard() { showQuiz(); }

    @FXML
    private void showStudyTools() {
        loadView("/com/revisionassistant/fxml/StudyToolsView.fxml", studyToolsButton,
                "Study Tools", "Import compatible JSON and explore study utilities.");
    }

    @FXML
    private void handleReplayTour() {
        showFeatureTour();
    }

    private void showFeatureTour() {
        if (tourOverlay.getChildren().size() > 0) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/revisionassistant/fxml/FeatureTourView.fxml"));
            Parent tour = loader.load();
            FeatureTourController controller = loader.getController();
            controller.setMainController(this);
            tourOverlay.getChildren().setAll(tour);
            tourOverlay.setVisible(true);
            tourOverlay.setManaged(true);
            tourOverlay.setMouseTransparent(false);
            tourOverlay.setPickOnBounds(true);
            tourOverlay.toFront();
            tour.setOpacity(0.0);
            FadeTransition transition = new FadeTransition(Duration.millis(180), tour);
            transition.setFromValue(0.0);
            transition.setToValue(1.0);
            transition.play();
        } catch (IOException e) {
            showError("The feature tour could not be opened.");
        }
    }

    public void closeFeatureTour() {
        tourOverlay.getChildren().clear();
        tourOverlay.setMouseTransparent(true);
        tourOverlay.setVisible(false);
        tourOverlay.setManaged(false);
        clearTourHighlight();
    }

    public void highlightTourTarget(String target) {
        clearTourHighlight();
        // The Welcome and Finish steps intentionally pass a null target (no
        // sidebar item to highlight). A plain switch on a null String throws
        // an NPE, which was silently aborting the tour before it ever became
        // visible, so null is handled explicitly before the switch.
        Button targetButton = target == null ? null : switch (target) {
            case "dashboard" -> dashboardButton;
            case "subjects" -> subjectsButton;
            case "topics" -> topicsButton;
            case "planner" -> tasksButton;
            case "flashcards" -> flashcardsButton;
            case "quiz" -> quizButton;
            case "exams" -> examsButton;
            case "tools" -> studyToolsButton;
            default -> null;
        };
        if (targetButton != null) {
            targetButton.getStyleClass().add("tour-highlight");
        }
    }

    public void clearTourHighlight() {
        for (Button button : new Button[]{dashboardButton, subjectsButton, topicsButton,
                tasksButton, examsButton, studySessionsButton, flashcardsButton, quizButton,
                studyToolsButton}) {
            button.getStyleClass().remove("tour-highlight");
        }
    }

    private void loadView(String fxmlPath, Button activeButton, String title, String subtitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof DashboardController dashboardController) {
                dashboardController.setMainController(this);
            }

            view.setOpacity(0.0);
            contentArea.getChildren().setAll(view);
            markActive(activeButton);
            pageTitleLabel.setText(title);
            pageSubtitleLabel.setText(subtitle);

            FadeTransition transition = new FadeTransition(Duration.millis(180), view);
            transition.setFromValue(0.0);
            transition.setToValue(1.0);
            transition.play();
        } catch (IOException e) {
            showError("Unable to load view: " + e.getMessage());
        }
    }

    /** Highlights the sidebar item for the view currently on screen. */
    private void markActive(Button activeButton) {
        for (Button button : new Button[]{dashboardButton, subjectsButton, topicsButton,
                tasksButton, examsButton, studySessionsButton, flashcardsButton, quizButton,
                studyToolsButton}) {
            button.getStyleClass().remove("sidebar-button-active");
        }
        activeButton.getStyleClass().add("sidebar-button-active");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Could not open page");
        alert.showAndWait();
    }
}
