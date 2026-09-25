package com.revisionassistant.controller;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Task;
import com.revisionassistant.service.DashboardService;
import com.revisionassistant.service.DashboardService.SubjectProgress;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for DashboardView.fxml. Reads already-aggregated data
 * from {@link DashboardService} and renders it - it has no SQL and no
 * business rules of its own.
 */
public class DashboardController {

    @FXML
    private Label pendingTasksValue;
    @FXML
    private Label completedTasksValue;
    @FXML
    private Label studyTodayValue;
    @FXML
    private Label studyWeekValue;

    @FXML
    private VBox todaysTasksBox;
    @FXML
    private VBox upcomingExamsBox;
    @FXML
    private VBox subjectProgressBox;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        refresh();
    }

    /** Reloads every section from the database. Safe to call again after data changes elsewhere. */
    public void refresh() {
        try {
            pendingTasksValue.setText(String.valueOf(dashboardService.getPendingTaskCount()));
            completedTasksValue.setText(String.valueOf(dashboardService.getCompletedTaskCount()));
            studyTodayValue.setText(formatMinutes(dashboardService.getMinutesStudiedToday()));
            studyWeekValue.setText(formatMinutes(dashboardService.getMinutesStudiedThisWeek()));

            renderTasks(dashboardService.getTodaysTasks(), dashboardService.getOverdueTasks());
            renderExams(dashboardService.getUpcomingExams(5));
            renderSubjectProgress(dashboardService.getSubjectProgress());
        } catch (SQLException e) {
            Label error = new Label("Could not load dashboard data: " + e.getMessage());
            todaysTasksBox.getChildren().setAll(error);
        }
    }

    private void renderTasks(List<Task> todaysTasks, List<Task> overdueTasks) {
        todaysTasksBox.getChildren().clear();

        if (todaysTasks.isEmpty() && overdueTasks.isEmpty()) {
            todaysTasksBox.getChildren().add(emptyStateLabel("Nothing due today. You're all caught up."));
            return;
        }

        for (Task task : overdueTasks) {
            todaysTasksBox.getChildren().add(buildTaskRow(task, true));
        }
        for (Task task : todaysTasks) {
            todaysTasksBox.getChildren().add(buildTaskRow(task, false));
        }
    }

    private HBox buildTaskRow(Task task, boolean overdue) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("row-title");

        Label meta = new Label(formatMinutesShort(task.getEstimatedMinutes()) + " · "
                + task.getPriority().getLabel() + " priority"
                + (overdue ? " · overdue" : ""));
        meta.getStyleClass().add("row-meta");
        if (overdue) {
            meta.getStyleClass().add("row-meta-warning");
        }

        VBox textBox = new VBox(2, title, meta);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(10, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");
        return row;
    }

    private void renderExams(List<Exam> exams) {
        upcomingExamsBox.getChildren().clear();

        if (exams.isEmpty()) {
            upcomingExamsBox.getChildren().add(emptyStateLabel("No upcoming exams."));
            return;
        }

        for (Exam exam : exams) {
            upcomingExamsBox.getChildren().add(buildExamRow(exam));
        }
    }

    private HBox buildExamRow(Exam exam) {
        Label title = new Label(exam.getTitle());
        title.getStyleClass().add("row-title");

        long days = exam.getDaysRemaining();
        String daysText = days == 0 ? "Today" : days == 1 ? "1 day left" : days + " days left";
        Label meta = new Label(daysText + " · " + exam.getExamDate() + " · " + exam.getProgress() + "% prepared");
        meta.getStyleClass().add("row-meta");

        VBox textBox = new VBox(2, title, meta);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(10, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");
        return row;
    }

    private void renderSubjectProgress(List<SubjectProgress> progressList) {
        subjectProgressBox.getChildren().clear();

        if (progressList.isEmpty()) {
            subjectProgressBox.getChildren().add(emptyStateLabel("Add a subject to start tracking progress."));
            return;
        }

        for (SubjectProgress progress : progressList) {
            subjectProgressBox.getChildren().add(buildProgressRow(progress));
        }
    }

    private VBox buildProgressRow(SubjectProgress progress) {
        Label title = new Label(progress.getSubjectName());
        title.getStyleClass().add("row-title");

        Label countLabel = new Label(progress.getCompletedTopics() + " / " + progress.getTotalTopics() + " topics");
        countLabel.getStyleClass().add("row-meta");

        HBox header = new HBox(title, spacer(), countLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(progress.getFraction());
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("subject-progress-bar");

        VBox box = new VBox(6, header, bar);
        box.getStyleClass().add("dashboard-row");
        box.setPadding(new Insets(0));
        return box;
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        return spacer;
    }

    private Label emptyStateLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int remaining = minutes % 60;
        if (hours == 0) {
            return remaining + " min";
        }
        return hours + "h " + remaining + "m";
    }

    private String formatMinutesShort(int minutes) {
        return minutes + " min";
    }
}
