package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.util.DialogStyler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for SubjectView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link SubjectService}.
 *
 * <p>Subjects are presented as a wrapping grid of colored cards (matching
 * the app's visual design) rather than a plain list; clicking a card
 * selects it for editing/deleting, the same role a ListView selection
 * played previously.
 */
public class SubjectController {

    @FXML
    private FlowPane subjectsGrid;

    @FXML
    private TextField nameField;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Label countCaptionLabel;

    private final SubjectService subjectService = new SubjectService();
    private Subject selectedSubject;

    @FXML
    public void initialize() {
        colorPicker.setValue(Color.LIGHTBLUE);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        try {
            subjectService.addSubject(nameField.getText(), colorToHex(colorPicker.getValue()));
            clearForm();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add subject", e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Select a subject to edit first.");
            return;
        }
        try {
            selectedSubject.setName(nameField.getText());
            selectedSubject.setColor(colorToHex(colorPicker.getValue()));
            subjectService.updateSubject(selectedSubject);
            refreshTable();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update subject", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Select a subject to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete subject \"" + selectedSubject.getName() + "\"? This cannot be undone.");
        confirm.setHeaderText("Confirm delete");
        DialogStyler.style(confirm);
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            int deletedId = selectedSubject.getId();
            subjectService.deleteSubject(deletedId);
            clearForm();
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Cannot delete subject", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleClearSelection() {
        clearForm();
    }

    private void refreshTable() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            renderGrid(all);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void renderGrid(List<Subject> subjects) {
        subjectsGrid.getChildren().clear();

        if (countCaptionLabel != null) {
            countCaptionLabel.setText(subjects.isEmpty()
                    ? "No subjects yet - add your first one above."
                    : subjects.size() + (subjects.size() == 1 ? " subject" : " subjects"));
        }

        if (subjects.isEmpty()) {
            Label empty = new Label("No subjects yet. Add your first subject to start tracking progress.");
            empty.setWrapText(true);
            empty.getStyleClass().add("empty-state");
            subjectsGrid.getChildren().add(empty);
            return;
        }

        for (Subject subject : subjects) {
            subjectsGrid.getChildren().add(buildSubjectCard(subject));
        }
    }

    private VBox buildSubjectCard(Subject subject) {
        Region icon = new Region();
        icon.getStyleClass().add("subject-icon");
        icon.setStyle("-fx-background-color: " + subject.getColor() + ";");

        Label name = new Label(subject.getName());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label meta = new Label("Subject #" + subject.getId());
        meta.getStyleClass().add("row-meta");

        VBox card = new VBox(10, icon, name, meta);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(190);
        card.setMinWidth(190);
        card.getStyleClass().add("subject-card");
        if (selectedSubject != null && selectedSubject.getId() == subject.getId()) {
            card.getStyleClass().add("subject-card-selected");
        }
        card.setOnMouseClicked(event -> selectSubject(subject));
        return card;
    }

    private void selectSubject(Subject subject) {
        selectedSubject = subject;
        nameField.setText(subject.getName());
        colorPicker.setValue(hexToColor(subject.getColor()));
        refreshTable();
    }

    private void clearForm() {
        nameField.clear();
        colorPicker.setValue(Color.LIGHTBLUE);
        selectedSubject = null;
        refreshTable();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        DialogStyler.style(alert);
        alert.showAndWait();
    }

    private String colorToHex(Color color) {
        if (color == null) {
            return "#ADD8E6";
        }
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private Color hexToColor(String hex) {
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return Color.LIGHTBLUE;
        }
    }

}
