package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for TopicView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link TopicService} and
 * {@link SubjectService}.
 */
public class TopicController {

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private TableView<Topic> topicsTable;
    @FXML
    private TableColumn<Topic, Integer> idColumn;
    @FXML
    private TableColumn<Topic, String> nameColumn;
    @FXML
    private TableColumn<Topic, Boolean> completedColumn;
    @FXML
    private TextField topicNameField;
    @FXML
    private CheckBox completedCheckBox;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Topic> topics = FXCollections.observableArrayList();

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
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshTopics());

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        completedColumn.setCellValueFactory(cellData -> {
            Topic topic = cellData.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(topic.isCompleted());
            property.addListener((obs, oldVal, newVal) -> {
                try {
                    topicService.setCompleted(topic.getId(), newVal);
                    topic.setCompleted(newVal);
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
                }
            });
            return property;
        });
        completedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(completedColumn));

        topicsTable.setItems(topics);
        topicsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                topicNameField.setText(newValue.getName());
                completedCheckBox.setSelected(newValue.isCompleted());
            }
        });

        refreshSubjects();
    }

    @FXML
    private void handleAddTopic() {
        Subject selectedSubject = subjectComboBox.getValue();
        try {
            int subjectId = selectedSubject == null ? 0 : selectedSubject.getId();
            topicService.addTopic(subjectId, topicNameField.getText());
            topicNameField.clear();
            refreshTopics();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add topic", e.getMessage());
        }
    }

    @FXML
    private void handleEditTopic() {
        Topic selected = topicsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No topic selected", "Select a topic to edit first.");
            return;
        }
        try {
            selected.setName(topicNameField.getText());
            selected.setCompleted(completedCheckBox.isSelected());
            topicService.updateTopic(selected);
            refreshTopics();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update topic", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTopic() {
        Topic selected = topicsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No topic selected", "Select a topic to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete topic \"" + selected.getName() + "\"?");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            topicService.deleteTopic(selected.getId());
            topicNameField.clear();
            completedCheckBox.setSelected(false);
            refreshTopics();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleMarkCompleted() {
        Topic selected = topicsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No topic selected", "Select a topic to mark as completed.");
            return;
        }
        try {
            topicService.setCompleted(selected.getId(), true);
            refreshTopics();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void refreshSubjects() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            Subject previouslySelected = subjectComboBox.getValue();
            subjects.setAll(all);
            if (previouslySelected != null && subjects.contains(previouslySelected)) {
                subjectComboBox.setValue(previouslySelected);
            } else if (!subjects.isEmpty()) {
                subjectComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshTopics() {
        Subject selectedSubject = subjectComboBox.getValue();
        if (selectedSubject == null) {
            topics.clear();
            return;
        }
        try {
            List<Topic> topicList = topicService.getTopicsForSubject(selectedSubject.getId());
            topics.setAll(topicList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
