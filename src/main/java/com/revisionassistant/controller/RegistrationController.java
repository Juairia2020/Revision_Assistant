package com.revisionassistant.controller;

import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

/** Controller for creating a local Revision Assistant account. */
public class RegistrationController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleRegister() {
        clearMessage();

        try {
            userService.register(
                    nameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText()
            );
            try {
                userService.rememberCurrentSession();
            } catch (SQLException e) {
                // Persisting "remember me" is a convenience on top of a successful
                // registration, not a requirement for it.
            }
            AppNavigator.showMain(getStage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("The account could not be saved. Please try again.");
        } catch (Exception e) {
            showError("The study workspace could not be opened.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            AppNavigator.showLogin(getStage());
        } catch (Exception e) {
            showError("The login screen could not be opened.");
        }
    }

    private Stage getStage() {
        return (Stage) nameField.getScene().getWindow();
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
