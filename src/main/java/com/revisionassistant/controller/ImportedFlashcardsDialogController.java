package com.revisionassistant.controller;

import com.revisionassistant.dto.ImportedFlashcardDTO;
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

public class ImportedFlashcardsDialogController {
    @FXML private TableView<ImportedFlashcardDTO> cardsTable;
    @FXML private TableColumn<ImportedFlashcardDTO, Boolean> selectColumn;
    @FXML private TableColumn<ImportedFlashcardDTO, String> questionColumn;
    @FXML private TableColumn<ImportedFlashcardDTO, String> answerColumn;
    @FXML private Button saveButton;

    private final ObservableList<ImportedFlashcardDTO> cards = FXCollections.observableArrayList();
    private boolean confirmed;

    @FXML public void initialize() {
        selectColumn.setCellValueFactory(data -> {
            ImportedFlashcardDTO card = data.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(card.isSelected());
            property.addListener((obs, oldValue, newValue) -> card.setSelected(newValue));
            return property;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestion()));
        answerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAnswer()));
        cardsTable.setItems(cards);
        cardsTable.setEditable(true);
    }

    public void setCards(List<ImportedFlashcardDTO> values) { cards.setAll(values); }

    @FXML private void handleSelectAll() { cards.forEach(c -> c.setSelected(true)); cardsTable.refresh(); }
    @FXML private void handleSelectNone() { cards.forEach(c -> c.setSelected(false)); cardsTable.refresh(); }
    @FXML private void handleConfirm() { confirmed = true; close(); }
    @FXML private void handleCancel() { confirmed = false; close(); }
    private void close() { ((Stage) saveButton.getScene().getWindow()).close(); }

    public boolean isConfirmed() { return confirmed; }
    public List<ImportedFlashcardDTO> getSelectedCards() {
        return cards.stream().filter(ImportedFlashcardDTO::isSelected).collect(Collectors.toList());
    }
}
