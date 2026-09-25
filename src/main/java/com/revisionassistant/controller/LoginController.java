package com.revisionassistant.controller;

import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

/** Controller for the local login screen. */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() {
        clearMessage();

        try {
            userService.login(emailField.getText(), passwordField.getText());
            AppNavigator.showMain(getStage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("The account could not be checked. Please try again.");
        } catch (Exception e) {
            showError("The application could not open the study workspace.");
        }
    }

    @FXML
    private void handleOpenRegistration() {
        try {
            AppNavigator.showRegistration(getStage());
        } catch (Exception e) {
            showError("The registration screen could not be opened.");
        }
    }

    private Stage getStage() {
        return (Stage) emailField.getScene().getWindow();
    }

    private void showError(String message) {
        messageLabel.setText(message == null ? "Please check your details." : message);
        messageLabel.getStyleClass().remove("form-message-success");
        if (!messageLabel.getStyleClass().contains("form-message-error")) {
            messageLabel.getStyleClass().add("form-message-error");
        }
    }

    private void clearMessage() {
        messageLabel.setText("");
    }
}
