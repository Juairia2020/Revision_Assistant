package com.revisionassistant.controller;

import com.revisionassistant.dto.ImportedQuizQuestionDTO;
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

public class ImportedQuestionsDialogController {
    @FXML private TableView<ImportedQuizQuestionDTO> questionsTable;
    @FXML private TableColumn<ImportedQuizQuestionDTO, Boolean> selectColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> questionColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> optionAColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> optionBColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> optionCColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> optionDColumn;
    @FXML private TableColumn<ImportedQuizQuestionDTO, String> correctColumn;
    @FXML private Button confirmButton;

    private final ObservableList<ImportedQuizQuestionDTO> questions = FXCollections.observableArrayList();
    private boolean confirmed;

    @FXML public void initialize() {
        selectColumn.setCellValueFactory(data -> {
            ImportedQuizQuestionDTO q = data.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(q.isSelected());
            property.addListener((obs, oldValue, newValue) -> q.setSelected(newValue));
            return property;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestion()));
        optionAColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionA()));
        optionBColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionB()));
        optionCColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionC()));
        optionDColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOptionD()));
        correctColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCorrectOptionLetter()));
        questionsTable.setItems(questions);
        questionsTable.setEditable(true);
    }

    public void setQuestions(List<ImportedQuizQuestionDTO> values) { questions.setAll(values); }

    @FXML private void handleSelectAll() { questions.forEach(q -> q.setSelected(true)); questionsTable.refresh(); }
    @FXML private void handleSelectNone() { questions.forEach(q -> q.setSelected(false)); questionsTable.refresh(); }
    @FXML private void handleConfirm() { confirmed = true; close(); }
    @FXML private void handleCancel() { confirmed = false; close(); }
    private void close() { ((Stage) confirmButton.getScene().getWindow()).close(); }

    public boolean isConfirmed() { return confirmed; }
    public List<ImportedQuizQuestionDTO> getSelectedQuestions() {
        return questions.stream().filter(ImportedQuizQuestionDTO::isSelected).collect(Collectors.toList());
    }
}
