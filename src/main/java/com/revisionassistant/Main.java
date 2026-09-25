package com.revisionassistant;

import com.revisionassistant.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Application entry point. Makes sure the SQLite database and its
 * schema exist, then loads the main FXML view.
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/revisionassistant/fxml/MainView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 960, 620);
            scene.getStylesheets().add(
                    getClass().getResource("/com/revisionassistant/css/style.css").toExternalForm());

            primaryStage.setTitle("Revision Assistant");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(760);
            primaryStage.setMinHeight(480);
            primaryStage.show();
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
