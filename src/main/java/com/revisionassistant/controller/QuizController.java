package com.revisionassistant.controller;

import com.revisionassistant.model.QuizAttempt;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.model.QuizQuestion;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.QuizService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

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
    private TableView<QuizQuestion> questionsTable;
    @FXML
    private TableColumn<QuizQuestion, String> questionColumn;
    @FXML
    private TableColumn<QuizQuestion, String> questionSubjectColumn;
    @FXML
    private TableColumn<QuizQuestion, String> questionTopicColumn;
    @FXML
    private TableColumn<QuizQuestion, String> correctColumn;

    // ----- Take Quiz tab ------------------------------------------------

    @FXML
    private ComboBox<Subject> quizSubjectComboBox;
    @FXML
    private ComboBox<Topic> quizTopicComboBox;
    @FXML
    private VBox questionsBox;
    @FXML
    private Button submitQuizButton;
    @FXML
    private Label quizResultLabel;

    @FXML
    private TableView<QuizAttempt> historyTable;
    @FXML
    private TableColumn<QuizAttempt, String> historyDateColumn;
    @FXML
    private TableColumn<QuizAttempt, String> historySubjectColumn;
    @FXML
    private TableColumn<QuizAttempt, String> historyTopicColumn;
    @FXML
    private TableColumn<QuizAttempt, String> historyScoreColumn;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final QuizService quizService = new QuizService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<QuizQuestion> questions = FXCollections.observableArrayList();
    private final ObservableList<QuizAttempt> attempts = FXCollections.observableArrayList();

    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

    private List<QuizQuestion> currentQuiz = List.of();
    private final Map<Integer, ToggleGroup> answerGroups = new LinkedHashMap<>();

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

        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(truncate(data.getValue().getQuestionText())));
        questionSubjectColumn.setCellValueFactory(data -> {
            Subject subject = subjectsById.get(data.getValue().getSubjectId());
            return new SimpleStringProperty(subject == null ? "—" : subject.getName());
        });
        questionTopicColumn.setCellValueFactory(data -> {
            Integer topicId = data.getValue().getTopicId();
            Topic topic = topicId == null ? null : topicsById.get(topicId);
            return new SimpleStringProperty(topic == null ? "—" : topic.getName());
        });
        correctColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCorrectOption().name()));

        questionsTable.setItems(questions);
        questionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                populateQuestionForm(newValue);
            }
        });
    }

    private void setUpQuizTab() {
        quizSubjectComboBox.setItems(subjects);
        quizSubjectComboBox.setConverter(subjectConverter(null, quizSubjectComboBox));
        quizSubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshTopicChoices(quizSubjectComboBox, quizTopicComboBox);
            quizTopicComboBox.getItems().add(0, null);
        });

        historyDateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAttemptDate().toString()));
        historySubjectColumn.setCellValueFactory(data -> {
            Subject subject = subjectsById.get(data.getValue().getSubjectId());
            return new SimpleStringProperty(subject == null ? "—" : subject.getName());
        });
        historyTopicColumn.setCellValueFactory(data -> {
            Integer topicId = data.getValue().getTopicId();
            Topic topic = topicId == null ? null : topicsById.get(topicId);
            return new SimpleStringProperty(topic == null ? "—" : topic.getName());
        });
        historyScoreColumn.setCellValueFactory(data -> {
            QuizAttempt attempt = data.getValue();
            return new SimpleStringProperty(attempt.getCorrectAnswers() + " / " + attempt.getTotalQuestions()
                    + " (" + attempt.getScorePercent() + "%)");
        });
        historyTable.setItems(attempts);

        submitQuizButton.setDisable(true);
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
    private void handleEditQuestion() {
        QuizQuestion selected = questionsTable.getSelectionModel().getSelectedItem();
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
        QuizQuestion selected = questionsTable.getSelectionModel().getSelectedItem();
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
        submitQuizButton.setDisable(false);
    }

    private void buildQuizQuestionPanels() {
        questionsBox.getChildren().clear();
        answerGroups.clear();

        int number = 1;
        for (QuizQuestion question : currentQuiz) {
            VBox panel = new VBox(6);
            panel.getStyleClass().add("dashboard-row");

            Label questionLabel = new Label(number + ". " + question.getQuestionText());
            questionLabel.getStyleClass().add("row-title");
            questionLabel.setWrapText(true);
            panel.getChildren().add(questionLabel);

            ToggleGroup toggleGroup = new ToggleGroup();
            answerGroups.put(question.getId(), toggleGroup);

            for (QuizOption option : QuizOption.values()) {
                RadioButton radioButton = new RadioButton(option.name() + ". " + question.getOptionText(option));
                radioButton.setUserData(option);
                radioButton.setToggleGroup(toggleGroup);
                radioButton.setWrapText(true);
                panel.getChildren().add(radioButton);
            }

            questionsBox.getChildren().add(panel);
            number++;
        }
    }

    @FXML
    private void handleSubmitQuiz() {
        if (currentQuiz.isEmpty()) {
            return;
        }

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
                    subject.getId(), topic == null ? null : topic.getId(), currentQuiz, answers);
            quizResultLabel.setText("You scored " + attempt.getCorrectAnswers() + " / "
                    + attempt.getTotalQuestions() + " (" + attempt.getScorePercent() + "%)");
            markAnswers();
            submitQuizButton.setDisable(true);
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

            for (int i = 1; i < panel.getChildren().size(); i++) {
                RadioButton radioButton = (RadioButton) panel.getChildren().get(i);
                radioButton.setDisable(true);
                QuizOption option = (QuizOption) radioButton.getUserData();
                if (option == question.getCorrectOption()) {
                    radioButton.getStyleClass().add("row-title");
                } else if (radioButton.equals(selected)) {
                    radioButton.getStyleClass().add("row-meta-warning");
                }
            }
            index++;
        }
    }

    private void refreshHistory() {
        try {
            attempts.setAll(quizService.getRecentAttempts(20));
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
        questionsTable.getSelectionModel().clearSelection();
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
        alert.showAndWait();
    }
}
