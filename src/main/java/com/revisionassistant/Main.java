package com.revisionassistant;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.navigation.AppNavigator;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Application entry point. Database setup happens first; every new
 * application session then starts at the local login screen.
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
            AppNavigator.showLogin(primaryStage);
        } catch (Exception e) {
            showFatalError("Could not start the application:\n" + e.getMessage());
        }
    }

    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Startup error");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
