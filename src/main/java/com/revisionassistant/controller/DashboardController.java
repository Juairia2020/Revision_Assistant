package com.revisionassistant.controller;

import com.revisionassistant.util.EmptyStates;
import com.revisionassistant.model.Task;
import com.revisionassistant.service.DashboardService;
import com.revisionassistant.service.DashboardService.ContinueItem;
import com.revisionassistant.service.DashboardService.ExamProgress;
import com.revisionassistant.service.DashboardService.StudyActivity;
import com.revisionassistant.service.DashboardService.SubjectProgress;
import com.revisionassistant.session.CurrentUser;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Dashboard UI controller. Aggregation and calculations live in DashboardService. */
public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Arc overallProgressArc;
    @FXML private Label overallProgressRingLabel;
    @FXML private Label pendingTasksValue;
    @FXML private Label studyWeekValue;
    @FXML private Label upcomingExamCountValue;
    @FXML private Label completedTopicsValue;
    @FXML private VBox todaysTasksBox;
    @FXML private VBox upcomingExamsBox;
    @FXML private VBox subjectProgressBox;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private Label activityEmptyLabel;
    @FXML private Label continueSectionLabel;
    @FXML private Label continueTitleLabel;
    @FXML private Label continueReasonLabel;
    @FXML private Button continueButton;
    @FXML private Label quoteTextLabel;
    @FXML private Label quoteAuthorLabel;
    @FXML private Label quoteStatusLabel;
    @FXML private Pane quickActionsBox;

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
        buildQuickActions();
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

    /** Top stat row. */
    private void refreshStats() {
        try {
            int overall = dashboardService.getOverallProgressPercent();
            int completedTopics = dashboardService.getCompletedTopicCount();
            int upcoming = dashboardService.getUpcomingExams(20).size();

            pendingTasksValue.setText(String.valueOf(dashboardService.getPendingTaskCount()));
            studyWeekValue.setText(formatMinutes(dashboardService.getMinutesStudiedThisWeek()));
            upcomingExamCountValue.setText(String.valueOf(upcoming));
            completedTopicsValue.setText(String.valueOf(completedTopics));
            animateOverallProgressRing(overall);
        } catch (SQLException | RuntimeException e) {
            pendingTasksValue.setText("—");
            studyWeekValue.setText("—");
            upcomingExamCountValue.setText("—");
            completedTopicsValue.setText("—");
            animateOverallProgressRing(0);
            overallProgressRingLabel.setText("—");   // after the animation call, which resets it to "0%"
        }
    }

    /** Animates the Overall Progress ring's fill and keeps its centered label in sync. */
    private void animateOverallProgressRing(int percent) {
        if (overallProgressArc == null || overallProgressRingLabel == null) {
            return;
        }
        overallProgressRingLabel.setText(percent + "%");
        double targetLength = -360.0 * (percent / 100.0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(overallProgressArc.lengthProperty(), overallProgressArc.getLength())),
                new KeyFrame(Duration.millis(500), new KeyValue(overallProgressArc.lengthProperty(), targetLength))
        );
        timeline.play();
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
            continueReasonLabel.setText("");
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

    /**
     * The 5 actions, same behavior as always, now presented as a
     * genuine stack of cards (a {@link Pane}, so children can freely
     * overlap by absolute position) rather than a plain list. At rest
     * only each card's icon is visible - the label is unmanaged and
     * invisible, so it takes no layout space and doesn't render.
     * Hovering a card reveals its label and brings it to front; the
     * cards stacked above it (earlier in the list, positioned higher)
     * slide right out of the way. Leaving the stack returns everything
     * to rest.
     */
    private static final double QA_CARD_HEIGHT = 124;   // tall, portrait cards
    private static final double QA_GAP = 8;
    private static final double QA_PEEK = 26;            // visible strip of each card behind the front one
    private static final double QA_FLOOR_WIDTH = 44;    // absolute lower bound when the panel is very narrow
    private static final double QA_MAX_WIDTH = 112;
    private static final double QA_STAGGER_MS = 50;

    private final List<Timeline> quickActionTimelines = new ArrayList<>();
    private boolean quickActionsExpanded = false;

    /**
     * Quick Access as a sideways stacking card list: at rest the five
     * tall, narrow cards sit as an overlapped deck running left to right
     * (front card fully visible on the right, the ones behind it peeking
     * out to its left, shorter and dimmer to suggest depth). Hovering the
     * panel fans the deck out into a full row with a staggered delay per
     * card; leaving folds it back. Same five actions and click behavior.
     */
    private void buildQuickActions() {
        if (quickActionsBox == null) {
            return;
        }

        record QuickAction(String label, Node icon, Runnable action) {
        }

        // Last item is the front card of the deck.
        List<QuickAction> actions = List.of(
                new QuickAction("Add Subject", iconFolder(), () -> navigate("subjects")),
                new QuickAction("Add Exam", iconDocument(), () -> navigate("exams")),
                new QuickAction("Revision Task", iconCheck(), () -> navigate("planner")),
                new QuickAction("Flashcards", iconCards(), () -> navigate("flashcards")),
                new QuickAction("Start Quiz", iconTarget(), () -> navigate("quiz"))
        );

        quickActionsBox.getChildren().clear();
        quickActionTimelines.clear();
        quickActionsExpanded = false;
        List<VBox> cards = new ArrayList<>();
        for (QuickAction qa : actions) {
            VBox card = buildQuickAccessCard(qa.label(), qa.icon(), qa.action());
            cards.add(card);
            quickActionsBox.getChildren().add(card);   // later = higher z-order = front of deck
            quickActionTimelines.add(null);
        }

        // Fixed height so expanding never shifts the page layout.
        quickActionsBox.setPrefHeight(QA_CARD_HEIGHT);
        quickActionsBox.setMinHeight(QA_CARD_HEIGHT);

        // Card width follows the panel width so the fanned-out row always fits.
        quickActionsBox.widthProperty().addListener((obs, o, w) -> applyQuickActionsState(cards, quickActionsExpanded, false));
        applyQuickActionsState(cards, false, false);

        quickActionsBox.setOnMouseEntered(e -> {
            quickActionsExpanded = true;
            applyQuickActionsState(cards, true, true);
        });
        quickActionsBox.setOnMouseExited(e -> {
            quickActionsExpanded = false;
            applyQuickActionsState(cards, false, true);
        });
    }

    private VBox buildQuickAccessCard(String label, Node icon, Runnable action) {
        StackPane iconSlot = new StackPane(icon);
        iconSlot.setMinSize(30, 30);
        iconSlot.setPrefSize(30, 30);
        iconSlot.setMaxSize(30, 30);

        Label textLabel = new Label(label);
        textLabel.getStyleClass().add("quick-action-label");
        textLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Icon top-left (so it still peeks out from the deck's left edge), label at the bottom.
        VBox card = new VBox(6, iconSlot, spacer, textLabel);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefHeight(QA_CARD_HEIGHT);
        card.setMinHeight(QA_CARD_HEIGHT);
        card.setMaxHeight(QA_CARD_HEIGHT);
        card.getStyleClass().add("quick-access-card");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> action.run());
        Tooltip.install(card, new Tooltip(label));
        return card;
    }

    /** Moves every card to its fanned-out (row) or folded (deck) pose. */
    private void applyQuickActionsState(List<VBox> cards, boolean expanded, boolean animate) {
        double paneWidth = quickActionsBox.getWidth();
        if (paneWidth <= 0) {
            return;
        }
        int n = cards.size();
        // The fanned-out row must always fit inside the panel, however narrow it gets:
        // the gap tightens first, then the cards themselves shrink (down to a small
        // floor) instead of being held at a fixed minimum that pushes the last cards
        // past the window edge.
        double gap = paneWidth < 460 ? 4 : QA_GAP;
        double cardWidth = Math.max(QA_FLOOR_WIDTH, Math.min(QA_MAX_WIDTH, (paneWidth - (n - 1) * gap) / n));
        double rowStep = Math.min(cardWidth + gap, (paneWidth - cardWidth) / (n - 1));
        // The folded deck likewise never extends beyond the panel.
        double peek = Math.max(8, Math.min(QA_PEEK, (paneWidth - cardWidth) / (n - 1)));
        double restWidth = (n - 1) * peek + cardWidth;
        double restLeft = Math.max(0, (paneWidth - restWidth) / 2.0);   // deck centered in the panel

        for (int i = 0; i < n; i++) {
            VBox card = cards.get(i);
            Label label = (Label) card.getChildren().get(2);
            int depth = n - 1 - i;                                  // 0 = front card

            card.setPrefWidth(cardWidth);
            card.setMinWidth(cardWidth);
            card.setMaxWidth(cardWidth);
            card.setLayoutX(0);

            double targetX = expanded ? i * rowStep : restLeft + i * peek;
            double targetScale = expanded ? 1.0 : 1.0 - 0.05 * depth;    // back cards are shorter
            double targetOpacity = expanded ? 1.0 : Math.max(0.55, 1.0 - 0.12 * depth);
            double targetLabelOpacity = (expanded || depth == 0) ? 1.0 : 0.0;

            Timeline previous = quickActionTimelines.get(i);
            if (previous != null) {
                previous.stop();
            }
            if (!animate) {
                card.setTranslateX(targetX);
                card.setScaleY(targetScale);
                card.setOpacity(targetOpacity);
                label.setOpacity(targetLabelOpacity);
                continue;
            }

            Timeline t = new Timeline(new KeyFrame(Duration.millis(320),
                    new KeyValue(card.translateXProperty(), targetX, Interpolator.EASE_BOTH),
                    new KeyValue(card.scaleYProperty(), targetScale, Interpolator.EASE_BOTH),
                    new KeyValue(card.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH),
                    new KeyValue(label.opacityProperty(), targetLabelOpacity, Interpolator.EASE_BOTH)));
            // Fan out left-to-right; fold back right-to-left.
            t.setDelay(Duration.millis(QA_STAGGER_MS * (expanded ? i : depth)));
            quickActionTimelines.set(i, t);
            t.play();
        }
    }

    private Group iconFolder() {
        Polygon folder = new Polygon(1, 4, 6, 4, 8, 2, 15, 2, 15, 14, 1, 14);
        folder.getStyleClass().add("nav-icon");
        return new Group(folder);
    }

    private Group iconCheck() {
        Rectangle box = new Rectangle(1, 1, 14, 14);
        box.setArcWidth(3);
        box.setArcHeight(3);
        box.setFill(Color.TRANSPARENT);
        box.setStrokeWidth(1.6);
        box.getStyleClass().add("nav-icon-stroke");

        Polyline check = new Polyline(3.5, 8, 6.5, 11, 12.5, 4.5);
        check.setFill(Color.TRANSPARENT);
        check.setStrokeWidth(2);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);
        check.getStyleClass().add("nav-icon-stroke");

        return new Group(box, check);
    }

    private Group iconCards() {
        Rectangle back = new Rectangle(2, 4, 11, 8);
        back.setArcWidth(2);
        back.setArcHeight(2);
        back.setRotate(-8);
        back.setFill(Color.TRANSPARENT);
        back.setStrokeWidth(1.4);
        back.getStyleClass().add("nav-icon-stroke");

        Rectangle front = new Rectangle(3, 3, 11, 8);
        front.setArcWidth(2);
        front.setArcHeight(2);
        front.setRotate(6);
        front.getStyleClass().add("nav-icon");

        return new Group(back, front);
    }

    private Group iconTarget() {
        Circle outer = new Circle(8, 8, 6.5);
        outer.setFill(Color.TRANSPARENT);
        outer.setStrokeWidth(1.5);
        outer.getStyleClass().add("nav-icon-stroke");

        Circle inner = new Circle(8, 8, 2.4);
        inner.getStyleClass().add("nav-icon");

        return new Group(outer, inner);
    }

    private Group iconDocument() {
        Rectangle body = new Rectangle(2, 1, 12, 14);
        body.setArcWidth(2);
        body.setArcHeight(2);
        body.setFill(Color.TRANSPARENT);
        body.setStrokeWidth(1.6);
        body.getStyleClass().add("nav-icon-stroke");

        Line l1 = new Line(4.5, 5, 11.5, 5);
        Line l2 = new Line(4.5, 8, 11.5, 8);
        Line l3 = new Line(4.5, 11, 9, 11);
        for (Line line : new Line[]{l1, l2, l3}) {
            line.setStrokeWidth(1.4);
            line.getStyleClass().add("nav-icon-stroke");
        }

        return new Group(body, l1, l2, l3);
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
        continueReasonLabel.setText(item.getReason() == null ? "" : item.getReason());
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
        EmptyStates.decorate(label);
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
