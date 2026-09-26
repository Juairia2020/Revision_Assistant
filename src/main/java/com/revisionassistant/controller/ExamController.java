package com.revisionassistant.controller;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Subject;
import com.revisionassistant.service.ExamService;
import com.revisionassistant.service.SubjectService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Slider;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for ExamView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link ExamService} and
 * {@link SubjectService}.
 */
public class ExamController {

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private TextField titleField;
    @FXML
    private DatePicker examDatePicker;
    @FXML
    private Slider progressSlider;
    @FXML
    private javafx.scene.control.Label progressValueLabel;

    @FXML
    private ListView<Exam> examsList;

    private final SubjectService subjectService = new SubjectService();
    private final ExamService examService = new ExamService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Exam> exams = FXCollections.observableArrayList();
    private final Map<Integer, Subject> subjectsById = new HashMap<>();

    @FXML
    public void initialize() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(new StringConverter<Subject>() {
            @Override
            public String toString(Subject subject) {
                return subject == null ? "" : subject.getName();
            }

            @Override
            public Subject fromString(String string) {
                return subjectComboBox.getValue();
            }
        });

        progressSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                progressValueLabel.setText(Math.round(newValue.doubleValue()) + "%"));

        setUpTable();
        refreshSubjects();
        refreshExams();
    }

    private void setUpTable() {
        examsList.setItems(exams);
        examsList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Exam exam, boolean empty) {
                super.updateItem(exam, empty);
                if (empty || exam == null) { setGraphic(null); return; }
                VBox card = new VBox(6); card.getStyleClass().add("exam-card");
                Label title = new Label(exam.getTitle()); title.getStyleClass().add("card-title");
                Subject subject = subjectsById.get(exam.getSubjectId());
                Label meta = new Label((subject == null ? "No subject" : subject.getName()) + "  •  " + exam.getExamDate()); meta.getStyleClass().add("row-meta");
                long days = exam.getDaysRemaining();
                Label daysLabel = new Label(days < 0 ? "Past" : days == 0 ? "Today" : days + " days remaining"); daysLabel.getStyleClass().add(days <= 3 ? "badge-warning" : "badge-accent");
                javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(exam.getProgress()/100.0); bar.setMaxWidth(Double.MAX_VALUE);
                Label progress = new Label(exam.getProgress() + "% prepared"); progress.getStyleClass().add("row-meta");
                card.getChildren().addAll(title, meta, daysLabel, bar, progress); setGraphic(card);
            }
        });
        examsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) { subjectComboBox.setValue(subjectsById.get(newValue.getSubjectId())); titleField.setText(newValue.getTitle()); examDatePicker.setValue(newValue.getExamDate()); progressSlider.setValue(newValue.getProgress()); }
        });
    }

    @FXML
    private void handleAddExam() {
        try {
            Subject subject = subjectComboBox.getValue();
            examService.addExam(
                    subject == null ? 0 : subject.getId(),
                    titleField.getText(),
                    examDatePicker.getValue(),
                    (int) Math.round(progressSlider.getValue()));
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
            selected.setProgress((int) Math.round(progressSlider.getValue()));
            examService.updateExam(selected);
            refreshExams();
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
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            examService.deleteExam(selected.getId());
            clearForm();
            refreshExams();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void refreshSubjects() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            Subject previouslySelected = subjectComboBox.getValue();
            subjects.setAll(all);
            subjectsById.clear();
            for (Subject subject : all) {
                subjectsById.put(subject.getId(), subject);
            }
            if (previouslySelected != null && subjects.contains(previouslySelected)) {
                subjectComboBox.setValue(previouslySelected);
            } else if (!subjects.isEmpty()) {
                subjectComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshExams() {
        try {
            exams.setAll(examService.getAllExams());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load exams: " + e.getMessage());
        }
    }

    private void clearForm() {
        titleField.clear();
        examDatePicker.setValue(null);
        progressSlider.setValue(0);
        examsList.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
