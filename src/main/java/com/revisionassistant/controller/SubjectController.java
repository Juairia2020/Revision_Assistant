package com.revisionassistant.controller;

import com.revisionassistant.model.Subject;
import com.revisionassistant.service.SubjectService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for SubjectView.fxml. Handles UI events only - all
 * validation and persistence goes through {@link SubjectService}.
 */
public class SubjectController {

    @FXML
    private TableView<Subject> subjectsTable;
    @FXML
    private TableColumn<Subject, Integer> idColumn;
    @FXML
    private TableColumn<Subject, String> nameColumn;
    @FXML
    private TableColumn<Subject, String> colorColumn;

    @FXML
    private TextField nameField;
    @FXML
    private ColorPicker colorPicker;

    private final SubjectService subjectService = new SubjectService();
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
        colorColumn.setCellFactory(column -> new ColorSwatchCell());

        subjectsTable.setItems(subjects);
        subjectsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                nameField.setText(newValue.getName());
                colorPicker.setValue(hexToColor(newValue.getColor()));
            }
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
        Subject selected = subjectsTable.getSelectionModel().getSelectedItem();
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
        Subject selected = subjectsTable.getSelectionModel().getSelectedItem();
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
        subjectsTable.getSelectionModel().clearSelection();
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

    /** Renders the stored hex color as a small colored swatch plus its code. */
    private static class ColorSwatchCell extends TableCell<Subject, String> {
        @Override
        protected void updateItem(String hex, boolean empty) {
            super.updateItem(hex, empty);
            if (empty || hex == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(hex);
            Region swatch = new Region();
            swatch.setPrefSize(14, 14);
            swatch.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 3;");
            setGraphic(swatch);
        }
    }
}
