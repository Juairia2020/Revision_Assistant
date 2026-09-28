package com.revisionassistant;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import javafx.application.Application;
import javafx.scene.control.Alert;
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
