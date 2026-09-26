package com.revisionassistant.controller;

import com.revisionassistant.model.StudySession;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.StudySessionService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
    private ListView<StudySession> sessionsList;

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
        sessionsList.setItems(sessions);
        sessionsList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(StudySession session, boolean empty) {
                super.updateItem(session, empty);
                if (empty || session == null) { setGraphic(null); return; }
                VBox card = new VBox(5); card.getStyleClass().add("session-card");
                Subject subject = subjectsById.get(session.getSubjectId());
                Topic topic = session.getTopicId() == null ? null : topicsById.get(session.getTopicId());
                Label date = new Label(session.getDate() + "  •  " + session.getDurationMinutes() + " min"); date.getStyleClass().add("session-date");
                Label title = new Label(subject == null ? "Study session" : subject.getName() + (topic == null ? "" : "  •  " + topic.getName())); title.getStyleClass().add("card-title");
                Label notes = new Label(session.getNotes() == null || session.getNotes().isBlank() ? "No notes" : session.getNotes()); notes.setWrapText(true); notes.getStyleClass().add("row-meta");
                card.getChildren().addAll(date, title, notes); setGraphic(card);
            }
        });
        sessionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> { if (newValue != null) populateForm(newValue); });
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
        StudySession selected = sessionsList.getSelectionModel().getSelectedItem();
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
        StudySession selected = sessionsList.getSelectionModel().getSelectedItem();
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
        sessionsList.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
