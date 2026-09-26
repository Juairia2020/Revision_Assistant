package com.revisionassistant.controller;

import com.revisionassistant.navigation.AppNavigator;
import com.revisionassistant.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

/** Controller for the local login screen. */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() {
        clearMessage();

        try {
            userService.login(emailField.getText(), passwordField.getText());
            try {
                if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
                    userService.rememberCurrentSession();
                } else {
                    // Signing in without the box checked on a device that already has a
                    // remembered session (perhaps for a different account) should not
                    // silently keep that old session alive.
                    userService.forgetRememberedSession();
                }
            } catch (SQLException e) {
                // Persisting "remember me" is a convenience on top of a successful
                // login, not a requirement for it - proceed either way and the user
                // simply logs in again next time if this could not be saved.
            }
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
