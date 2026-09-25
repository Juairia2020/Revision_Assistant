package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.service.SubjectService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for SubjectView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link SubjectService}.
 */
public class SubjectController {

    @FXML
    private ListView<Subject> subjectsList;

    @FXML
    private TextField nameField;
    @FXML
    private ColorPicker colorPicker;

    private final SubjectService subjectService = new SubjectService();
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        subjectsList.setItems(subjects);
        subjectsList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Subject subject, boolean empty) {
                super.updateItem(subject, empty);
                if (empty || subject == null) { setGraphic(null); return; }
                HBox row = new HBox(14); row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Region swatch = new Region(); swatch.setPrefSize(14, 44);
                swatch.setStyle("-fx-background-color: " + subject.getColor() + "; -fx-background-radius: 8;");
                VBox text = new VBox(3);
                Label name = new Label(subject.getName()); name.getStyleClass().add("card-title");
                Label meta = new Label("Subject " + subject.getId() + "  •  " + subject.getColor()); meta.getStyleClass().add("row-meta");
                text.getChildren().addAll(name, meta); row.getChildren().addAll(swatch, text); setGraphic(row);
            }
        });
        subjectsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) { nameField.setText(newValue.getName()); colorPicker.setValue(hexToColor(newValue.getColor())); }
        });

        colorPicker.setValue(Color.LIGHTBLUE);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        try {
            subjectService.addSubject(nameField.getText(), colorToHex(colorPicker.getValue()));
            clearForm();
            refreshTable();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add subject", e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        Subject selected = subjectsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Select a subject to edit first.");
            return;
        }
        try {
            selected.setName(nameField.getText());
            selected.setColor(colorToHex(colorPicker.getValue()));
            subjectService.updateSubject(selected);
            refreshTable();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update subject", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Subject selected = subjectsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Select a subject to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete subject \"" + selected.getName() + "\"? This cannot be undone.");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            subjectService.deleteSubject(selected.getId());
            clearForm();
            refreshTable();
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Cannot delete subject", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void refreshTable() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            subjects.setAll(all);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.clear();
        colorPicker.setValue(Color.LIGHTBLUE);
        subjectsList.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
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
