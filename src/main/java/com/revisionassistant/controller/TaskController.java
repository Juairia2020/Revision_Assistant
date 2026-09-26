package com.revisionassistant.controller;

import com.revisionassistant.model.Priority;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TaskService;
import com.revisionassistant.service.TopicService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for TaskView.fxml. Handles UI events and basic
 * filtering only - all validation and persistence goes through
 * {@link TaskService}, {@link SubjectService} and {@link TopicService}.
 */
public class TaskController {

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<Topic> topicComboBox;
    @FXML
    private TextField titleField;
    @FXML
    private Spinner<Integer> estimatedMinutesSpinner;
    @FXML
    private ComboBox<Priority> priorityComboBox;
    @FXML
    private DatePicker deadlinePicker;
    @FXML
    private Button addTaskButton;
    @FXML
    private Label formStatusLabel;

    @FXML
    private ComboBox<Subject> filterSubjectComboBox;
    @FXML
    private ComboBox<Boolean> filterStatusComboBox;
    @FXML
    private ComboBox<Priority> filterPriorityComboBox;

    @FXML
    private ListView<Task> tasksList;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final TaskService taskService = new TaskService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

    @FXML
    public void initialize() {
        setUpFormControls();
        setUpFilterControls();
        setUpTable();
        refreshSubjects();
        refreshTasks();
    }

    private void setUpFormControls() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(subjectConverter(null));
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshTopicChoices());

        priorityComboBox.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityComboBox.setValue(Priority.MEDIUM);

        estimatedMinutesSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 600, 30, 5));
        estimatedMinutesSpinner.setEditable(true);
        estimatedMinutesSpinner.getEditor().setPromptText("Minutes");

        deadlinePicker.setValue(null);

        if (addTaskButton != null) {
            addTaskButton.disableProperty().bind(subjectComboBox.valueProperty().isNull()
                    .or(titleField.textProperty().isEmpty()));
        }
        titleField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (formStatusLabel != null && newValue != null && !newValue.trim().isEmpty()) {
                formStatusLabel.setText("");
            }
        });
    }

    private void setUpFilterControls() {
        ObservableList<Subject> filterSubjects = FXCollections.observableArrayList();
        filterSubjects.add(null);
        filterSubjectComboBox.setItems(filterSubjects);
        filterSubjectComboBox.setConverter(subjectConverter("All subjects"));
        filterSubjectComboBox.setValue(null);
        filterSubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        ObservableList<Boolean> statusOptions = FXCollections.observableArrayList();
        statusOptions.add(null);
        statusOptions.add(Boolean.FALSE);
        statusOptions.add(Boolean.TRUE);
        filterStatusComboBox.setItems(statusOptions);
        filterStatusComboBox.setConverter(new StringConverter<Boolean>() {
            @Override
            public String toString(Boolean value) {
                if (value == null) {
                    return "All statuses";
                }
                return value ? "Completed" : "Pending";
            }

            @Override
            public Boolean fromString(String string) {
                return null;
            }
        });
        filterStatusComboBox.setValue(null);
        filterStatusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        ObservableList<Priority> filterPriorities = FXCollections.observableArrayList();
        filterPriorities.add(null);
        filterPriorities.addAll(Priority.values());
        filterPriorityComboBox.setItems(filterPriorities);
        filterPriorityComboBox.setConverter(new StringConverter<Priority>() {
            @Override
            public String toString(Priority priority) {
                return priority == null ? "All priorities" : priority.getLabel();
            }

            @Override
            public Priority fromString(String string) {
                return null;
            }
        });
        filterPriorityComboBox.setValue(null);
        filterPriorityComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void setUpTable() {
        tasksList.setItems(tasks);
        tasksList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) { setGraphic(null); return; }
                VBox card = new VBox(6); card.getStyleClass().add(task.isCompleted() ? "task-card completed" : task.isOverdue() ? "task-card overdue" : "task-card");
                Label title = new Label(task.getTitle()); title.getStyleClass().add("card-title");
                Subject subject = subjectsById.get(task.getSubjectId());
                Topic topic = task.getTopicId() == null ? null : topicsById.get(task.getTopicId());
                Label meta = new Label((subject == null ? "No subject" : subject.getName()) + (topic == null ? "" : "  •  " + topic.getName()) + "  •  " + task.getEstimatedMinutes() + " min"); meta.getStyleClass().add("row-meta");
                Label state = new Label(task.isCompleted() ? "Completed" : task.isOverdue() ? "Overdue" : task.getPriority().getLabel()); state.getStyleClass().add(task.isCompleted() ? "badge-success" : task.isOverdue() ? "badge-danger" : "badge-accent");
                Label deadline = new Label(task.getDeadline() == null ? "No deadline" : "Due " + task.getDeadline()); deadline.getStyleClass().add("row-meta");
                card.getChildren().addAll(title, meta, state, deadline); setGraphic(card);
            }
        });
        tasksList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> { if (newValue != null) populateForm(newValue); });
    }

    private void populateForm(Task task) {
        subjectComboBox.setValue(subjectsById.get(task.getSubjectId()));
        refreshTopicChoices();
        Integer topicId = task.getTopicId();
        topicComboBox.setValue(topicId == null ? null : topicsById.get(topicId));
        titleField.setText(task.getTitle());
        estimatedMinutesSpinner.getValueFactory().setValue(task.getEstimatedMinutes());
        priorityComboBox.setValue(task.getPriority());
        deadlinePicker.setValue(task.getDeadline());
    }

    @FXML
    private void handleAddTask() {
        try {
            Subject subject = subjectComboBox.getValue();
            if (subject == null) {
                showFormMessage("Choose a subject before adding a task.", true);
                return;
            }
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) {
                showFormMessage("Give the task a short title first.", true);
                titleField.requestFocus();
                return;
            }

            commitSpinnerValue();
            Integer minutes = estimatedMinutesSpinner.getValue();
            if (minutes == null || minutes < 0) {
                showFormMessage("Estimated time must be 0 minutes or more.", true);
                return;
            }

            Topic topic = topicComboBox.getValue();
            taskService.addTask(
                    subject.getId(),
                    topic == null ? null : topic.getId(),
                    title,
                    minutes,
                    priorityComboBox.getValue(),
                    deadlinePicker.getValue());

            showFormMessage("Task added successfully.", false);
            clearForm();
            refreshTasks();
            titleField.requestFocus();
        } catch (NumberFormatException e) {
            showFormMessage("Estimated time must be a whole number.", true);
        } catch (IllegalArgumentException | SQLException e) {
            showFormMessage(e.getMessage() == null ? "The task could not be added." : e.getMessage(), true);
        }
    }

    private void commitSpinnerValue() {
        if (estimatedMinutesSpinner.isEditable()) {
            String text = estimatedMinutesSpinner.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) {
                estimatedMinutesSpinner.getValueFactory().setValue(Integer.parseInt(text.trim()));
            }
        }
    }

    private void showFormMessage(String message, boolean error) {
        if (formStatusLabel != null) {
            formStatusLabel.setText(message == null ? "" : message);
            formStatusLabel.getStyleClass().removeAll("success", "error");
            formStatusLabel.getStyleClass().add(error ? "error" : "success");
        } else if (error) {
            showAlert(Alert.AlertType.ERROR, "Could not add task", message);
        }
    }

    @FXML
    private void handleEditTask() {
        Task selected = tasksList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No task selected", "Select a task to edit first.");
            return;
        }
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTopicId(topic == null ? null : topic.getId());
            selected.setTitle(titleField.getText());
            selected.setEstimatedMinutes(estimatedMinutesSpinner.getValue());
            selected.setPriority(priorityComboBox.getValue());
            selected.setDeadline(deadlinePicker.getValue());
            taskService.updateTask(selected);
            refreshTasks();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update task", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task selected = tasksList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No task selected", "Select a task to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete task \"" + selected.getTitle() + "\"?");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            taskService.deleteTask(selected.getId());
            clearForm();
            refreshTasks();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        filterSubjectComboBox.setValue(null);
        filterStatusComboBox.setValue(null);
        filterPriorityComboBox.setValue(null);
    }

    private void applyFilters() {
        Subject subject = filterSubjectComboBox.getValue();
        Boolean status = filterStatusComboBox.getValue();
        Priority priority = filterPriorityComboBox.getValue();

        try {
            List<Task> filtered = taskService.getFilteredTasks(
                    subject == null ? null : subject.getId(), status, priority);
            tasks.setAll(filtered);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load tasks: " + e.getMessage());
        }
    }

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

    private void refreshTasks() {
        rebuildTopicIndex();
        applyFilters();
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
        subjectComboBox.setValue(null);
        refreshTopicChoices();
        titleField.clear();
        estimatedMinutesSpinner.getValueFactory().setValue(30);
        priorityComboBox.setValue(Priority.MEDIUM);
        deadlinePicker.setValue(null);
        tasksList.getSelectionModel().clearSelection();
    }

    private StringConverter<Subject> subjectConverter(String nullLabel) {
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
                return subjectComboBox.getValue();
            }
        };
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
