package com.revisionassistant.controller;

import com.revisionassistant.model.User;
import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.ReminderNotificationService;
import com.revisionassistant.service.UserService;
import com.revisionassistant.util.ThemeManager;
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
 * Application shell controller.
 * Handles sidebar navigation, profile information, notifications,
 * theme application, and the optional feature tour.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private StackPane tourOverlay;
    @FXML private StackPane notificationLayer;
    @FXML private Button notificationBellButton;
    @FXML private Label notificationBadge;

    private ReminderNotificationService reminderNotificationService;

    @FXML private Button dashboardButton;
    @FXML private Button subjectsButton;
    @FXML private Button topicsButton;
    @FXML private Button tasksButton;
    @FXML private Button examsButton;
    @FXML private Button studySessionsButton;
    @FXML private Button flashcardsButton;
    @FXML private Button quizButton;
    @FXML private Button studyToolsButton;
    @FXML private Button studyPathButton;
    @FXML private Button pomodoroButton;
    @FXML private Button mindMapButton;
    @FXML private Button resourcesButton;
    @FXML private Button settingsButton;

    @FXML
    public void initialize() {
        if (!CurrentUser.isLoggedIn()) {
            javafx.application.Platform.runLater(() -> {
                try {
                    AppNavigator.showLogin(
                            (javafx.stage.Stage) contentArea.getScene().getWindow()
                    );
                } catch (Exception ignored) {
                }
            });
            return;
        }

        updateProfile();

        if (sidebar != null && contentArea != null) {
            contentArea.sceneProperty().addListener((obs, oldScene, scene) -> {
                if (scene != null) {
                    updateResponsiveLayout(scene.getWidth());

                    scene.widthProperty().addListener((o, oldWidth, newWidth) ->
                            updateResponsiveLayout(newWidth.doubleValue()));
                }
            });
        }

        /*
         * IMPORTANT:
         * Do not auto-open the feature tour here.
         * The tour overlay covers the entire application and can block
         * mouse input from reaching the sidebar.
         */
        closeFeatureTour();

        showDashboard();

        javafx.application.Platform.runLater(() -> {
            if (contentArea != null && contentArea.getScene() != null) {
                ThemeManager.applyForCurrentUser(contentArea.getScene());
            }

            if (notificationLayer != null) {
                notificationLayer.setMouseTransparent(true);
                notificationLayer.setPickOnBounds(false);
            }

            if (tourOverlay != null) {
                tourOverlay.setVisible(false);
                tourOverlay.setManaged(false);
                tourOverlay.setMouseTransparent(true);
                tourOverlay.setPickOnBounds(false);
            }

            reminderNotificationService =
                    new ReminderNotificationService(notificationLayer, notificationBadge);

            reminderNotificationService.start();
        });
    }

    private void updateResponsiveLayout(double width) {
        if (sidebar == null) {
            return;
        }

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

    private void updateProfile() {
        User user = CurrentUser.get();

        if (user != null) {
            if (profileNameLabel != null) {
                profileNameLabel.setText(user.getName());
            }

            if (profileEmailLabel != null) {
                profileEmailLabel.setText(user.getEmail());
            }
        }
    }

    @FXML
    private void handleShowNotifications() {
        if (reminderNotificationService == null
                || reminderNotificationService.getHistory().isEmpty()) {

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "No new reminders. Selected reminder preferences will appear here while the app is open."
            );

            alert.setHeaderText("Notifications");
            com.revisionassistant.util.DialogStyler.style(alert);
            alert.showAndWait();
            return;
        }

        StringBuilder text = new StringBuilder();

        for (String item : reminderNotificationService.getHistory()) {
            text.append("• ")
                    .append(item)
                    .append("\n\n");
        }

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                text.toString()
        );

        alert.setHeaderText("Notifications");
        com.revisionassistant.util.DialogStyler.style(alert);
        alert.showAndWait();

        reminderNotificationService.markRead();
    }

    @FXML
    private void handleLogout() {
        if (reminderNotificationService != null) {
            reminderNotificationService.stop();
        }

        new UserService().logout();

        try {
            AppNavigator.showLogin(
                    (javafx.stage.Stage) contentArea.getScene().getWindow()
            );
        } catch (Exception e) {
            showError("Could not return to the login screen.");
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    @FXML
    void showDashboard() {
        loadView(
                "/com/revisionassistant/fxml/DashboardView.fxml",
                dashboardButton,
                "Dashboard",
                "A calm place to plan, practise, and review."
        );
    }

    @FXML
    private void showSubjects() {
        loadView(
                "/com/revisionassistant/fxml/SubjectView.fxml",
                subjectsButton,
                "Subjects",
                "Organise the areas you are currently studying."
        );
    }

    @FXML
    private void showTopics() {
        loadView(
                "/com/revisionassistant/fxml/TopicView.fxml",
                topicsButton,
                "Topics",
                "Build the topic structure behind your study plan."
        );
    }

    @FXML
    private void showTasks() {
        loadView(
                "/com/revisionassistant/fxml/TaskView.fxml",
                tasksButton,
                "Study Planner",
                "Turn your study goals into manageable tasks."
        );
    }

    @FXML
    private void showExams() {
        loadView(
                "/com/revisionassistant/fxml/ExamView.fxml",
                examsButton,
                "Exams",
                "Keep upcoming assessments and exam preparation together."
        );
    }

    @FXML
    private void showStudySessions() {
        loadView(
                "/com/revisionassistant/fxml/StudySessionView.fxml",
                studySessionsButton,
                "Study Sessions",
                "Record focused study time and review your sessions."
        );
    }

    @FXML
    private void showFlashcards() {
        loadView(
                "/com/revisionassistant/fxml/FlashcardView.fxml",
                flashcardsButton,
                "Flashcards",
                "Practise active recall with your saved cards."
        );
    }

    @FXML
    private void showQuiz() {
        loadView(
                "/com/revisionassistant/fxml/QuizView.fxml",
                quizButton,
                "Quiz",
                "Test your understanding and revisit difficult questions."
        );
    }

    @FXML
    private void showStudyTools() {
        loadView(
                "/com/revisionassistant/fxml/StudyToolsView.fxml",
                studyToolsButton,
                "Study Tools",
                "Topic dependencies and study planner for smarter revision."
        );
    }

    @FXML
    private void showStudyPath() {
        loadView(
                "/com/revisionassistant/fxml/StudyPathView.fxml",
                studyPathButton,
                "My Study Path",
                "Your personalised learning journey toward your exams."
        );
    }

    @FXML
    private void showPomodoro() {
        loadView(
                "/com/revisionassistant/fxml/PomodoroView.fxml",
                pomodoroButton,
                "Pomodoro Timer",
                "Stay focused with structured study and break intervals."
        );
    }

    @FXML
    private void showMindMap() {
        loadView(
                "/com/revisionassistant/fxml/MindMapView.fxml",
                mindMapButton,
                "Mind Map",
                "Organise concepts visually around a central topic."
        );
    }

    @FXML
    private void showResources() {
        loadView(
                "/com/revisionassistant/fxml/ResourcesView.fxml",
                resourcesButton,
                "Resources",
                "Save and organise your favourite study links."
        );
    }

    @FXML
    private void showSettings() {
        loadView(
                "/com/revisionassistant/fxml/SettingsView.fxml",
                settingsButton,
                "Settings",
                "Customise your Revision Assistant experience."
        );
    }

    // -------------------------------------------------------------------------
    // Dashboard callbacks
    // -------------------------------------------------------------------------

    public void showSubjectsFromDashboard() {
        showSubjects();
    }

    public void showTopicsFromDashboard() {
        showTopics();
    }

    public void showTasksFromDashboard() {
        showTasks();
    }

    public void showExamsFromDashboard() {
        showExams();
    }

    public void showFlashcardsFromDashboard() {
        showFlashcards();
    }

    public void showQuizFromDashboard() {
        showQuiz();
    }

    public void showStudyPathFromDashboard() {
        showStudyPath();
    }

    // -------------------------------------------------------------------------
    // Feature tour
    // -------------------------------------------------------------------------

    @FXML
    private void handleReplayTour() {
        showFeatureTour();
    }

    private void showFeatureTour() {
        if (tourOverlay == null) {
            return;
        }

        if (!tourOverlay.getChildren().isEmpty()) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/revisionassistant/fxml/FeatureTourView.fxml"
                    )
            );

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

            FadeTransition transition =
                    new FadeTransition(Duration.millis(180), tour);

            transition.setFromValue(0.0);
            transition.setToValue(1.0);
            transition.play();

        } catch (IOException e) {
            showError("The feature tour could not be opened.");
        }
    }

    public void closeFeatureTour() {
        if (tourOverlay == null) {
            return;
        }

        tourOverlay.getChildren().clear();
        tourOverlay.setMouseTransparent(true);
        tourOverlay.setVisible(false);
        tourOverlay.setManaged(false);
        tourOverlay.setPickOnBounds(false);

        clearTourHighlight();
    }

    public void highlightTourTarget(String target) {
        clearTourHighlight();

        Button targetButton = target == null
                ? null
                : switch (target) {
            case "dashboard" -> dashboardButton;
            case "subjects" -> subjectsButton;
            case "topics" -> topicsButton;
            case "planner" -> tasksButton;
            case "flashcards" -> flashcardsButton;
            case "quiz" -> quizButton;
            case "exams" -> examsButton;
            case "tools" -> studyToolsButton;
            case "path" -> studyPathButton;
            default -> null;
        };

        if (targetButton != null) {
            targetButton.getStyleClass().add("tour-highlight");
        }
    }

    public void clearTourHighlight() {
        Button[] buttons = {
                dashboardButton,
                subjectsButton,
                topicsButton,
                tasksButton,
                examsButton,
                studySessionsButton,
                flashcardsButton,
                quizButton,
                studyToolsButton,
                studyPathButton,
                pomodoroButton,
                mindMapButton,
                resourcesButton,
                settingsButton
        };

        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("tour-highlight");
            }
        }
    }

    // -------------------------------------------------------------------------
    // View loading
    // -------------------------------------------------------------------------

    private void loadView(
            String fxmlPath,
            Button activeButton,
            String title,
            String subtitle
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Parent view = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardController dc) {
                dc.setMainController(this);
            }

            view.setOpacity(0.0);

            contentArea.getChildren().setAll(view);

            markActive(activeButton);

            pageTitleLabel.setText(title);
            pageSubtitleLabel.setText(subtitle);

            FadeTransition transition =
                    new FadeTransition(Duration.millis(180), view);

            transition.setFromValue(0.0);
            transition.setToValue(1.0);
            transition.play();

        } catch (IOException e) {
            showError("Unable to load view: " + e.getMessage());
        }
    }

    private void markActive(Button activeButton) {
        Button[] buttons = {
                dashboardButton,
                subjectsButton,
                topicsButton,
                tasksButton,
                examsButton,
                studySessionsButton,
                flashcardsButton,
                quizButton,
                studyToolsButton,
                studyPathButton,
                pomodoroButton,
                mindMapButton,
                resourcesButton,
                settingsButton
        };

        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("sidebar-button-active");
            }
        }

        if (activeButton != null) {
            activeButton.getStyleClass().add("sidebar-button-active");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                message
        );

        alert.setHeaderText("Could not open page");
        com.revisionassistant.util.DialogStyler.style(alert);
        alert.showAndWait();
    }
}