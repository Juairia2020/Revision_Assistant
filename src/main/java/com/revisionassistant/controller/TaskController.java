package com.revisionassistant.controller;

import com.revisionassistant.model.Priority;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.TaskStatus;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.util.DialogStyler;
import com.revisionassistant.service.TaskService;
import com.revisionassistant.service.TopicService;
import com.revisionassistant.service.StudySessionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
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
    private ComboBox<TaskStatus> statusComboBox;
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
    private final StudySessionService studySessionService = new StudySessionService();

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

        statusComboBox.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        statusComboBox.setValue(TaskStatus.NOT_STARTED);

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
                VBox card = new VBox(0);
                card.getStyleClass().add(task.isCompleted() ? "mission-card mission-completed" : task.isOverdue() ? "mission-card mission-overdue" : "mission-card");
                Subject subject = subjectsById.get(task.getSubjectId());
                String color = subjectColor(subject);

                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
                VBox nodeCol = new VBox(); nodeCol.setAlignment(Pos.TOP_CENTER);
                StackPane node = new StackPane(); node.setMinSize(32, 32); node.setPrefSize(32, 32);
                Circle bg = new Circle(16);
                if (task.isCompleted()) {
                    bg.setStyle("-fx-fill: " + color + ";");
                    Label check = new Label("✓"); check.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");
                    node.getChildren().addAll(bg, check);
                } else if (task.isOverdue()) {
                    bg.setStyle("-fx-fill: #EF5B5B; -fx-effect: dropshadow(gaussian, #EF5B5B, 10, 0.35, 0, 0);");
                    Label mark = new Label("!"); mark.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");
                    node.getChildren().addAll(bg, mark);
                } else {
                    bg.setStyle("-fx-fill: " + color + "; -fx-effect: dropshadow(gaussian, " + color + ", 10, 0.35, 0, 0);");
                    Circle inner = new Circle(8); inner.setStyle("-fx-fill:white;");
                    node.getChildren().addAll(bg, inner);
                }
                nodeCol.getChildren().add(node);
                Region connector = new Region();
                connector.setMinHeight(12); connector.setPrefWidth(2); connector.setMaxWidth(2);
                connector.setStyle("-fx-background-color: " + color + ";");
                nodeCol.getChildren().add(connector);

                VBox info = new VBox(5); info.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
                Label title = new Label(task.getTitle()); title.getStyleClass().add(task.isCompleted() ? "path-node-name-done" : "path-node-name-current");
                Topic topic = task.getTopicId() == null ? null : topicsById.get(task.getTopicId());
                Label meta = new Label((subject == null ? "No subject" : subject.getName()) + (topic == null ? "" : "  •  " + topic.getName()) + "  •  " + task.getEstimatedMinutes() + " min"); meta.getStyleClass().add("row-meta");
                HBox badges = new HBox(8); badges.setAlignment(Pos.CENTER_LEFT);
                Label state = new Label(task.isCompleted() ? "Completed" : task.isOverdue() ? "Overdue" : task.getStatus().getLabel()); state.getStyleClass().add(task.isCompleted() ? "path-badge-done" : task.isOverdue() ? "badge-danger" : "path-badge-current");
                Label priority = new Label(task.getPriority().getLabel()); priority.getStyleClass().add("path-badge-upcoming");
                badges.getChildren().addAll(state, priority);
                info.getChildren().addAll(title, meta, badges);

                VBox action = new VBox(7); action.setAlignment(Pos.CENTER_RIGHT);
                Label deadline = new Label(task.getDeadline() == null ? "No deadline" : "Due " + task.getDeadline()); deadline.getStyleClass().add("row-meta");
                Button logSession = new Button("+ Study Session");
                logSession.getStyleClass().add("secondary-button");
                logSession.setOnAction(e -> handleLogStudySession(task));
                action.getChildren().addAll(deadline, logSession);
                row.getChildren().addAll(nodeCol, info, action);
                card.getChildren().add(row);
                playCardEntrance(card);
                setGraphic(card);
            }
        });
        tasksList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> { if (newValue != null) populateForm(newValue); });
    }

    private void handleLogStudySession(Task task) {
        if (task == null) return;
        TextInputDialog dialog = new TextInputDialog(String.valueOf(Math.max(5, task.getEstimatedMinutes())));
        dialog.setTitle("Add Study Session");
        dialog.setHeaderText("Log a study session for: " + task.getTitle());
        dialog.setContentText("Duration in minutes:");
        dialog.getEditor().setPromptText("Minutes");
        dialog.showAndWait().ifPresent(value -> {
            try {
                int minutes = Integer.parseInt(value.trim());
                if (minutes <= 0) throw new IllegalArgumentException("Duration must be greater than zero minutes.");
                studySessionService.addSession(
                        task.getSubjectId(),
                        task.getTopicId(),
                        LocalDate.now(),
                        minutes,
                        "From study task: " + task.getTitle());
                showFormMessage("Study session added for this task.", false);
            } catch (NumberFormatException e) {
                showFormMessage("Enter a whole number of minutes.", true);
            } catch (IllegalArgumentException | SQLException e) {
                showFormMessage(e.getMessage() == null ? "Could not add the study session." : e.getMessage(), true);
            }
        });
    }

    /**
     * Called whenever a task's status transitions into Completed (either
     * at creation or via Update). Logs a matching study session
     * automatically, using the task's estimated time as the duration.
     */
    private void autoLogSessionForCompletedTask(int subjectId, Integer topicId, String taskTitle, int estimatedMinutes) {
        try {
            int minutes = Math.max(5, estimatedMinutes);
            studySessionService.addSession(
                    subjectId,
                    topicId,
                    LocalDate.now(),
                    minutes,
                    "Completed study task: " + taskTitle);
        } catch (IllegalArgumentException | SQLException e) {
            showFormMessage("Task marked complete, but the study session could not be logged automatically"
                    + (e.getMessage() == null ? "." : ": " + e.getMessage()), true);
        }
    }

    private String subjectColor(Subject subject) {
        String color = subject == null ? "#6C63F5" : subject.getColor();
        try { Color.web(color); return color; } catch (Exception e) { return "#6C63F5"; }
    }

    private void playCardEntrance(VBox card) {
        FadeTransition fade = new FadeTransition(Duration.millis(250), card);
        fade.setFromValue(0); fade.setToValue(1); fade.play();
    }

    private void populateForm(Task task) {
        subjectComboBox.setValue(subjectsById.get(task.getSubjectId()));
        refreshTopicChoices();
        Integer topicId = task.getTopicId();
        topicComboBox.setValue(topicId == null ? null : topicsById.get(topicId));
        titleField.setText(task.getTitle());
        estimatedMinutesSpinner.getValueFactory().setValue(task.getEstimatedMinutes());
        priorityComboBox.setValue(task.getPriority());
        statusComboBox.setValue(task.getStatus());
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
            TaskStatus startingStatus = statusComboBox.getValue();
            taskService.addTask(
                    subject.getId(),
                    topic == null ? null : topic.getId(),
                    title,
                    minutes,
                    priorityComboBox.getValue(),
                    deadlinePicker.getValue(),
                    startingStatus);

            showFormMessage("Task added successfully.", false);
            clearForm();
            refreshTasks();
            titleField.requestFocus();

            if (startingStatus == TaskStatus.COMPLETED) {
                autoLogSessionForCompletedTask(subject.getId(), topic == null ? null : topic.getId(), title, minutes);
            }
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
            boolean wasCompleted = selected.isCompleted();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTopicId(topic == null ? null : topic.getId());
            selected.setTitle(titleField.getText());
            selected.setEstimatedMinutes(estimatedMinutesSpinner.getValue());
            selected.setPriority(priorityComboBox.getValue());
            selected.setStatus(statusComboBox.getValue());
            selected.setDeadline(deadlinePicker.getValue());
            taskService.updateTask(selected);
            refreshTasks();

            boolean justCompleted = !wasCompleted && selected.isCompleted();
            if (justCompleted) {
                autoLogSessionForCompletedTask(selected.getSubjectId(), selected.getTopicId(),
                        selected.getTitle(), selected.getEstimatedMinutes());
                showFormMessage("Task marked complete — a study session was logged for it.", false);
            }
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
        statusComboBox.setValue(TaskStatus.NOT_STARTED);
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
        DialogStyler.style(alert);
        alert.showAndWait();
    }

    private Circle createSubjectDot(Subject subject) {
        Circle dot = new Circle(5);
        String color = subject == null ? "#6C63F5" : subject.getColor();
        try {
            dot.setFill(Color.web(color));
        } catch (Exception e) {
            dot.setFill(Color.web("#6C63F5"));
        }
        return dot;
    }



}
