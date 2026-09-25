package com.revisionassistant.navigation;

import com.revisionassistant.session.CurrentUser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.IOException;

/**
 * Central navigation for the application-level login/main flow.
 */
public final class AppNavigator {

    private static final String FXML_BASE = "/com/revisionassistant/fxml/";
    private static final String CSS = "/com/revisionassistant/css/style.css";
    private static final String ICON = "/com/revisionassistant/images/app-icon.png";

    private AppNavigator() {
    }

    public static void showLogin(Stage stage) throws IOException {
        show(stage, "LoginView.fxml", "Revision Assistant — Login");
    }

    public static void showRegistration(Stage stage) throws IOException {
        show(stage, "RegistrationView.fxml", "Revision Assistant — Create account");
    }

    public static void showMain(Stage stage) throws IOException {
        if (!CurrentUser.isLoggedIn()) {
            showLogin(stage);
            return;
        }
        show(stage, "MainView.fxml", "Revision Assistant");
    }

    private static void show(Stage stage, String fxml, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(FXML_BASE + fxml));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1120, 720);
        scene.setFill(Color.web("#F3F1FC"));
        scene.getStylesheets().add(
                AppNavigator.class.getResource(CSS).toExternalForm());

        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setWidth(Math.max(stage.getWidth(), 1120));
        stage.setHeight(Math.max(stage.getHeight(), 720));

        // Use one consistent application identity for every window.
        if (stage.getIcons().isEmpty()) {
            var iconStream = AppNavigator.class.getResourceAsStream(ICON);
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }
        stage.show();
    }
}
