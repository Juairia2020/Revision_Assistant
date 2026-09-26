package com.revisionassistant.controller;

import com.revisionassistant.dto.ImportedQuizQuestionDTO;
import com.revisionassistant.dto.QuizImportDTO;
import com.revisionassistant.model.QuizAttempt;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.model.QuizQuestion;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.JsonImportException;
import com.revisionassistant.util.DialogStyler;
import com.revisionassistant.service.JsonImportService;
import com.revisionassistant.service.QuizService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.scene.layout.Region;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for QuizView.fxml. Handles UI events for both managing
 * MCQs and taking a quiz - all validation, scoring and persistence
 * goes through {@link QuizService}, {@link SubjectService} and
 * {@link TopicService}.
 */
public class QuizController {

    // ----- Manage Questions tab ---------------------------------------

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<Topic> topicComboBox;
    @FXML
    private TextArea questionTextArea;
    @FXML
    private TextField optionAField;
    @FXML
    private TextField optionBField;
    @FXML
    private TextField optionCField;
    @FXML
    private TextField optionDField;
    @FXML
    private ComboBox<QuizOption> correctOptionComboBox;

    @FXML
    private ComboBox<Subject> filterSubjectComboBox;

    @FXML
    private FlowPane questionCardsPane;

    // ----- Take Quiz tab ------------------------------------------------

    @FXML
    private ComboBox<Subject> quizSubjectComboBox;
    @FXML
    private ComboBox<Topic> quizTopicComboBox;
    @FXML
    private ComboBox<Integer> quizTimeLimitComboBox;
    @FXML
    private VBox questionsBox;
    @FXML
    private Button submitQuizButton;
    @FXML
    private Label quizResultLabel;
    @FXML
    private ProgressBar quizProgressBar;
    @FXML
    private Label quizProgressText;
    @FXML
    private Label quizTimerLabel;

    @FXML
    private VBox historyBox;

    private QuizQuestion selectedQuestion;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final QuizService quizService = new QuizService();
    private final JsonImportService jsonImportService = new JsonImportService();

    @FXML
    private Button importJsonButton;
    @FXML
    private ProgressIndicator importProgressIndicator;
    @FXML
    private Label importStatusLabel;

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<QuizQuestion> questions = FXCollections.observableArrayList();
    private final ObservableList<QuizAttempt> attempts = FXCollections.observableArrayList();

    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

    private List<QuizQuestion> currentQuiz = List.of();
    private final Map<Integer, ToggleGroup> answerGroups = new LinkedHashMap<>();

    /** Ticks once a second while a quiz is in progress; stopped on submit. */
    private Timeline quizTimer;
    /** How long the current quiz has been running, in seconds. Recorded with the attempt on submit. */
    private int quizElapsedSeconds;
    /** The chosen time limit in seconds for the current quiz, or 0 for no limit. */
    private int quizTimeLimitSeconds;

    @FXML
    public void initialize() {
        setUpManageTab();
        setUpQuizTab();
        refreshSubjects();
        refreshQuestions();
        refreshHistory();
    }

    // ----- Setup --------------------------------------------------------

    private void setUpManageTab() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(subjectConverter(null, subjectComboBox));
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
                refreshTopicChoices(subjectComboBox, topicComboBox));
        correctOptionComboBox.setItems(FXCollections.observableArrayList(QuizOption.values()));
        correctOptionComboBox.setValue(QuizOption.A);
        ObservableList<Subject> filterSubjects = FXCollections.observableArrayList();
        filterSubjects.add(null);
        filterSubjectComboBox.setItems(filterSubjects);
        filterSubjectComboBox.setConverter(subjectConverter("All subjects", filterSubjectComboBox));
        filterSubjectComboBox.setValue(null);
        filterSubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyQuestionFilter());
        questionCardsPane.setHgap(14);
        questionCardsPane.setVgap(14);
        questionCardsPane.widthProperty().addListener((obs, oldValue, newValue) ->
                questionCardsPane.setPrefWrapLength(Math.max(420, newValue.doubleValue() - 24)));
    }

    private void rebuildQuestionCards() {
        questionCardsPane.getChildren().clear();
        for (QuizQuestion question : questions) {
            VBox card = new VBox(8);
            card.getStyleClass().add("question-library-card");
            card.setPrefWidth(310);
            Label title = new Label(truncate(question.getQuestionText()));
            title.setWrapText(true);
            title.getStyleClass().add("row-title");
            Label answer = new Label("Correct: " + question.getCorrectOption().name());
            answer.getStyleClass().add("badge-success");
            Subject subject = subjectsById.get(question.getSubjectId());
            Label meta = new Label(subject == null ? "No subject" : subject.getName());
            meta.getStyleClass().add("row-meta");
            card.getChildren().addAll(title, answer, meta);
            card.setOnMouseClicked(e -> { selectedQuestion = question; populateQuestionForm(question); selectQuestionCard(question); });
            questionCardsPane.getChildren().add(card);
        }
    }

    private void selectQuestionCard(QuizQuestion question) {
        for (int i = 0; i < questions.size(); i++) {
            questionCardsPane.getChildren().get(i).pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), questions.get(i).equals(question));
        }
    }

    private void setUpQuizTab() {
        quizSubjectComboBox.setItems(subjects);
        quizSubjectComboBox.setConverter(subjectConverter(null, quizSubjectComboBox));
        quizSubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshTopicChoices(quizSubjectComboBox, quizTopicComboBox);
            if (!quizTopicComboBox.getItems().contains(null)) quizTopicComboBox.getItems().add(0, null);
        });

        ObservableList<Integer> timeLimits = FXCollections.observableArrayList();
        timeLimits.addAll(null, 2, 5, 10, 15, 20);
        quizTimeLimitComboBox.setItems(timeLimits);
        quizTimeLimitComboBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer minutes) {
                return minutes == null ? "No time limit" : minutes + " min limit";
            }

            @Override
            public Integer fromString(String string) {
                return quizTimeLimitComboBox.getValue();
            }
        });
        quizTimeLimitComboBox.setValue(null);

        quizTimerLabel.setVisible(false);
        quizTimerLabel.setManaged(false);

        quizProgressBar.setProgress(0);
        quizProgressText.setText("0 questions");
        submitQuizButton.setDisable(true);
    }

    private void rebuildHistoryCards() {
        historyBox.getChildren().clear();
        for (QuizAttempt attempt : attempts) {
            HBox row = new HBox(14);
            row.getStyleClass().add("history-card");
            Label score = new Label(attempt.getCorrectAnswers() + "/" + attempt.getTotalQuestions());
            score.getStyleClass().add("history-score");
            String timePart = attempt.getTimeTakenSeconds() > 0 ? "  •  " + formatDuration(attempt.getTimeTakenSeconds()) : "";
            Label details = new Label(attempt.getAttemptDate() + "  •  " +
                    (subjectsById.get(attempt.getSubjectId()) == null ? "Subject" : subjectsById.get(attempt.getSubjectId()).getName()) +
                    "  •  " + attempt.getScorePercent() + "%" + timePart);
            details.getStyleClass().add("row-meta");
            row.getChildren().addAll(score, details);
            historyBox.getChildren().add(row);
        }
    }

    // ----- Manage Questions handlers ------------------------------------

    private void populateQuestionForm(QuizQuestion question) {
        subjectComboBox.setValue(subjectsById.get(question.getSubjectId()));
        refreshTopicChoices(subjectComboBox, topicComboBox);
        Integer topicId = question.getTopicId();
        topicComboBox.setValue(topicId == null ? null : topicsById.get(topicId));
        questionTextArea.setText(question.getQuestionText());
        optionAField.setText(question.getOptionA());
        optionBField.setText(question.getOptionB());
        optionCField.setText(question.getOptionC());
        optionDField.setText(question.getOptionD());
        correctOptionComboBox.setValue(question.getCorrectOption());
    }

    @FXML
    private void handleAddQuestion() {
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            quizService.addQuestion(
                    subject == null ? 0 : subject.getId(),
                    topic == null ? null : topic.getId(),
                    questionTextArea.getText(),
                    optionAField.getText(), optionBField.getText(), optionCField.getText(), optionDField.getText(),
                    correctOptionComboBox.getValue());
            clearQuestionForm();
            refreshQuestions();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add question", e.getMessage());
        }
    }

    @FXML
    private void handleImportJson() {
        Subject subject = subjectComboBox.getValue();
        if (subject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected",
                    "Choose a subject before importing JSON.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import quiz questions from JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        java.io.File file = chooser.showOpenDialog(questionCardsPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        importJsonButton.setDisable(true);
        importProgressIndicator.setVisible(true);
        importStatusLabel.setText("Reading and validating JSON…");

        final Task<QuizImportDTO> task = new Task<>() {
            @Override
            protected QuizImportDTO call() throws Exception {
                if (isCancelled()) {
                    return null;
                }
                return jsonImportService.readQuiz(file);
            }
        };

        task.setOnSucceeded(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");

            QuizImportDTO document = task.getValue();
            if (document == null) {
                return;
            }

            Topic topic;
            try {
                topic = resolveImportedTopic(subject, document.getTopic());
            } catch (IllegalArgumentException | SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid topic", e.getMessage());
                return;
            }

            List<ImportedQuizQuestionDTO> accepted =
                    showImportedQuestionsDialog(document.getQuestions());
            if (accepted == null || accepted.isEmpty()) {
                return;
            }

            int saved = 0;
            for (ImportedQuizQuestionDTO question : accepted) {
                try {
                    quizService.addQuestion(subject.getId(), topic.getId(),
                            question.getQuestion(), question.getOptionA(), question.getOptionB(),
                            question.getOptionC(), question.getOptionD(),
                            QuizOption.fromString(question.getCorrectOptionLetter()));
                    saved++;
                } catch (IllegalArgumentException | SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Could not save imported question", e.getMessage());
                    break;
                }
            }
            refreshQuestions();
            if (saved > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Import complete",
                        saved + " question(s) were added.");
            }
        });

        task.setOnFailed(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");
            Throwable error = task.getException();
            String message = error instanceof JsonImportException
                    ? error.getMessage()
                    : "Could not process the JSON file.";
            showAlert(Alert.AlertType.ERROR, "Invalid quiz JSON", message);
        });

        task.setOnCancelled(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");
        });

        Thread worker = new Thread(task, "quiz-json-import");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleCopyJsonPrompt() {
        ClipboardContent content = new ClipboardContent();
        content.putString(JsonImportService.quizPrompt());
        Clipboard.getSystemClipboard().setContent(content);
        showAlert(Alert.AlertType.INFORMATION, "Prompt copied",
                "The quiz JSON prompt was copied to the clipboard.");
    }

    private Topic resolveImportedTopic(Subject subject, String topicName) throws SQLException {
        String imported = topicName == null ? "" : topicName.trim();
        Topic selected = topicComboBox.getValue();
        if (selected != null && !selected.getName().equalsIgnoreCase(imported)) {
            throw new IllegalArgumentException(
                    "The JSON topic \"" + imported + "\" does not match the selected topic \""
                            + selected.getName() + "\".");
        }
        for (Topic topic : topicService.getTopicsForSubject(subject.getId())) {
            if (topic.getName().equalsIgnoreCase(imported)) {
                return topic;
            }
        }
        throw new IllegalArgumentException(
                "Topic \"" + imported + "\" does not exist for the selected subject.");
    }

    private List<ImportedQuizQuestionDTO> showImportedQuestionsDialog(
            List<ImportedQuizQuestionDTO> imported) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/revisionassistant/fxml/ImportedQuestionsDialog.fxml"));
            Parent root = loader.load();
            ImportedQuestionsDialogController controller = loader.getController();
            controller.setQuestions(imported);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Preview imported quiz questions");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            return controller.isConfirmed() ? controller.getSelectedQuestions() : null;
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Could not open preview",
                    "Could not show the imported questions: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void handleEditQuestion() {
        QuizQuestion selected = selectedQuestion;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No question selected", "Select a question to edit first.");
            return;
        }
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTopicId(topic == null ? null : topic.getId());
            selected.setQuestionText(questionTextArea.getText());
            selected.setOptionA(optionAField.getText());
            selected.setOptionB(optionBField.getText());
            selected.setOptionC(optionCField.getText());
            selected.setOptionD(optionDField.getText());
            selected.setCorrectOption(correctOptionComboBox.getValue());
            quizService.updateQuestion(selected);
            refreshQuestions();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update question", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteQuestion() {
        QuizQuestion selected = selectedQuestion;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No question selected", "Select a question to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this question?");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            quizService.deleteQuestion(selected.getId());
            clearQuestionForm();
            refreshQuestions();
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Cannot delete question", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleClearQuestionFilter() {
        filterSubjectComboBox.setValue(null);
    }

    private void applyQuestionFilter() {
        Subject subject = filterSubjectComboBox.getValue();
        try {
            List<QuizQuestion> filtered = quizService.getFilteredQuestions(
                    subject == null ? null : subject.getId(), null);
            questions.setAll(filtered);
            rebuildQuestionCards();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load questions: " + e.getMessage());
        }
    }

    // ----- Take Quiz handlers --------------------------------------------

    @FXML
    private void handleStartQuiz() {
        Subject subject = quizSubjectComboBox.getValue();
        if (subject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Choose a subject to start a quiz.");
            return;
        }
        Topic topic = quizTopicComboBox.getValue();

        try {
            currentQuiz = quizService.getQuestionsForQuiz(subject.getId(), topic == null ? null : topic.getId());
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not start quiz", e.getMessage());
            return;
        }

        if (currentQuiz.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No questions available",
                    "There are no quiz questions stored for this selection yet. Add some in the Manage Questions tab.");
            return;
        }

        buildQuizQuestionPanels();
        quizResultLabel.setText("");
        quizProgressBar.setProgress(1.0);
        quizProgressText.setText(currentQuiz.size() + (currentQuiz.size() == 1 ? " question" : " questions"));
        submitQuizButton.setDisable(false);
        startQuizTimer();
    }

    /** Starts (or restarts) the per-second timer for the quiz just launched. */
    private void startQuizTimer() {
        stopQuizTimer();
        Integer limitMinutes = quizTimeLimitComboBox.getValue();
        quizTimeLimitSeconds = limitMinutes == null ? 0 : limitMinutes * 60;
        quizElapsedSeconds = 0;
        quizTimerLabel.setVisible(true);
        quizTimerLabel.setManaged(true);
        updateQuizTimerLabel();

        quizTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickQuizTimer()));
        quizTimer.setCycleCount(Timeline.INDEFINITE);
        quizTimer.play();
    }

    private void tickQuizTimer() {
        quizElapsedSeconds++;
        updateQuizTimerLabel();
        if (quizTimeLimitSeconds > 0 && quizElapsedSeconds >= quizTimeLimitSeconds) {
            autoSubmitQuiz();
        }
    }

    private void updateQuizTimerLabel() {
        if (quizTimeLimitSeconds > 0) {
            int remaining = Math.max(0, quizTimeLimitSeconds - quizElapsedSeconds);
            quizTimerLabel.setText(formatDuration(remaining) + " left");
        } else {
            quizTimerLabel.setText(formatDuration(quizElapsedSeconds));
        }
    }

    private void stopQuizTimer() {
        if (quizTimer != null) {
            quizTimer.stop();
        }
    }

    /** Renders a duration in seconds as "M:SS" (or "H:MM:SS" past one hour). */
    private String formatDuration(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }

    private void buildQuizQuestionPanels() {
        questionsBox.getChildren().clear();
        answerGroups.clear();

        int number = 1;
        for (QuizQuestion question : currentQuiz) {
            VBox panel = new VBox(10);
            panel.getStyleClass().add("quiz-question-card");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label numberBadge = new Label(String.valueOf(number));
            numberBadge.getStyleClass().add("quiz-question-number");
            VBox headerText = new VBox(2);
            Label eyebrow = new Label("QUESTION " + number);
            eyebrow.getStyleClass().add("quiz-question-topic");
            Label prompt = new Label("Choose one answer");
            prompt.getStyleClass().add("row-meta");
            Label count = new Label(number + " / " + currentQuiz.size());
            count.getStyleClass().add("quiz-question-count");
            headerText.getChildren().addAll(eyebrow, prompt);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(numberBadge, headerText, spacer, count);

            Label questionLabel = new Label(question.getQuestionText());
            questionLabel.getStyleClass().add("quiz-question-text");
            questionLabel.setWrapText(true);

            VBox optionsBox = new VBox(10);
            ToggleGroup toggleGroup = new ToggleGroup();
            answerGroups.put(question.getId(), toggleGroup);
            for (QuizOption option : QuizOption.values()) {
                ToggleButton choice = new ToggleButton();
                choice.setUserData(option);
                choice.setToggleGroup(toggleGroup);
                choice.setMaxWidth(Double.MAX_VALUE);
                choice.setMinHeight(50);
                choice.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                choice.getStyleClass().add("quiz-option");

                Label letter = new Label(option.name());
                letter.getStyleClass().add("quiz-option-letter");
                Label answerText = new Label(question.getOptionText(option));
                answerText.setWrapText(true);
                answerText.getStyleClass().add("quiz-option-text");
                HBox graphic = new HBox(12, letter, answerText);
                graphic.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(answerText, Priority.ALWAYS);
                choice.setGraphic(graphic);
                optionsBox.getChildren().add(choice);
            }

            Label feedback = new Label("");
            feedback.setWrapText(true);
            feedback.getStyleClass().add("quiz-answer-note");
            feedback.setVisible(false);
            feedback.setManaged(false);

            panel.getChildren().addAll(header, questionLabel, optionsBox, feedback);
            questionsBox.getChildren().add(panel);
            number++;
        }
    }

    @FXML
    private void handleSubmitQuiz() {
        submitQuiz(false);
    }

    /** Called by the timer itself when a time-limited quiz runs out. */
    private void autoSubmitQuiz() {
        submitQuiz(true);
    }

    /**
     * Shared submit path for both a manual submit and an automatic one
     * triggered by the time limit running out. When {@code allowUnanswered}
     * is true, questions left blank are simply scored as incorrect instead
     * of blocking the submission.
     */
    private void submitQuiz(boolean allowUnanswered) {
        if (currentQuiz.isEmpty()) {
            return;
        }
        stopQuizTimer();

        Map<Integer, QuizOption> answers = new LinkedHashMap<>();
        for (Map.Entry<Integer, ToggleGroup> entry : answerGroups.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();
            if (selected != null) {
                answers.put(entry.getKey(), (QuizOption) selected.getUserData());
            }
        }

        Subject subject = quizSubjectComboBox.getValue();
        Topic topic = quizTopicComboBox.getValue();

        try {
            QuizAttempt attempt = quizService.submitAttempt(
                    subject.getId(), topic == null ? null : topic.getId(), currentQuiz, answers,
                    quizElapsedSeconds, allowUnanswered);
            String prefix = allowUnanswered ? "Time's up! You scored " : "You scored ";
            quizResultLabel.setText(prefix + attempt.getCorrectAnswers() + " / "
                    + attempt.getTotalQuestions() + " (" + attempt.getScorePercent() + "%)");
            markAnswers();
            submitQuizButton.setDisable(true);
            quizTimerLabel.setVisible(false);
            quizTimerLabel.setManaged(false);
            refreshHistory();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not submit quiz", e.getMessage());
        }
    }

    /** After submitting, disables the answer options and highlights correct/incorrect choices. */
    private void markAnswers() {
        int index = 0;
        for (QuizQuestion question : currentQuiz) {
            VBox panel = (VBox) questionsBox.getChildren().get(index);
            ToggleGroup toggleGroup = answerGroups.get(question.getId());
            Toggle selected = toggleGroup == null ? null : toggleGroup.getSelectedToggle();
            Label feedback = (Label) panel.getChildren().get(panel.getChildren().size() - 1);
            boolean answeredCorrectly = false;

            Node optionsNode = panel.getChildren().get(2);
            if (optionsNode instanceof VBox optionsBox) {
                for (Node n : optionsBox.getChildren()) {
                    ToggleButton choice = (ToggleButton) n;
                    choice.setDisable(true);
                    QuizOption option = (QuizOption) choice.getUserData();
                    if (option == question.getCorrectOption()) {
                        choice.getStyleClass().add("quiz-correct-option");
                        if (choice.equals(selected)) answeredCorrectly = true;
                    } else if (choice.equals(selected)) {
                        choice.getStyleClass().add("quiz-wrong-option");
                    }
                }
            }
            feedback.setVisible(true);
            feedback.setManaged(true);
            feedback.setText(answeredCorrectly
                    ? "✓ Correct answer"
                    : "Correct answer: " + question.getCorrectOption().name() + ". " + question.getOptionText(question.getCorrectOption()));
            index++;
        }
    }

    private void refreshHistory() {
        try {
            attempts.setAll(quizService.getRecentAttempts(20));
            rebuildHistoryCards();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load quiz history: " + e.getMessage());
        }
    }

    // ----- Shared helpers -------------------------------------------------

    private void refreshSubjects() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            subjects.setAll(all);
            subjectsById.clear();
            for (Subject subject : all) {
                subjectsById.put(subject.getId(), subject);
            }

            filterSubjectComboBox.getItems().setAll(subjects);
            filterSubjectComboBox.getItems().add(0, null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshTopicChoices(ComboBox<Subject> subjectBox, ComboBox<Topic> topicBox) {
        Subject subject = subjectBox.getValue();
        ObservableList<Topic> topics = FXCollections.observableArrayList();
        topics.add(null);
        if (subject != null) {
            try {
                topics.addAll(topicService.getTopicsForSubject(subject.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
            }
        }
        topicBox.setItems(topics);
        topicBox.setConverter(new StringConverter<Topic>() {
            @Override
            public String toString(Topic topic) {
                return topic == null ? "All topics" : topic.getName();
            }

            @Override
            public Topic fromString(String string) {
                return topicBox.getValue();
            }
        });
    }

    private void refreshQuestions() {
        rebuildTopicIndex();
        applyQuestionFilter();
    }

    private void rebuildTopicIndex() {
        topicsById.clear();
        try {
            for (Subject subject : subjects) {
                List<Topic> topics = topicService.getTopicsForSubject(subject.getId());
                for (Topic topic : topics) {
                    topicsById.put(topic.getId(), topic);
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
        }
    }

    private void clearQuestionForm() {
        subjectComboBox.setValue(null);
        refreshTopicChoices(subjectComboBox, topicComboBox);
        questionTextArea.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        correctOptionComboBox.setValue(QuizOption.A);
        selectedQuestion = null;
        questionCardsPane.getChildren().forEach(n -> n.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), false));
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replace("\n", " ");
        return singleLine.length() > 60 ? singleLine.substring(0, 57) + "..." : singleLine;
    }

    private StringConverter<Subject> subjectConverter(String nullLabel, ComboBox<Subject> box) {
        return new StringConverter<Subject>() {
            @Override
            public String toString(Subject subject) {
                if (subject == null) {
                    return nullLabel == null ? "" : nullLabel;
                }
                return subject.getName();
            }

            @Override
            public Subject fromString(String string) {
                return box.getValue();
            }
        };
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        DialogStyler.style(alert);
        alert.showAndWait();
    }
}