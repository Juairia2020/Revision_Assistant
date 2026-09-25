package com.revisionassistant.controller;

import com.revisionassistant.dto.ApiDemoResponseDTO;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.ApiDemoException;
import com.revisionassistant.service.ApiDemoService;
import com.revisionassistant.service.StudyPlannerService;
import com.revisionassistant.service.StudyPlannerService.Strategy;
import com.revisionassistant.service.StudyPlannerService.StudyPlan;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicDependencyService;
import com.revisionassistant.service.TopicService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for StudyToolsView.fxml: the Topic Dependencies tab (a
 * practical front end for the {@code algorithm} package's graph
 * algorithms) and the Study Planner tab (front end for the Knapsack /
 * Sum of Subsets planner). All graph and planning logic lives in
 * {@link TopicDependencyService} and {@link StudyPlannerService} - this
 * class only wires up the controls and renders their results.
 */
public class StudyToolsController {

    // ----- Topic Dependencies tab ---------------------------------------

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<Topic> topicComboBox;
    @FXML
    private ComboBox<Topic> prerequisiteComboBox;
    @FXML
    private ComboBox<Topic> targetTopicComboBox;

    @FXML
    private VBox prerequisitesBox;
    @FXML
    private VBox dependentsBox;
    @FXML
    private VBox pathBox;

    // ----- Study Planner tab ---------------------------------------------

    @FXML
    private TextField availableMinutesField;
    @FXML
    private ComboBox<Strategy> strategyComboBox;
    @FXML
    private Label planSummaryLabel;
    @FXML
    private VBox recommendedTasksBox;

    // ----- API demonstration tab ---------------------------------------

    @FXML
    private Button apiLoadButton;
    @FXML
    private Button apiCancelButton;
    @FXML
    private ProgressIndicator apiProgressIndicator;
    @FXML
    private Label apiStatusLabel;
    @FXML
    private Label apiResultLabel;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final TopicDependencyService dependencyService = new TopicDependencyService();
    private final StudyPlannerService plannerService = new StudyPlannerService();
    private final ApiDemoService apiDemoService = new ApiDemoService();
    private javafx.concurrent.Task<ApiDemoResponseDTO> apiTask;

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Topic> allTopics = FXCollections.observableArrayList();
    private final Map<Integer, Subject> subjectsById = new HashMap<>();

    @FXML
    public void initialize() {
        setUpDependencyTab();
        setUpPlannerTab();
        refreshSubjects();
        refreshTopics();
    }

    private void setUpDependencyTab() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(subjectConverter(null, subjectComboBox));
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshTopicChoiceForSubject());

        topicComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshDependencyPanels());

        prerequisiteComboBox.setItems(allTopics);
        prerequisiteComboBox.setConverter(topicConverter(prerequisiteComboBox));

        targetTopicComboBox.setItems(allTopics);
        targetTopicComboBox.setConverter(topicConverter(targetTopicComboBox));

        refreshDependencyPanels();
    }

    private void setUpPlannerTab() {
        strategyComboBox.setItems(FXCollections.observableArrayList(Strategy.values()));
        strategyComboBox.setConverter(new StringConverter<Strategy>() {
            @Override
            public String toString(Strategy strategy) {
                if (strategy == null) {
                    return "";
                }
                return strategy == Strategy.PRIORITY_BASED
                        ? "Priority-based (recommended)" : "Maximize time used";
            }

            @Override
            public Strategy fromString(String string) {
                return strategyComboBox.getValue();
            }
        });
        strategyComboBox.setValue(Strategy.PRIORITY_BASED);
    }

    // ----- Topic Dependencies handlers ------------------------------------

    @FXML
    private void handleAddPrerequisite() {
        Topic topic = topicComboBox.getValue();
        Topic prerequisite = prerequisiteComboBox.getValue();
        if (topic == null || prerequisite == null) {
            showAlert(Alert.AlertType.WARNING, "Choose two topics",
                    "Select a topic and a prerequisite topic first.");
            return;
        }
        try {
            dependencyService.addDependency(topic.getId(), prerequisite.getId());
            prerequisiteComboBox.setValue(null);
            refreshDependencyPanels();
        } catch (IllegalArgumentException | IllegalStateException | SQLException e) {
            showAlert(Alert.AlertType.WARNING, "Could not add prerequisite", e.getMessage());
        }
    }

    @FXML
    private void handleShowStudyOrder() {
        Subject subject = subjectComboBox.getValue();
        if (subject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Choose a subject first.");
            return;
        }
        try {
            List<Topic> order = dependencyService.getStudyOrder(subject.getId());
            if (order.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Suggested Study Order",
                        "This subject has no topics yet.");
                return;
            }
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < order.size(); i++) {
                message.append(i + 1).append(". ").append(order.get(i).getName()).append('\n');
            }
            showAlert(Alert.AlertType.INFORMATION, "Suggested Study Order for " + subject.getName(),
                    message.toString());
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Cannot suggest an order", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleCheckCycles() {
        try {
            boolean hasCycle = dependencyService.hasCycle();
            if (hasCycle) {
                showAlert(Alert.AlertType.WARNING, "Check Dependencies",
                        "A circular dependency was found somewhere in the topic graph. "
                                + "Study order suggestions will not work until it is removed.");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Check Dependencies",
                        "No circular dependencies found. The topic graph is valid.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleFindPath() {
        Topic topic = topicComboBox.getValue();
        Topic target = targetTopicComboBox.getValue();
        pathBox.getChildren().clear();

        if (topic == null || target == null) {
            showAlert(Alert.AlertType.WARNING, "Choose two topics",
                    "Select the current topic and a target topic first.");
            return;
        }

        try {
            List<Topic> path = dependencyService.getDependencyPath(topic.getId(), target.getId());
            if (path.isEmpty()) {
                pathBox.getChildren().add(emptyStateLabel("No dependency path connects these two topics."));
                return;
            }
            StringBuilder chain = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    chain.append("  →  ");
                }
                chain.append(path.get(i).getName());
            }
            Label chainLabel = new Label(chain.toString());
            chainLabel.setWrapText(true);
            chainLabel.getStyleClass().add("row-title");
            pathBox.getChildren().add(chainLabel);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void refreshDependencyPanels() {
        Topic topic = topicComboBox.getValue();
        pathBox.getChildren().clear();

        if (topic == null) {
            prerequisitesBox.getChildren().setAll(emptyStateLabel("Select a topic to inspect its dependencies."));
            dependentsBox.getChildren().setAll(emptyStateLabel("Select a topic to inspect its dependencies."));
            return;
        }

        try {
            renderPrerequisites(topic);
            renderDependents(topic);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load dependencies: " + e.getMessage());
        }
    }

    private void renderPrerequisites(Topic topic) throws SQLException {
        prerequisitesBox.getChildren().clear();
        List<Topic> direct = dependencyService.getDirectPrerequisites(topic.getId());
        List<Topic> all = dependencyService.getAllPrerequisites(topic.getId());

        if (all.isEmpty()) {
            prerequisitesBox.getChildren().add(emptyStateLabel("No prerequisites recorded."));
            return;
        }

        Map<Integer, Boolean> isDirect = new HashMap<>();
        for (Topic t : direct) {
            isDirect.put(t.getId(), true);
        }

        for (Topic prerequisite : all) {
            boolean direct1 = isDirect.getOrDefault(prerequisite.getId(), false);
            prerequisitesBox.getChildren().add(buildDependencyRow(prerequisite, direct1,
                    direct1 ? () -> handleRemovePrerequisite(topic, prerequisite) : null));
        }
    }

    private void renderDependents(Topic topic) throws SQLException {
        dependentsBox.getChildren().clear();
        List<Topic> direct = dependencyService.getDirectDependents(topic.getId());
        List<Topic> all = dependencyService.getAllDependents(topic.getId());

        if (all.isEmpty()) {
            dependentsBox.getChildren().add(emptyStateLabel("No other topics depend on this one yet."));
            return;
        }

        Map<Integer, Boolean> isDirect = new HashMap<>();
        for (Topic t : direct) {
            isDirect.put(t.getId(), true);
        }

        for (Topic dependent : all) {
            dependentsBox.getChildren().add(
                    buildDependencyRow(dependent, isDirect.getOrDefault(dependent.getId(), false), null));
        }
    }

    private void handleRemovePrerequisite(Topic topic, Topic prerequisite) {
        try {
            dependencyService.removeDependency(topic.getId(), prerequisite.getId());
            refreshDependencyPanels();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private HBox buildDependencyRow(Topic topic, boolean direct, Runnable onRemove) {
        Subject subject = subjectsById.get(topic.getSubjectId());
        Label title = new Label(topic.getName());
        title.getStyleClass().add("row-title");

        Label meta = new Label((subject == null ? "" : subject.getName()) + (direct ? " · direct" : " · indirect"));
        meta.getStyleClass().add("row-meta");

        VBox textBox = new VBox(2, title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(10, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");

        if (onRemove != null) {
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("danger-button");
            removeButton.setOnAction(event -> onRemove.run());
            row.getChildren().add(removeButton);
        }
        return row;
    }

    // ----- Study Planner handlers --------------------------------------------

    @FXML
    private void handleGeneratePlan() {
        int minutes;
        try {
            minutes = Integer.parseInt(availableMinutesField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid time",
                    "Enter the number of minutes you have available as a whole number.");
            return;
        }

        try {
            StudyPlan plan = plannerService.generatePlan(minutes, strategyComboBox.getValue());
            renderPlan(plan);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Could not generate plan", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void renderPlan(StudyPlan plan) {
        recommendedTasksBox.getChildren().clear();

        String summary = plan.getMinutesUsed() + " of " + plan.getMinutesAvailable() + " minutes used";
        if (plan.getStrategy() == Strategy.PRIORITY_BASED) {
            summary += "  ·  total priority score " + plan.getTotalValue();
        }
        planSummaryLabel.setText(summary);

        if (plan.getRecommendedTasks().isEmpty()) {
            recommendedTasksBox.getChildren().add(
                    emptyStateLabel("No pending tasks fit in the available time."));
            return;
        }

        for (Task task : plan.getRecommendedTasks()) {
            recommendedTasksBox.getChildren().add(buildTaskRow(task));
        }
    }

    private HBox buildTaskRow(Task task) {
        Subject subject = subjectsById.get(task.getSubjectId());

        Label title = new Label(task.getTitle());
        title.getStyleClass().add("row-title");

        String subjectName = subject == null ? "" : subject.getName();
        String deadlinePart = task.getDeadline() == null ? "" : " · due " + task.getDeadline();
        Label meta = new Label(subjectName + " · " + task.getEstimatedMinutes() + " min · "
                + task.getPriority().getLabel() + " priority" + deadlinePart);
        meta.getStyleClass().add("row-meta");

        VBox textBox = new VBox(2, title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(10, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");
        return row;
    }

    // ----- API demonstration ---------------------------------------------

    @FXML
    private void handleLoadApiDemo() {
        if (apiTask != null && apiTask.isRunning()) {
            return;
        }

        apiLoadButton.setDisable(true);
        apiCancelButton.setDisable(false);
        apiProgressIndicator.setVisible(true);
        apiStatusLabel.setText("Requesting public API…");
        apiResultLabel.setText("");

        apiTask = new javafx.concurrent.Task<>() {
            @Override
            protected ApiDemoResponseDTO call() throws Exception {
                if (isCancelled()) {
                    return null;
                }
                ApiDemoResponseDTO result = apiDemoService.loadSample();
                if (isCancelled()) {
                    return null;
                }
                return result;
            }
        };

        apiTask.setOnSucceeded(event -> {
            resetApiControls();
            ApiDemoResponseDTO result = apiTask.getValue();
            if (result != null) {
                apiResultLabel.setText(
                        "HTTP JSON response converted to Java DTO\n\n"
                                + "User ID: " + result.getUserId() + "\n"
                                + "Record ID: " + result.getId() + "\n"
                                + "Title: " + result.getTitle() + "\n"
                                + "Completed: " + result.getCompleted());
            }
        });

        apiTask.setOnFailed(event -> {
            Throwable error = apiTask.getException();
            resetApiControls();
            String message = error instanceof ApiDemoException
                    ? error.getMessage()
                    : "The API demonstration could not be completed.";
            apiStatusLabel.setText(message);
            apiResultLabel.setText("");
        });

        apiTask.setOnCancelled(event -> {
            resetApiControls();
            apiStatusLabel.setText("API request cancelled.");
            apiResultLabel.setText("");
        });

        Thread worker = new Thread(apiTask, "api-demo-request");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleCancelApiDemo() {
        if (apiTask != null && apiTask.isRunning()) {
            apiTask.cancel();
        }
    }

    private void resetApiControls() {
        apiLoadButton.setDisable(false);
        apiCancelButton.setDisable(true);
        apiProgressIndicator.setVisible(false);
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
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshTopics() {
        try {
            allTopics.setAll(topicService.getAllTopics());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
        }
    }

    private void refreshTopicChoiceForSubject() {
        Subject subject = subjectComboBox.getValue();
        ObservableList<Topic> topics = FXCollections.observableArrayList();
        if (subject != null) {
            try {
                topics.addAll(topicService.getTopicsForSubject(subject.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
            }
        }
        topicComboBox.setItems(topics);
        topicComboBox.setConverter(topicConverter(topicComboBox));
        topicComboBox.setValue(null);
        refreshDependencyPanels();
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

    private StringConverter<Topic> topicConverter(ComboBox<Topic> box) {
        return new StringConverter<Topic>() {
            @Override
            public String toString(Topic topic) {
                if (topic == null) {
                    return "";
                }
                Subject subject = subjectsById.get(topic.getSubjectId());
                return subject == null ? topic.getName() : topic.getName() + " (" + subject.getName() + ")";
            }

            @Override
            public Topic fromString(String string) {
                return box.getValue();
            }
        };
    }

    private Label emptyStateLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
