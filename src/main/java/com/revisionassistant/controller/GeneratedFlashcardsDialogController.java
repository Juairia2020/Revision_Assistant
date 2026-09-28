package com.revisionassistant.controller;

import com.revisionassistant.dto.GeneratedFlashcardDTO;
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
 * Controller for GeneratedFlashcardsDialog.fxml - the "preview
 * generated cards" step of the API flashcard workflow. Shows every
 * card the AI proposed with a checkbox so the user can decide which
 * ones to keep; nothing here writes to the database. The caller reads
 * {@link #isConfirmed()} and {@link #getSelectedCards()} once the
 * dialog closes and saves the chosen cards itself, through the
 * existing {@code FlashcardService}.
 */
public class GeneratedFlashcardsDialogController {

    @FXML
    private TableView<GeneratedFlashcardDTO> cardsTable;
    @FXML
    private TableColumn<GeneratedFlashcardDTO, Boolean> selectColumn;
    @FXML
    private TableColumn<GeneratedFlashcardDTO, String> frontColumn;
    @FXML
    private TableColumn<GeneratedFlashcardDTO, String> backColumn;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private final ObservableList<GeneratedFlashcardDTO> cards = FXCollections.observableArrayList();
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        selectColumn.setCellValueFactory(data -> {
            GeneratedFlashcardDTO card = data.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(card.isSelected());
            property.addListener((obs, oldValue, newValue) -> card.setSelected(newValue));
            return property;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        frontColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFront()));
        backColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBack()));

        cardsTable.setItems(cards);
        cardsTable.setEditable(true);
    }

    /** Populates the preview table with the cards the AI proposed. */
    public void setCards(List<GeneratedFlashcardDTO> generatedCards) {
        cards.setAll(generatedCards);
    }

    @FXML
    private void handleSelectAll() {
        cards.forEach(card -> card.setSelected(true));
        cardsTable.refresh();
    }

    @FXML
    private void handleSelectNone() {
        cards.forEach(card -> card.setSelected(false));
        cardsTable.refresh();
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

    /** The cards the user left checked, in table order. Empty (never null) if none were selected. */
    public List<GeneratedFlashcardDTO> getSelectedCards() {
        return cards.stream().filter(GeneratedFlashcardDTO::isSelected).collect(Collectors.toList());
    }
}
