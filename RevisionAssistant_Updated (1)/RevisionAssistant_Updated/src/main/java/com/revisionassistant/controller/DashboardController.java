package com.revisionassistant.controller;

import com.revisionassistant.model.Task;
import com.revisionassistant.service.DashboardService;
import com.revisionassistant.service.DashboardService.ContinueItem;
import com.revisionassistant.service.DashboardService.ExamProgress;
import com.revisionassistant.service.DashboardService.StudyActivity;
import com.revisionassistant.service.DashboardService.SubjectProgress;
import com.revisionassistant.session.CurrentUser;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Arc;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Dashboard UI controller. Aggregation and calculations live in DashboardService. */
public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label overallProgressValue;
    @FXML private Label pendingTasksValue;
    @FXML private Label studyWeekValue;
    @FXML private Label upcomingExamCountValue;
    @FXML private Label completedTopicsValue;
    @FXML private Label overallProgressRingValue;
    @FXML private Label progressSummaryLabel;
    @FXML private Arc overallProgressArc;
    @FXML private VBox todaysTasksBox;
    @FXML private VBox upcomingExamsBox;
    @FXML private VBox subjectProgressBox;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private Label activityEmptyLabel;
    @FXML private Label continueSectionLabel;
    @FXML private Label continueTitleLabel;
    @FXML private Button continueButton;
    @FXML private Label quoteTextLabel;
    @FXML private Label quoteAuthorLabel;
    @FXML private Label quoteStatusLabel;

    private final DashboardService dashboardService = new DashboardService();
    private final com.revisionassistant.service.DailyQuoteService dailyQuoteService = new com.revisionassistant.service.DailyQuoteService();
    private MainController mainController;
    private ContinueItem continueItem;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM");

    @FXML
    public void initialize() {
        String name = CurrentUser.get() == null ? "there" : CurrentUser.get().getName();
        String hourMessage = java.time.LocalTime.now().getHour() < 12 ? "Good morning" :
                java.time.LocalTime.now().getHour() < 18 ? "Good afternoon" : "Good evening";
        welcomeLabel.setText(hourMessage + ", " + name);
        refresh();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void refresh() {
        refreshStats();
        refreshTasks();
        refreshExams();
        refreshSubjectProgress();
        refreshActivity();
        refreshContinue();
        loadDailyQuote();
        animateEntrance();
    }

    /** Top stat row + the "Overall Progress" ring. Kept together since the ring is derived from the same numbers. */
    private void refreshStats() {
        try {
            int overall = dashboardService.getOverallProgressPercent();
            int completedTopics = dashboardService.getCompletedTopicCount();
            int totalTopics = dashboardService.getTotalTopicCount();
            int upcoming = dashboardService.getUpcomingExams(20).size();

            overallProgressValue.setText(overall + "%");
            pendingTasksValue.setText(String.valueOf(dashboardService.getPendingTaskCount()));
            studyWeekValue.setText(formatMinutes(dashboardService.getMinutesStudiedThisWeek()));
            upcomingExamCountValue.setText(String.valueOf(upcoming));
            completedTopicsValue.setText(String.valueOf(completedTopics));
            overallProgressRingValue.setText(overall + "%");
            progressSummaryLabel.setText(completedTopics + " of " + totalTopics + " topics completed");
            animateProgress(overall / 100.0);
        } catch (SQLException | RuntimeException e) {
            overallProgressValue.setText("—");
            pendingTasksValue.setText("—");
            studyWeekValue.setText("—");
            upcomingExamCountValue.setText("—");
            completedTopicsValue.setText("—");
            overallProgressRingValue.setText("—");
            progressSummaryLabel.setText("Progress data is temporarily unavailable.");
            animateProgress(0.0);
        }
    }

    private void refreshTasks() {
        try {
            renderTasks(dashboardService.getTodaysTasks(), dashboardService.getOverdueTasks());
        } catch (SQLException | RuntimeException e) {
            todaysTasksBox.getChildren().setAll(emptyStateLabel("Could not load today's tasks."));
        }
    }

    private void refreshExams() {
        try {
            renderExams(dashboardService.getUpcomingExamProgress(5));
        } catch (SQLException | RuntimeException e) {
            upcomingExamsBox.getChildren().setAll(emptyStateLabel("Could not load upcoming exams."));
        }
    }

    private void refreshSubjectProgress() {
        try {
            renderSubjectProgress(dashboardService.getSubjectProgress());
        } catch (SQLException | RuntimeException e) {
            subjectProgressBox.getChildren().setAll(emptyStateLabel("Could not load subject progress."));
        }
    }

    private void refreshActivity() {
        try {
            renderActivity(dashboardService.getStudyActivity(7));
        } catch (SQLException | RuntimeException e) {
            activityChart.getData().clear();
            activityChart.setVisible(false);
            activityChart.setManaged(false);
            activityEmptyLabel.setText("Study activity is temporarily unavailable.");
            activityEmptyLabel.setVisible(true);
            activityEmptyLabel.setManaged(true);
        }
    }

    private void refreshContinue() {
        try {
            renderContinue(dashboardService.getContinueItem());
        } catch (SQLException | RuntimeException e) {
            continueItem = null;
            continueSectionLabel.setText("Continue Studying");
            continueTitleLabel.setText("Not available right now");
            continueButton.setText("Continue →");
        }
    }


    private void loadDailyQuote() {
        if (quoteStatusLabel == null) return;
        quoteStatusLabel.setText("Loading today's quote…");
        javafx.concurrent.Task<com.revisionassistant.service.DailyQuoteService.Quote> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.revisionassistant.service.DailyQuoteService.Quote call() throws Exception {
                        return dailyQuoteService.fetchToday();
                    }
                };
        task.setOnSucceeded(e -> {
            var quote = task.getValue();
            quoteTextLabel.setText("“" + quote.text() + "”");
            quoteAuthorLabel.setText("— " + quote.author());
            quoteStatusLabel.setText("Fetched from the daily quote API.");
        });
        task.setOnFailed(e -> {
            quoteTextLabel.setText("“Small steps each day add up to meaningful progress.”");
            quoteAuthorLabel.setText("— Revision Assistant");
            quoteStatusLabel.setText("Offline fallback shown; the API response could not be loaded.");
        });
        Thread t = new Thread(task, "daily-quote-request");
        t.setDaemon(true);
        t.start();
    }

    private void animateEntrance() {
        FadeTransition fade = new FadeTransition(Duration.millis(220), welcomeLabel.getParent());
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void animateProgress(double target) {
        double targetLength = -360.0 * target;

        overallProgressArc.setLength(0);

        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(overallProgressArc.lengthProperty(), 0)
                ),
                new KeyFrame(
                        Duration.millis(650),
                        new KeyValue(
                                overallProgressArc.lengthProperty(),
                                targetLength
                        )
                )
        );

        timeline.play();
    }

    private void renderTasks(List<Task> todaysTasks, List<Task> overdueTasks) {
        todaysTasksBox.getChildren().clear();
        if (todaysTasks.isEmpty() && overdueTasks.isEmpty()) {
            todaysTasksBox.getChildren().add(emptyStateLabel("Nothing due today. You're all caught up."));
            return;
        }
        for (Task task : overdueTasks) todaysTasksBox.getChildren().add(buildTaskRow(task, true));
        for (Task task : todaysTasks) todaysTasksBox.getChildren().add(buildTaskRow(task, false));
    }

    private HBox buildTaskRow(Task task, boolean overdue) {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("row-title");
        Label meta = new Label(formatMinutesShort(task.getEstimatedMinutes()) + " · "
                + task.getPriority().getLabel() + " priority" + (overdue ? " · overdue" : ""));
        meta.getStyleClass().add("row-meta");
        if (overdue) meta.getStyleClass().add("row-meta-warning");
        VBox text = new VBox(2, title, meta);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(10, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");
        return row;
    }

    private void renderExams(List<ExamProgress> exams) {
        upcomingExamsBox.getChildren().clear();
        if (exams.isEmpty()) {
            upcomingExamsBox.getChildren().add(emptyStateLabel("No upcoming exams. Add an exam to start tracking preparation."));
            return;
        }
        for (ExamProgress exam : exams) upcomingExamsBox.getChildren().add(buildExamCard(exam));
    }

    private VBox buildExamCard(ExamProgress exam) {
        Label title = new Label(exam.getTitle());
        title.getStyleClass().add("row-title");
        Label meta = new Label(exam.getSubjectName() + " · " +
                (exam.getDaysRemaining() == 0 ? "Today" : exam.getDaysRemaining() == 1 ? "1 day left" : exam.getDaysRemaining() + " days left") +
                " · " + exam.getExamDate());
        meta.getStyleClass().add("row-meta");
        ProgressBar bar = new ProgressBar(exam.getProgressPercent() / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("subject-progress-bar");
        Label progress = new Label(exam.getProgressPercent() + "% prepared");
        progress.getStyleClass().add("progress-value-label");
        HBox header = new HBox(title, spacer(), progress);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(5, header, meta, bar);
        card.getStyleClass().add("dashboard-row");
        return card;
    }

    private void renderSubjectProgress(List<SubjectProgress> progressList) {
        subjectProgressBox.getChildren().clear();
        if (progressList.isEmpty()) {
            subjectProgressBox.getChildren().add(emptyStateLabel("No subjects yet. Add your first subject to start tracking progress."));
            return;
        }
        for (SubjectProgress progress : progressList) {
            Label name = new Label(progress.getSubjectName());
            name.getStyleClass().add("row-title");
            Label value = new Label(progress.getPercent() + "%");
            value.getStyleClass().add("progress-value-label");
            HBox header = new HBox(name, spacer(), value);
            header.setAlignment(Pos.CENTER_LEFT);
            Label count = new Label(progress.getCompletedTopics() + " / " + progress.getTotalTopics() + " topics");
            count.getStyleClass().add("row-meta");
            ProgressBar bar = new ProgressBar(progress.getFraction());
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("subject-progress-bar");
            subjectProgressBox.getChildren().add(new VBox(4, header, count, bar));
        }
    }

    private static final String NO_ACTIVITY_MESSAGE =
            "No study activity yet. Complete a study session and it will appear here.";

    private void renderActivity(List<StudyActivity> activities) {
        activityChart.getData().clear();
        if (activities.stream().allMatch(a -> a.getMinutes() == 0)) {
            activityChart.setVisible(false);
            activityChart.setManaged(false);
            activityEmptyLabel.setText(NO_ACTIVITY_MESSAGE);
            activityEmptyLabel.setVisible(true);
            activityEmptyLabel.setManaged(true);
            return;
        }
        activityChart.setVisible(true);
        activityChart.setManaged(true);
        activityEmptyLabel.setVisible(false);
        activityEmptyLabel.setManaged(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (StudyActivity activity : activities) {
            series.getData().add(new XYChart.Data<>(activity.getDate().format(DATE_FORMAT), activity.getMinutes()));
        }
        activityChart.getData().add(series);
    }

    private void renderContinue(ContinueItem item) {
        continueItem = item;
        continueSectionLabel.setText(item.getSection());
        continueTitleLabel.setText(item.getTitle());
        continueButton.setText("Continue " + item.getSection() + " →");
    }

    @FXML private void handleContinue() { navigate(continueItem == null ? "subjects" : continueItem.getAction()); }
    @FXML private void handleAddSubject() { navigate("subjects"); }
    @FXML private void handleAddTask() { navigate("planner"); }
    @FXML private void handleFlashcards() { navigate("flashcards"); }
    @FXML private void handleQuiz() { navigate("quiz"); }
    @FXML private void handleAddExam() { navigate("exams"); }

    private void navigate(String destination) {
        if (mainController == null) return;
        switch (destination) {
            case "subjects" -> mainController.showSubjectsFromDashboard();
            case "planner", "task" -> mainController.showTasksFromDashboard();
            case "topics" -> mainController.showTopicsFromDashboard();
            case "exams" -> mainController.showExamsFromDashboard();
            case "flashcards" -> mainController.showFlashcardsFromDashboard();
            case "quiz" -> mainController.showQuizFromDashboard();
            case "path" -> mainController.showStudyPathFromDashboard();
            default -> mainController.showDashboard();
        }
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Label emptyStateLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int remainder = minutes % 60;
        if (hours == 0) return minutes + " min";
        if (remainder == 0) return hours + "h";
        return hours + "h " + remainder + "m";
    }

    private String formatMinutesShort(int minutes) {
        return minutes + " min";
    }
}
