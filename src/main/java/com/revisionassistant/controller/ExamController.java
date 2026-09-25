package com.revisionassistant.controller;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Subject;
import com.revisionassistant.service.ExamService;
import com.revisionassistant.service.SubjectService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private TableView<Exam> examsTable;
    @FXML
    private TableColumn<Exam, String> subjectColumn;
    @FXML
    private TableColumn<Exam, String> titleColumn;
    @FXML
    private TableColumn<Exam, String> dateColumn;
    @FXML
    private TableColumn<Exam, String> daysRemainingColumn;
    @FXML
    private TableColumn<Exam, String> progressColumn;

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
        subjectColumn.setCellValueFactory(data -> {
            Subject subject = subjectsById.get(data.getValue().getSubjectId());
            return new SimpleStringProperty(subject == null ? "—" : subject.getName());
        });
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getExamDate().toString()));
        daysRemainingColumn.setCellValueFactory(data -> {
            long days = data.getValue().getDaysRemaining();
            String text;
            if (days < 0) {
                text = "Past";
            } else if (days == 0) {
                text = "Today";
            } else if (days == 1) {
                text = "1 day";
            } else {
                text = days + " days";
            }
            return new SimpleStringProperty(text);
        });
        progressColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProgress() + "%"));

        examsTable.setItems(exams);
        examsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                subjectComboBox.setValue(subjectsById.get(newValue.getSubjectId()));
                titleField.setText(newValue.getTitle());
                examDatePicker.setValue(newValue.getExamDate());
                progressSlider.setValue(newValue.getProgress());
            }
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
        Exam selected = examsTable.getSelectionModel().getSelectedItem();
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
        Exam selected = examsTable.getSelectionModel().getSelectedItem();
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
        examsTable.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
