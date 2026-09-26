package com.revisionassistant.service;

import com.revisionassistant.dao.UserPreferencesDAO;
import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.UserPreferences;
import com.revisionassistant.session.CurrentUser;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Reminder service with both compact in-app toasts and native desktop
 * (system tray) notifications, so reminders are visible whether or not
 * Revision Assistant is the focused window. Date-based reminders fire
 * the day before the related event.
 */
public class ReminderNotificationService {
    private static volatile ReminderNotificationService active;

    private final UserPreferencesDAO preferencesDAO = new UserPreferencesDAO();
    private final DashboardService dashboardService = new DashboardService();
    private final TaskService taskService = new TaskService();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "revision-reminders");
        t.setDaemon(true);
        return t;
    });

    private final StackPane notificationLayer;
    private final Label notificationBadge;
    private final Set<String> sentKeys = new HashSet<>();
    private final List<String> history = new ArrayList<>();
    private int unreadCount;
    private String lastCheckedDate = "";

    private TrayIcon trayIcon;

    public ReminderNotificationService(StackPane notificationLayer, Label notificationBadge) {
        this.notificationLayer = notificationLayer;
        this.notificationBadge = notificationBadge;
        if (notificationBadge != null) notificationBadge.setVisible(false);
        initializeSystemTray();
        active = this;
    }

    public static ReminderNotificationService getActive() {
        return active;
    }

    public void start() {
        if (notificationLayer == null) return;
        executor.scheduleAtFixedRate(this::checkReminders, 5, 10, TimeUnit.MINUTES);
        refreshNow();
    }

    public void refreshNow() {
        executor.execute(this::checkReminders);
    }

    public void stop() {
        if (active == this) active = null;
        executor.shutdownNow();
        removeTrayIcon();
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public List<String> getHistory() {
        return List.copyOf(history);
    }

    public void markRead() {
        unreadCount = 0;
        if (notificationBadge != null) {
            notificationBadge.setText("0");
            notificationBadge.setVisible(false);
        }
    }

    private void checkReminders() {
        if (!CurrentUser.isLoggedIn()) return;

        LocalDate today = LocalDate.now();
        String dateKey = today.toString();
        if (!dateKey.equals(lastCheckedDate)) {
            lastCheckedDate = dateKey;
            sentKeys.clear();
        }

        try {
            UserPreferences prefs = preferencesDAO.findByUserId(CurrentUser.get().getId());
            LocalDate tomorrow = today.plusDays(1);

            // Study-session reminder: tell the student the previous day about
            // pending tasks that are due tomorrow. There is no future study-session
            // date in the current data model, so task deadlines are the actionable
            // study event available to remind about.
            if (prefs.isStudyReminders()) {
                List<Task> tomorrowTasks = taskService.getAllTasks().stream()
                        .filter(task -> !task.isCompleted())
                        .filter(task -> tomorrow.equals(task.getDeadline()))
                        .collect(Collectors.toList());

                if (!tomorrowTasks.isEmpty()) {
                    String titles = tomorrowTasks.stream()
                            .limit(3)
                            .map(Task::getTitle)
                            .collect(Collectors.joining(", "));
                    if (tomorrowTasks.size() > 3) titles += " and " + (tomorrowTasks.size() - 3) + " more";
                    notifyOnce("study-tomorrow-" + tomorrow,
                            "Study reminder for tomorrow",
                            "You have " + tomorrowTasks.size() + " pending study task"
                                    + (tomorrowTasks.size() == 1 ? "" : "s") + " due tomorrow: " + titles + ".");
                }
            }

            List<Exam> upcoming = dashboardService.getUpcomingExams(20);

            // Normal exam reminder: previous day of an exam.
            if (prefs.isExamReminders()) {
                List<Exam> tomorrowExams = upcoming.stream()
                        .filter(exam -> exam.getDaysRemaining() == 1)
                        .toList();
                for (Exam exam : tomorrowExams) {
                    notifyOnce("exam-tomorrow-" + exam.getId() + "-" + tomorrow,
                            "Exam tomorrow",
                            exam.getTitle() + " is tomorrow. Syllabus progress: "
                                    + exam.getProgress() + "%. Review the remaining topics today.");
                }
            }

            // Urgent alert: within two days AND more than 80% of the selected
            // syllabus remains incomplete, i.e. preparation is below 20%.
            for (Exam exam : upcoming) {
                long days = exam.getDaysRemaining();
                if (days >= 0 && days <= 2 && exam.getProgress() < 20) {
                    notifyOnce("urgent-exam-" + exam.getId() + "-" + today,
                            "URGENT exam preparation",
                            exam.getTitle() + " is within " + days + (days == 1 ? " day" : " days")
                                    + " and more than 80% of its selected syllabus is still incomplete.");
                }
            }

            if (prefs.isAchievementNotifications()) {
                int progress = dashboardService.getOverallProgressPercent();
                if (progress > 0 && progress % 25 == 0) {
                    notifyOnce("achievement-" + progress,
                            "Progress milestone",
                            "You're at " + progress + "% overall topic progress.");
                }
            }

            if (prefs.isStreakNotifications() && dashboardService.getMinutesStudiedToday() >= 30) {
                notifyOnce("streak", "Study streak", "Nice work — you've studied for at least 30 minutes today.");
            }
        } catch (SQLException ignored) {
            // Reminders must never interrupt the study workspace when persistence is unavailable.
        }
    }

    private void notifyOnce(String key, String title, String message) {
        synchronized (sentKeys) {
            if (!sentKeys.add(key)) return;
        }
        Platform.runLater(() -> showToast(title, message));
        showNativeNotification(title, message);
    }

    /** Small in-app toast, stacked in the top-right corner. */
    public void showToast(String title, String message) {
        unreadCount++;
        if (notificationBadge != null) {
            notificationBadge.setText(String.valueOf(Math.min(99, unreadCount)));
            notificationBadge.setVisible(true);
        }

        String record = title + ": " + message;
        history.add(0, record);
        if (history.size() > 20) history.remove(history.size() - 1);

        if (notificationLayer == null) return;

        VBox card = new VBox(3);
        card.getStyleClass().add("notification-toast");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notification-title");
        Label body = new Label(message);
        body.setWrapText(true);
        body.getStyleClass().add("notification-message");

        HBox top = new HBox(titleLabel);
        Button close = new Button("×");
        close.getStyleClass().add("notification-close");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        top.getChildren().add(close);
        top.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, body);
        close.setOnAction(e -> removeToast(card));
        StackPane.setAlignment(card, Pos.TOP_RIGHT);
        StackPane.setMargin(card, new javafx.geometry.Insets(8, 8, 0, 8));
        card.setTranslateY(notificationLayer.getChildren().size() * 66);
        card.setMouseTransparent(false);
        notificationLayer.getChildren().add(card);

        card.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(180), card);
        in.setToValue(1);
        in.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(7));
        pause.setOnFinished(e -> removeToast(card));
        pause.play();
    }

    private void removeToast(Node node) {
        if (notificationLayer == null) return;
        FadeTransition out = new FadeTransition(Duration.millis(140), node);
        out.setToValue(0);
        out.setOnFinished(e -> notificationLayer.getChildren().remove(node));
        out.play();
    }

    // ----- Out-of-app (system tray) notifications ---------------------------

    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            InputStream stream = getClass().getResourceAsStream("/com/revisionassistant/images/app-icon.png");
            if (stream == null) return;
            Image image = ImageIO.read(stream);
            if (image == null) return;

            trayIcon = new TrayIcon(image, "Revision Assistant");
            trayIcon.setImageAutoSize(true);
            ActionListener listener = e -> Platform.runLater(() -> {
                // The notification itself is already visible outside the app;
                // this keeps a future tray-click hook available without coupling
                // the service to a specific Stage/controller.
            });
            trayIcon.addActionListener(listener);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException | java.io.IOException ignored) {
            trayIcon = null;
        }
    }

    private void showNativeNotification(String title, String message) {
        if (trayIcon == null) return;
        TrayIcon.MessageType type = title.startsWith("URGENT")
                ? TrayIcon.MessageType.WARNING
                : TrayIcon.MessageType.INFO;
        trayIcon.displayMessage(title, message, type);
    }

    private void removeTrayIcon() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {
            }
            trayIcon = null;
        }
    }
}
