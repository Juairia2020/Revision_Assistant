package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.StudySessionService;
import com.revisionassistant.service.TopicMasteryService;
import com.revisionassistant.service.TopicService;
import com.revisionassistant.session.CurrentUser;
import com.revisionassistant.util.EmptyStates;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * My Study Path — visual journey of the user's topics toward their exam.
 * Progress is calculated automatically from actual topic completion.
 */
public class StudyPathController {

    @FXML private ComboBox<Subject> subjectFilterCombo;
    @FXML private Label overallProgressLabel;
    @FXML private HBox overallBarHost;
    @FXML private Label statsLabel;
    @FXML private VBox pathContainer;
    @FXML private Label emptyStateLabel;
    @FXML private HBox legendBox;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final StudySessionService studySessionService = new StudySessionService();
    private final TopicMasteryService topicMasteryService = new TopicMasteryService();

    private ProgressTrack overallBar;
    private Integer justCompletedTopicId;   // drives the one-off "done!" feedback after a refresh

    /**
     * A progress bar drawn from a plain track + fill region. JavaFX's stock
     * ProgressBar skin ignores most height styling (it renders as a thin
     * line), so this guarantees a clearly visible bar whose fill width is
     * exactly track width * fraction.
     */
    private static final class ProgressTrack extends StackPane {
        private final DoubleProperty fraction = new SimpleDoubleProperty(0);
        private Timeline running;

        ProgressTrack(double height, String fillColorOrNull) {
            getStyleClass().add("path-track");
            setMinHeight(height);
            setPrefHeight(height);
            setMaxHeight(height);
            setAlignment(Pos.CENTER_LEFT);

            Region fill = new Region();
            fill.getStyleClass().add("path-fill");
            if (fillColorOrNull != null) {
                fill.setStyle("-fx-background-color: " + fillColorOrNull + ";");
            }
            fill.maxWidthProperty().bind(widthProperty().multiply(fraction));
            fill.prefWidthProperty().bind(widthProperty().multiply(fraction));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            getChildren().add(fill);
        }

        void setProgress(double value, boolean animate) {
            double target = Math.max(0, Math.min(1, value));
            if (running != null) {
                running.stop();
            }
            if (!animate) {
                fraction.set(target);
                return;
            }
            running = new Timeline(new KeyFrame(Duration.millis(450),
                    new KeyValue(fraction, target, Interpolator.EASE_BOTH)));
            running.play();
        }
    }

    @FXML
    public void initialize() {
        overallBar = new ProgressTrack(14, null);
        HBox.setHgrow(overallBar, Priority.ALWAYS);
        overallBarHost.getChildren().add(overallBar);
        EmptyStates.decorate(emptyStateLabel);
        loadSubjects();
        refresh();
    }

    private void loadSubjects() {
        try {
            List<Subject> subjects = subjectService.getAllSubjects();
            subjectFilterCombo.getItems().clear();
            subjectFilterCombo.getItems().add(null);
            subjectFilterCombo.getItems().addAll(subjects);
            subjectFilterCombo.setConverter(new javafx.util.StringConverter<Subject>() {
                @Override public String toString(Subject s) { return s == null ? "All Subjects" : s.getName(); }
                @Override public Subject fromString(String s) { return null; }
            });
            subjectFilterCombo.setValue(null);
            subjectFilterCombo.valueProperty().addListener((obs, o, n) -> refresh());
        } catch (SQLException e) {
            showToast("Could not load subjects.");
        }
    }

    @FXML
    private void refresh() {
        pathContainer.getChildren().clear();
        try {
            Subject filter = subjectFilterCombo.getValue();
            List<Subject> subjects = filter != null ? List.of(filter) : subjectService.getAllSubjects();

            if (subjects.isEmpty()) {
                emptyStateLabel.setVisible(true);
                emptyStateLabel.setManaged(true);
                legendBox.setVisible(false);
                overallProgressLabel.setText("0%");
                overallBar.setProgress(0, false);
                statsLabel.setText("No subjects yet. Add a subject and topics to see your study path.");
                return;
            }

            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            legendBox.setVisible(true);

            // Calculate overall stats
            int totalTopics = 0;
            int completedTopics = 0;

            // Build per-subject sections
            Map<Subject, List<Topic>> subjectTopics = new LinkedHashMap<>();
            for (Subject s : subjects) {
                List<Topic> topics = topicService.getTopicsForSubject(s.getId());
                subjectTopics.put(s, topics);
                totalTopics += topics.size();
                for (Topic t : topics) { if (t.isCompleted()) completedTopics++; }
            }

            double overallFraction = totalTopics == 0 ? 0.0 : (double) completedTopics / totalTopics;
            int overallPercent = (int) Math.round(overallFraction * 100);

            overallProgressLabel.setText(overallPercent + "%");
            overallBar.setProgress(overallFraction, true);
            statsLabel.setText(completedTopics + " of " + totalTopics + " topics completed across " + subjects.size() + " subject" + (subjects.size() == 1 ? "" : "s"));

            // Render path for each subject
            int subjectIndex = 0;
            for (Map.Entry<Subject, List<Topic>> entry : subjectTopics.entrySet()) {
                VBox subjectSection = buildSubjectSection(entry.getKey(), entry.getValue(), subjectIndex);
                pathContainer.getChildren().add(subjectSection);
                subjectIndex++;
            }

            animateEntrance();
            justCompletedTopicId = null;
        } catch (SQLException e) {
            showToast("Could not load study path data.");
        }
    }

    private VBox buildSubjectSection(Subject subject, List<Topic> topics, int index) {
        VBox section = new VBox(0);
        section.getStyleClass().add("path-subject-section");

        // Subject header
        int done = 0;
        for (Topic t : topics) { if (t.isCompleted()) done++; }
        int percent = topics.isEmpty() ? 0 : (int) Math.round((double) done / topics.size() * 100);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 10, 16));
        header.getStyleClass().add("path-subject-header");

        // Color dot
        String color = getSubjectColor(subject, index);
        Circle dot = new Circle(8);
        try { dot.setFill(Color.web(color)); } catch (Exception e) { dot.setFill(Color.web("#6C63F5")); }

        Label subjectName = new Label(subject.getName());
        subjectName.getStyleClass().add("path-subject-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label progressLabel = new Label(done + "/" + topics.size() + " topics · " + percent + "%");
        progressLabel.getStyleClass().add("path-subject-progress");

        ProgressTrack subjectBar = new ProgressTrack(12, color);
        subjectBar.setMinWidth(150);
        subjectBar.setPrefWidth(150);
        subjectBar.setMaxWidth(150);
        subjectBar.setProgress(topics.isEmpty() ? 0 : (double) done / topics.size(), true);

        header.getChildren().addAll(dot, subjectName, spacer, progressLabel, subjectBar);
        section.getChildren().add(header);

        if (topics.isEmpty()) {
            Label noTopics = new Label("No topics yet. Add topics to this subject to build your path.");
            noTopics.getStyleClass().add("path-empty-msg");
            noTopics.setPadding(new Insets(12, 24, 16, 24));
            section.getChildren().add(noTopics);
        } else {
            // Find the topic actually worth studying next: weighs quiz weakness,
            // upcoming exams, unmet prerequisites and overdue tasks rather than
            // simply picking the first incomplete topic in the list.
            int currentIndex = -1;
            String currentReason = null;
            try {
                var recommended = topicMasteryService.getRecommendedNext(subject.getId());
                if (recommended.isPresent()) {
                    int recommendedTopicId = recommended.get().getTopic().getId();
                    for (int i = 0; i < topics.size(); i++) {
                        if (topics.get(i).getId() == recommendedTopicId) {
                            currentIndex = i;
                            currentReason = recommended.get().getReason();
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                // fall through to the plain fallback below
            }
            if (currentIndex == -1) {
                for (int i = 0; i < topics.size(); i++) {
                    if (!topics.get(i).isCompleted()) { currentIndex = i; break; }
                }
            }

            VBox pathNodes = new VBox(0);
            pathNodes.setPadding(new Insets(4, 16, 12, 24));

            for (int i = 0; i < topics.size(); i++) {
                Topic topic = topics.get(i);
                boolean isCurrent = (i == currentIndex);
                boolean isCompleted = topic.isCompleted();
                boolean isLast = (i == topics.size() - 1);

                VBox nodeRow = buildPathNode(topic, isCompleted, isCurrent, isLast, color, i,
                        isCurrent ? currentReason : null);
                pathNodes.getChildren().add(nodeRow);
            }
            section.getChildren().add(pathNodes);
        }

        return section;
    }

    private VBox buildPathNode(Topic topic, boolean completed, boolean current, boolean isLast, String color,
                                int position, String reason) {
        VBox container = new VBox(0);

        HBox nodeRow = new HBox(14);
        nodeRow.setAlignment(Pos.TOP_LEFT);
        nodeRow.setPadding(new Insets(2, 8, 2, 0));
        nodeRow.getStyleClass().add("path-node-row");

        // Left column: connector line + circle
        VBox connectorCol = new VBox(0);
        connectorCol.setAlignment(Pos.TOP_CENTER);
        connectorCol.setMinWidth(32);
        connectorCol.setPrefWidth(32);

        // Top line (except first node)
        if (position > 0) {
            Region topLine = new Region();
            topLine.setMinHeight(12);
            topLine.setPrefWidth(2);
            topLine.setMaxWidth(2);
            topLine.setStyle(completed || current
                    ? "-fx-background-color: " + color + ";"
                    : "-fx-background-color: -ra-border-strong;");
            VBox.setMargin(topLine, new Insets(0, 0, 0, 15));
            connectorCol.getChildren().add(topLine);
        } else {
            Region spacer = new Region(); spacer.setMinHeight(12); connectorCol.getChildren().add(spacer);
        }

        // Node circle
        StackPane nodeCircle = new StackPane();
        nodeCircle.setMinSize(32, 32);
        nodeCircle.setPrefSize(32, 32);

        if (completed) {
            Circle bg = new Circle(16);
            bg.setStyle("-fx-fill: " + color + ";");
            Label check = new Label("✓");
            check.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            nodeCircle.getChildren().addAll(bg, check);
        } else if (current) {
            Circle bg = new Circle(16);
            bg.setStyle("-fx-fill: " + color + "; -fx-effect: dropshadow(gaussian, " + color + ", 12, 0.5, 0, 0);");
            Circle inner = new Circle(8);
            inner.setStyle("-fx-fill: white;");
            nodeCircle.getChildren().addAll(bg, inner);
        } else {
            Circle bg = new Circle(16);
            bg.setStyle("-fx-fill: -ra-surface-soft; -fx-stroke: -ra-border-strong; -fx-stroke-width: 2;");
            Label num = new Label(String.valueOf(position + 1));
            num.setStyle("-fx-text-fill: -ra-text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");
            nodeCircle.getChildren().addAll(bg, num);
        }
        connectorCol.getChildren().add(nodeCircle);

        // Bottom line (except last node)
        if (!isLast) {
            Region bottomLine = new Region();
            bottomLine.setMinHeight(12);
            bottomLine.setPrefWidth(2);
            bottomLine.setMaxWidth(2);
            bottomLine.setStyle(completed
                    ? "-fx-background-color: " + color + ";"
                    : "-fx-background-color: -ra-border-strong;");
            VBox.setMargin(bottomLine, new Insets(0, 0, 0, 15));
            connectorCol.getChildren().add(bottomLine);
        }

        // Right column: topic info
        VBox infoCol = new VBox(3);
        infoCol.setAlignment(Pos.CENTER_LEFT);
        infoCol.setPadding(new Insets(8, 0, 8, 0));
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        Label nameLabel = new Label(topic.getName());
        if (current) {
            nameLabel.getStyleClass().add("path-node-name-current");
        } else if (completed) {
            nameLabel.getStyleClass().add("path-node-name-done");
        } else {
            nameLabel.getStyleClass().add("path-node-name-upcoming");
        }

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        if (completed) {
            Label badge = new Label("Completed");
            badge.getStyleClass().add("path-badge-done");
            badgeRow.getChildren().add(badge);
        } else if (current) {
            Label badge = new Label("Study Now");
            badge.getStyleClass().add("path-badge-current");
            badgeRow.getChildren().add(badge);
        } else {
            Label badge = new Label("Upcoming");
            badge.getStyleClass().add("path-badge-upcoming");
            badgeRow.getChildren().add(badge);
        }

        infoCol.getChildren().addAll(nameLabel, badgeRow);

        if (current && reason != null && !reason.isBlank()) {
            Label reasonLabel = new Label(reason);
            reasonLabel.getStyleClass().add("path-node-reason");
            reasonLabel.setWrapText(true);
            reasonLabel.setMaxWidth(360);
            infoCol.getChildren().add(reasonLabel);
        }

        // Action cluster: one fixed-width primary action + one compact secondary.
        Button actionBtn;
        if (completed) {
            actionBtn = new Button("Undo");
            actionBtn.getStyleClass().add("path-undo-btn");
        } else {
            actionBtn = new Button(current ? "Mark Done ✓" : "Mark Done");
            actionBtn.getStyleClass().add(current ? "path-complete-btn-current" : "path-complete-btn");
        }
        actionBtn.setMinWidth(112);
        actionBtn.setPrefWidth(112);
        actionBtn.setMaxWidth(112);
        actionBtn.setOnAction(e -> toggleTopicCompletion(topic));

        Button logSessionBtn = new Button("+");
        logSessionBtn.getStyleClass().add("path-session-btn");
        logSessionBtn.setMinSize(32, 32);
        logSessionBtn.setPrefSize(32, 32);
        logSessionBtn.setMaxSize(32, 32);
        logSessionBtn.setTooltip(new Tooltip("Log a study session for this topic"));
        logSessionBtn.setOnAction(e -> handleLogStudySession(topic));

        HBox actionCol = new HBox(8, actionBtn, logSessionBtn);
        actionCol.setAlignment(Pos.CENTER_RIGHT);
        actionCol.setMinWidth(152);
        actionCol.setPrefWidth(152);
        actionCol.setPadding(new Insets(6, 0, 0, 0));

        // One-off acknowledgement right after a topic was marked done.
        if (completed && justCompletedTopicId != null && justCompletedTopicId == topic.getId()) {
            playCompletionFeedback(nodeCircle, nodeRow);
        }

        nodeRow.getChildren().addAll(connectorCol, infoCol, actionCol);
        container.getChildren().add(nodeRow);

        return container;
    }

    /** Checkmark pop + a soft green flash that fades out on the row (under a second). */
    private void playCompletionFeedback(Node circle, HBox row) {
        circle.setScaleX(0.4);
        circle.setScaleY(0.4);
        ScaleTransition pop = new ScaleTransition(Duration.millis(380), circle);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();

        DoubleProperty alpha = new SimpleDoubleProperty(0.22);
        alpha.addListener((obs, o, n) -> row.setStyle(String.format(java.util.Locale.ROOT,
                "-fx-background-color: rgba(34,197,94,%.3f); -fx-background-radius: 10;", n.doubleValue())));
        row.setStyle("-fx-background-color: rgba(34,197,94,0.22); -fx-background-radius: 10;");
        Timeline flash = new Timeline(new KeyFrame(Duration.millis(850), new KeyValue(alpha, 0.0)));
        flash.setOnFinished(e -> row.setStyle(null));
        flash.play();
    }

    private void toggleTopicCompletion(Topic topic) {
        try {
            topic.setCompleted(!topic.isCompleted());
            topicService.updateTopic(topic);
            justCompletedTopicId = topic.isCompleted() ? topic.getId() : null;
            refresh();
        } catch (SQLException e) {
            showToast("Could not update topic.");
        }
    }

    /**
     * Lets the user log a study session for a topic directly from the
     * Study Path, without having to navigate to Study Sessions.
     */
    private void handleLogStudySession(Topic topic) {
        if (topic == null) return;
        TextInputDialog dialog = new TextInputDialog("30");
        dialog.setTitle("Add Study Session");
        dialog.setHeaderText("Log a study session for: " + topic.getName());
        dialog.setContentText("Duration in minutes:");
        dialog.getEditor().setPromptText("Minutes");
        com.revisionassistant.util.DialogStyler.style(dialog);
        dialog.showAndWait().ifPresent(value -> {
            try {
                int minutes = Integer.parseInt(value.trim());
                if (minutes <= 0) throw new IllegalArgumentException("Duration must be greater than zero minutes.");
                studySessionService.addSession(
                        topic.getSubjectId(),
                        topic.getId(),
                        LocalDate.now(),
                        minutes,
                        "From study path: " + topic.getName());
                showToast("Study session added for " + topic.getName() + ".");
            } catch (NumberFormatException e) {
                showToast("Enter a whole number of minutes.");
            } catch (IllegalArgumentException | SQLException e) {
                showToast(e.getMessage() == null ? "Could not add the study session." : e.getMessage());
            }
        });
    }

    private String getSubjectColor(Subject subject, int index) {
        if (subject.getColor() != null && !subject.getColor().isBlank()) {
            return subject.getColor();
        }
        String[] palette = {"#6C63F5", "#22D3B4", "#FF6F91", "#FFB020", "#3B82F6", "#FB7A3C", "#34D399", "#A78BFA"};
        return palette[index % palette.length];
    }

    private void animateEntrance() {
        if (pathContainer.getChildren().isEmpty()) return;
        for (int i = 0; i < pathContainer.getChildren().size(); i++) {
            var node = pathContainer.getChildren().get(i);
            node.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), node);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 80));
            ft.play();
        }
    }

    private void showToast(String message) {
        // Inline label feedback — no Alert
        if (statsLabel != null) statsLabel.setText(message);
    }
}
