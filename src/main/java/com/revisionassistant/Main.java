package com.revisionassistant;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import javafx.application.Application;
import com.revisionassistant.service.PomodoroTimerService;
import com.revisionassistant.service.ReminderNotificationService;
import com.revisionassistant.util.PomodoroSessionPrompt;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Application entry point. Database setup happens first; if a valid
 * "remember me" session was left on this device, it is restored
 * automatically and the application opens straight to the main workspace -
 * otherwise every new application session starts at the local login screen.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseManager.initializeDatabase();
        } catch (SQLException e) {
            showFatalError("Could not initialize the database:\n" + e.getMessage());
            return;
        }

        PomodoroTimerService pomodoro = PomodoroTimerService.getInstance();
        pomodoro.setOnWorkSessionComplete(PomodoroSessionPrompt::prompt);
        primaryStage.setOnCloseRequest(event -> {
            if (!pomodoro.isFocusSessionActive()) return;
            ButtonType closeApp = new ButtonType("Close App", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            ButtonType keepFocusing = new ButtonType("Keep Focusing", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", closeApp, keepFocusing);
            confirm.setTitle("Focus session in progress");
            confirm.setHeaderText("Are you sure? It\u2019s Focus Time!");
            confirm.initOwner(primaryStage);
            com.revisionassistant.util.DialogStyler.style(confirm);
            if (confirm.showAndWait().orElse(keepFocusing) != closeApp) {
                event.consume();
            }
        });

        try {
            boolean restoredSession = false;
            try {
                restoredSession = new UserService().tryAutoLogin() != null;
            } catch (SQLException e) {
                // A remembered session simply isn't restored if it can't be checked -
                // the login screen below is always a safe fallback.
            }

            if (restoredSession) {
                AppNavigator.showMain(primaryStage);
            } else {
                AppNavigator.showLogin(primaryStage);
            }
        } catch (Exception e) {
            showFatalError("Could not start the application:\n" + e.getMessage());
        }
    }

    @Override
    public void stop() {
        PomodoroTimerService.getInstance().shutdown();
        ReminderNotificationService reminders = ReminderNotificationService.getActive();
        if (reminders != null) reminders.stop();
    }

    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Startup error");
        com.revisionassistant.util.DialogStyler.style(alert);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
