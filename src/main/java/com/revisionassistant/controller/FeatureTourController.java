package com.revisionassistant.controller;

import com.revisionassistant.service.UserService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;

/** Reusable first-launch and replayable feature tour. */
public class FeatureTourController {
    private static final int LAST_STEP = 9;

    @FXML private Label stepLabel;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label progressLabel;
    @FXML private VBox tourCard;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button skipButton;
    @FXML private Button finishButton;

    private MainController mainController;
    private int step = 1;

    private final UserService userService = new UserService();

    private final TourStep[] steps = {
            new TourStep("Welcome", "Welcome to Revision Assistant", "Let's take a quick tour of your new study workspace.", null),
            new TourStep("Dashboard", "Your study overview", "See overall study progress, important statistics, upcoming exams, study activity, and quick actions in one place.", "dashboard"),
            new TourStep("Subjects & Topics", "Organise what you study", "Create subjects for your courses, break them into topics, and track topic completion as you revise.", "subjects"),
            new TourStep("Study Planner", "Turn goals into a plan", "Use the planner to organise revision tasks and make use of the application's planning algorithms.", "planner"),
            new TourStep("Flashcards", "Practise active recall", "Review saved flashcards, including cards created from imported study material, to reinforce what you learn.", "flashcards"),
            new TourStep("Quiz", "Check your understanding", "Take quizzes, see how you perform, and revisit questions that are difficult.", "quiz"),
            new TourStep("Exams", "Keep assessment preparation visible", "Create and manage exams, track preparation, and monitor progress toward upcoming assessments.", "exams"),
            new TourStep("AI / JSON Import", "Bring in study material from any AI tool", "Study material → Any AI assistant → Generate required JSON → Download .json → Import → Preview → Confirm → Flashcards / Quiz. You may use any AI you prefer; Revision Assistant does not require an AI API key for this workflow.", "tools"),
            new TourStep("Finish", "You're ready!", "Start organising your study material and make Revision Assistant your personal study workspace.", null)
    };

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        renderStep();
    }

    @FXML
    private void handleNext() {
        if (step < LAST_STEP) {
            step++;
            renderStep();
        }
    }

    @FXML
    private void handlePrevious() {
        if (step > 1) {
            step--;
            renderStep();
        }
    }

    @FXML
    private void handleSkip() {
        completeTour();
    }

    @FXML
    private void handleFinish() {
        completeTour();
    }

    private void renderStep() {
        TourStep current = steps[step - 1];
        stepLabel.setText(current.section());
        titleLabel.setText(current.title());
        descriptionLabel.setText(current.description());
        progressLabel.setText("Step " + step + " of " + LAST_STEP);
        previousButton.setDisable(step == 1);
        nextButton.setVisible(step < LAST_STEP);
        nextButton.setManaged(step < LAST_STEP);
        finishButton.setVisible(step == LAST_STEP);
        finishButton.setManaged(step == LAST_STEP);

        if (mainController != null) {
            mainController.highlightTourTarget(current.target());
        }

        FadeTransition fade = new FadeTransition(Duration.millis(120), tourCard);
        fade.setFromValue(0.78);
        fade.setToValue(1.0);
        fade.play();
    }

    private void completeTour() {
        try {
            userService.markOnboardingCompleted();
        } catch (SQLException ignored) {
            // The tour can still close; a later replay remains available.
        }
        if (mainController != null) {
            mainController.closeFeatureTour();
        }
    }

    private record TourStep(String section, String title, String description, String target) {
    }
}
