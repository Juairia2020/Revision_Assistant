package com.revisionassistant.controller;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.ExamService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import com.revisionassistant.util.DialogStyler;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exam management with topic-driven preparation progress. */
public class ExamController {

    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private TextField titleField;
    @FXML private DatePicker examDatePicker;
    @FXML private ListView<Topic> topicSelectionList;
    @FXML private Label selectedTopicCountLabel;
    @FXML private ListView<Exam> examsList;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final ExamService examService = new ExamService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Topic> availableTopics = FXCollections.observableArrayList();
    private final ObservableList<Exam> exams = FXCollections.observableArrayList();
    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Set<Integer> selectedTopicIds = new HashSet<>();

    @FXML
    public void initialize() {
        setupSubjectCombo();
        setupTopicSelection();
        setupExamList();
        refreshSubjects();
        refreshExams();
        updateSelectedTopicCount();
    }

    private void setupSubjectCombo() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(new StringConverter<Subject>() {
            @Override public String toString(Subject subject) {
                return subject == null ? "" : subject.getName();
            }
            @Override public Subject fromString(String string) {
                return subjectComboBox.getValue();
            }
        });
        subjectComboBox.valueProperty().addListener((obs, old, selected) -> refreshAvailableTopics());
    }

    private void setupTopicSelection() {
        topicSelectionList.setItems(availableTopics);
        topicSelectionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Topic topic, boolean empty) {
                super.updateItem(topic, empty);
                if (empty || topic == null) {
                    setGraphic(null);
                    return;
                }

                CheckBox check = new CheckBox();
                check.setSelected(selectedTopicIds.contains(topic.getId()));
                check.setOnAction(e -> {
                    if (check.isSelected()) selectedTopicIds.add(topic.getId());
                    else selectedTopicIds.remove(topic.getId());
                    updateSelectedTopicCount();
                });

                Label name = new Label(topic.getName());
                name.getStyleClass().add("row-title");
                Label state = new Label(topic.isCompleted() ? "Completed" : "Not completed");
                state.getStyleClass().add(topic.isCompleted() ? "badge-success" : "badge-accent");

                HBox row = new HBox(10, check, name, state);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });
    }

    private void setupExamList() {
        examsList.setItems(exams);
        examsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Exam exam, boolean empty) {
                super.updateItem(exam, empty);
                if (empty || exam == null) {
                    setGraphic(null);
                    return;
                }

                VBox card = new VBox(10);
                card.getStyleClass().add("mission-card");
                Subject subject = subjectsById.get(exam.getSubjectId());
                long days = exam.getDaysRemaining();
                boolean past = days < 0;
                boolean today = days == 0;

                Label title = new Label(exam.getTitle());
                title.getStyleClass().add("card-title");
                Label meta = new Label((subject == null ? "No subject" : subject.getName())
                        + "  •  " + exam.getExamDate());
                meta.getStyleClass().add("row-meta");

                HBox badges = new HBox(8);
                Label dateBadge = new Label(past ? "Past" : today ? "Exam Today" : days + " days remaining");
                dateBadge.getStyleClass().add(today ? "badge-danger" : (days <= 2 && days >= 0 ? "badge-warning" : "badge-accent"));
                badges.getChildren().add(dateBadge);

                int progress = exam.getProgress();
                Label progressLabel = new Label(progress + "% syllabus complete");
                progressLabel.getStyleClass().add("row-meta");
                ProgressBar bar = new ProgressBar(progress / 100.0);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.getStyleClass().add("path-subject-bar");

                HBox row = new HBox(12, title, new StackPane());
                HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);

                card.getChildren().addAll(row, meta, badges, progressLabel, bar);
                FadeTransition fade = new FadeTransition(Duration.millis(180), card);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
                setGraphic(card);
            }
        });

        examsList.getSelectionModel().selectedItemProperty().addListener((obs, old, exam) -> {
            if (exam == null) return;
            subjectComboBox.setValue(subjectsById.get(exam.getSubjectId()));
            titleField.setText(exam.getTitle());
            examDatePicker.setValue(exam.getExamDate());
            loadExamTopics(exam);
        });
    }

    @FXML
    private void handleAddExam() {
        try {
            Subject subject = subjectComboBox.getValue();
            Exam exam = examService.addExam(
                    subject == null ? 0 : subject.getId(),
                    titleField.getText(),
                    examDatePicker.getValue(),
                    selectedTopicIds.stream().sorted().toList());
            clearForm();
            refreshExams();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add exam", e.getMessage());
        }
    }

    @FXML
    private void handleEditExam() {
        Exam selected = examsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No exam selected", "Select an exam to edit first.");
            return;
        }
        try {
            Subject subject = subjectComboBox.getValue();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTitle(titleField.getText());
            selected.setExamDate(examDatePicker.getValue());
            examService.updateExam(selected, selectedTopicIds.stream().sorted().toList());
            refreshExams();
            examsList.getSelectionModel().clearSelection();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update exam", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteExam() {
        Exam selected = examsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No exam selected", "Select an exam to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete exam \"" + selected.getTitle() + "\"?");
        confirm.setHeaderText("Confirm delete");
        DialogStyler.style(confirm);
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;

        try {
            examService.deleteExam(selected.getId());
            clearForm();
            refreshExams();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshAvailableTopics();
        refreshExams();
        Exam selected = examsList.getSelectionModel().getSelectedItem();
        if (selected != null) loadExamTopics(selected);
    }

    private void refreshSubjects() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            Subject previous = subjectComboBox.getValue();
            subjects.setAll(all);
            subjectsById.clear();
            for (Subject subject : all) subjectsById.put(subject.getId(), subject);
            if (previous != null && subjects.contains(previous)) subjectComboBox.setValue(previous);
            else if (!subjects.isEmpty()) subjectComboBox.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshAvailableTopics() {
        Subject subject = subjectComboBox.getValue();
        try {
            availableTopics.clear();
            if (subject != null) availableTopics.addAll(topicService.getTopicsForSubject(subject.getId()));
            selectedTopicIds.removeIf(id -> availableTopics.stream().noneMatch(t -> t.getId() == id));
            topicSelectionList.refresh();
            updateSelectedTopicCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
        }
    }

    private void loadExamTopics(Exam exam) {
        try {
            selectedTopicIds.clear();
            selectedTopicIds.addAll(examService.getTopicIds(exam.getId()));
            refreshAvailableTopics();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load exam topics: " + e.getMessage());
        }
    }

    private void refreshExams() {
        try {
            exams.setAll(examService.getAllExams());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load exams: " + e.getMessage());
        }
    }

    private void updateSelectedTopicCount() {
        if (selectedTopicCountLabel != null) {
            selectedTopicCountLabel.setText(selectedTopicIds.size() + " topic"
                    + (selectedTopicIds.size() == 1 ? "" : "s") + " selected");
        }
    }

    private void clearForm() {
        titleField.clear();
        examDatePicker.setValue(null);
        selectedTopicIds.clear();
        topicSelectionList.refresh();
        updateSelectedTopicCount();
        examsList.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        DialogStyler.style(alert);
        alert.showAndWait();
    }
}
