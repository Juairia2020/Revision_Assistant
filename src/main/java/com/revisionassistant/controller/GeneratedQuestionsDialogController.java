package com.revisionassistant.controller;

import com.revisionassistant.dto.GeneratedQuestionDTO;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for GeneratedQuestionsDialog.fxml - the "preview
 * generated questions" step of the API quiz workflow. Shows every
 * question the AI proposed, with all four options and the correct
 * one, and a checkbox so the user can decide which ones to keep;
 * nothing here writes to the database. The caller reads
 * {@link #isConfirmed()} and {@link #getSelectedQuestions()} once the
 * dialog closes and saves the chosen questions itself, through the
 * existing {@code QuizService}.
 */
public class GeneratedQuestionsDialogController {

    @FXML
    private TableView<GeneratedQuestionDTO> questionsTable;
    @FXML
    private TableColumn<GeneratedQuestionDTO, Boolean> selectColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> questionColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> optionAColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> optionBColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> optionCColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> optionDColumn;
    @FXML
    private TableColumn<GeneratedQuestionDTO, String> correctColumn;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private final ObservableList<GeneratedQuestionDTO> questions = FXCollections.observableArrayList();
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        selectColumn.setCellValueFactory(data -> {
            GeneratedQuestionDTO question = data.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(question.isSelected());
            property.addListener((obs, oldValue, newValue) -> question.setSelected(newValue));
            return property;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestion()));
        optionAColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionA()));
        optionBColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionB()));
        optionCColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionC()));
        optionDColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionD()));
        correctColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCorrectOption()));

        questionsTable.setItems(questions);
        questionsTable.setEditable(true);
    }

    /** Populates the preview table with the questions the AI proposed. */
    public void setQuestions(List<GeneratedQuestionDTO> generatedQuestions) {
        questions.setAll(generatedQuestions);
    }

    @FXML
    private void handleSelectAll() {
        questions.forEach(question -> question.setSelected(true));
        questionsTable.refresh();
    }

    @FXML
    private void handleSelectNone() {
        questions.forEach(question -> question.setSelected(false));
        questionsTable.refresh();
    }

    @FXML
    private void handleSave() {
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    /** True if the user clicked "Save Selected" rather than "Cancel" or closing the window. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** The questions the user left checked, in table order. Empty (never null) if none were selected. */
    public List<GeneratedQuestionDTO> getSelectedQuestions() {
        return questions.stream().filter(GeneratedQuestionDTO::isSelected).collect(Collectors.toList());
    }
}
