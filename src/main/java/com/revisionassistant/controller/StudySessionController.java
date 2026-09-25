package com.revisionassistant.controller;

import com.revisionassistant.model.StudySession;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.StudySessionService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for StudySessionView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link StudySessionService},
 * {@link SubjectService} and {@link TopicService}.
 */
public class StudySessionController {

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<Topic> topicComboBox;
    @FXML
    private DatePicker sessionDatePicker;
    @FXML
    private Spinner<Integer> durationSpinner;
    @FXML
    private TextField notesField;

    @FXML
    private TableView<StudySession> sessionsTable;
    @FXML
    private TableColumn<StudySession, String> subjectColumn;
    @FXML
    private TableColumn<StudySession, String> topicColumn;
    @FXML
    private TableColumn<StudySession, String> dateColumn;
    @FXML
    private TableColumn<StudySession, String> durationColumn;
    @FXML
    private TableColumn<StudySession, String> notesColumn;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final StudySessionService studySessionService = new StudySessionService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<StudySession> sessions = FXCollections.observableArrayList();
    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

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
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshTopicChoices());

        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 600, 30, 5));
        durationSpinner.setEditable(true);

        sessionDatePicker.setValue(LocalDate.now());

        setUpTable();
        refreshSubjects();
        refreshSessions();
    }

    private void setUpTable() {
        subjectColumn.setCellValueFactory(data -> {
            Subject subject = subjectsById.get(data.getValue().getSubjectId());
            return new SimpleStringProperty(subject == null ? "—" : subject.getName());
        });
        topicColumn.setCellValueFactory(data -> {
            Integer topicId = data.getValue().getTopicId();
            Topic topic = topicId == null ? null : topicsById.get(topicId);
            return new SimpleStringProperty(topic == null ? "—" : topic.getName());
        });
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate().toString()));
        durationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDurationMinutes() + " min"));
        notesColumn.setCellValueFactory(data -> {
            String notes = data.getValue().getNotes();
            return new SimpleStringProperty(notes == null || notes.isEmpty() ? "—" : notes);
        });

        sessionsTable.setItems(sessions);
        sessionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                populateForm(newValue);
            }
        });
    }

    private void populateForm(StudySession session) {
        subjectComboBox.setValue(subjectsById.get(session.getSubjectId()));
        refreshTopicChoices();
        Integer topicId = session.getTopicId();
        topicComboBox.setValue(topicId == null ? null : topicsById.get(topicId));
        sessionDatePicker.setValue(session.getDate());
        durationSpinner.getValueFactory().setValue(session.getDurationMinutes());
        notesField.setText(session.getNotes());
    }

    @FXML
    private void handleAddSession() {
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            studySessionService.addSession(
                    subject == null ? 0 : subject.getId(),
                    topic == null ? null : topic.getId(),
                    sessionDatePicker.getValue(),
                    durationSpinner.getValue(),
                    notesField.getText());
            clearForm();
            refreshSessions();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not record session", e.getMessage());
        }
    }

    @FXML
    private void handleEditSession() {
        StudySession selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No session selected", "Select a session to edit first.");
            return;
        }
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTopicId(topic == null ? null : topic.getId());
            selected.setDate(sessionDatePicker.getValue());
            selected.setDurationMinutes(durationSpinner.getValue());
            selected.setNotes(notesField.getText());
            studySessionService.updateSession(selected);
            refreshSessions();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update session", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteSession() {
        StudySession selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No session selected", "Select a session to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this study session?");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            studySessionService.deleteSession(selected.getId());
            clearForm();
            refreshSessions();
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

    private void refreshTopicChoices() {
        Subject subject = subjectComboBox.getValue();
        ObservableList<Topic> topics = FXCollections.observableArrayList();
        topics.add(null);
        if (subject != null) {
            try {
                topics.addAll(topicService.getTopicsForSubject(subject.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
            }
        }
        topicComboBox.setItems(topics);
        topicComboBox.setConverter(new StringConverter<Topic>() {
            @Override
            public String toString(Topic topic) {
                return topic == null ? "No specific topic" : topic.getName();
            }

            @Override
            public Topic fromString(String string) {
                return topicComboBox.getValue();
            }
        });
    }

    private void refreshSessions() {
        rebuildTopicIndex();
        try {
            sessions.setAll(studySessionService.getAllSessions());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load sessions: " + e.getMessage());
        }
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

    private void clearForm() {
        sessionDatePicker.setValue(LocalDate.now());
        durationSpinner.getValueFactory().setValue(30);
        notesField.clear();
        sessionsTable.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
