package com.revisionassistant.controller;

import com.revisionassistant.dto.FlashcardImportDTO;
import com.revisionassistant.dto.ImportedFlashcardDTO;
import com.revisionassistant.model.Flashcard;
import com.revisionassistant.model.RevisionStatus;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.JsonImportException;
import com.revisionassistant.service.JsonImportService;
import com.revisionassistant.service.FlashcardService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for FlashcardView.fxml. Handles UI events, basic
 * filtering and the study/browse panel only - all validation and
 * persistence goes through {@link FlashcardService}, {@link SubjectService}
 * and {@link TopicService}.
 */
public class FlashcardController {

    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<Topic> topicComboBox;
    @FXML
    private TextArea frontArea;
    @FXML
    private TextArea backArea;
    @FXML
    private CheckBox difficultCheckBox;
    @FXML
    private ComboBox<RevisionStatus> revisionStatusComboBox;

    @FXML
    private ComboBox<Subject> filterSubjectComboBox;
    @FXML
    private ComboBox<Boolean> filterDifficultComboBox;
    @FXML
    private ComboBox<RevisionStatus> filterStatusComboBox;

    @FXML
    private TableView<Flashcard> flashcardsTable;
    @FXML
    private TableColumn<Flashcard, String> frontColumn;
    @FXML
    private TableColumn<Flashcard, String> subjectColumn;
    @FXML
    private TableColumn<Flashcard, String> topicColumn;
    @FXML
    private TableColumn<Flashcard, Boolean> difficultColumn;
    @FXML
    private TableColumn<Flashcard, String> statusColumn;

    @FXML
    private Label studyFrontLabel;
    @FXML
    private Label studyBackLabel;
    @FXML
    private Label studyPositionLabel;

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final FlashcardService flashcardService = new FlashcardService();
    private final JsonImportService jsonImportService = new JsonImportService();

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Flashcard> flashcards = FXCollections.observableArrayList();

    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

    private int studyIndex = -1;
    private boolean studyShowingBack = false;

    @FXML
    public void initialize() {
        setUpFormControls();
        setUpFilterControls();
        setUpTable();
        refreshSubjects();
        refreshFlashcards();
        updateStudyPanel();
    }

    private void setUpFormControls() {
        subjectComboBox.setItems(subjects);
        subjectComboBox.setConverter(subjectConverter(null));
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshTopicChoices());

        revisionStatusComboBox.setItems(FXCollections.observableArrayList(RevisionStatus.values()));
        revisionStatusComboBox.setValue(RevisionStatus.NOT_STARTED);
    }

    private void setUpFilterControls() {
        ObservableList<Subject> filterSubjects = FXCollections.observableArrayList();
        filterSubjects.add(null);
        filterSubjectComboBox.setItems(filterSubjects);
        filterSubjectComboBox.setConverter(subjectConverter("All subjects"));
        filterSubjectComboBox.setValue(null);
        filterSubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        ObservableList<Boolean> difficultOptions = FXCollections.observableArrayList();
        difficultOptions.add(null);
        difficultOptions.add(Boolean.TRUE);
        difficultOptions.add(Boolean.FALSE);
        filterDifficultComboBox.setItems(difficultOptions);
        filterDifficultComboBox.setConverter(new StringConverter<Boolean>() {
            @Override
            public String toString(Boolean value) {
                if (value == null) {
                    return "All cards";
                }
                return value ? "Difficult only" : "Not difficult";
            }

            @Override
            public Boolean fromString(String string) {
                return null;
            }
        });
        filterDifficultComboBox.setValue(null);
        filterDifficultComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        ObservableList<RevisionStatus> statusOptions = FXCollections.observableArrayList();
        statusOptions.add(null);
        statusOptions.addAll(RevisionStatus.values());
        filterStatusComboBox.setItems(statusOptions);
        filterStatusComboBox.setConverter(new StringConverter<RevisionStatus>() {
            @Override
            public String toString(RevisionStatus status) {
                return status == null ? "All statuses" : status.getLabel();
            }

            @Override
            public RevisionStatus fromString(String string) {
                return null;
            }
        });
        filterStatusComboBox.setValue(null);
        filterStatusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void setUpTable() {
        frontColumn.setCellValueFactory(data -> new SimpleStringProperty(truncate(data.getValue().getFront())));

        subjectColumn.setCellValueFactory(data -> {
            Subject subject = subjectsById.get(data.getValue().getSubjectId());
            return new SimpleStringProperty(subject == null ? "—" : subject.getName());
        });

        topicColumn.setCellValueFactory(data -> {
            Integer topicId = data.getValue().getTopicId();
            Topic topic = topicId == null ? null : topicsById.get(topicId);
            return new SimpleStringProperty(topic == null ? "—" : topic.getName());
        });

        difficultColumn.setCellValueFactory(data -> {
            Flashcard card = data.getValue();
            SimpleBooleanProperty property = new SimpleBooleanProperty(card.isDifficult());
            property.addListener((obs, oldVal, newVal) -> {
                try {
                    flashcardService.setDifficult(card.getId(), newVal);
                    card.setDifficult(newVal);
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
                }
            });
            return property;
        });
        difficultColumn.setCellFactory(CheckBoxTableCell.forTableColumn(difficultColumn));

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRevisionStatus().getLabel()));

        flashcardsTable.setItems(flashcards);
        flashcardsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                populateForm(newValue);
                studyIndex = flashcards.indexOf(newValue);
                studyShowingBack = false;
                updateStudyPanel();
            }
        });
    }

    private void populateForm(Flashcard card) {
        subjectComboBox.setValue(subjectsById.get(card.getSubjectId()));
        refreshTopicChoices();
        Integer topicId = card.getTopicId();
        topicComboBox.setValue(topicId == null ? null : topicsById.get(topicId));
        frontArea.setText(card.getFront());
        backArea.setText(card.getBack());
        difficultCheckBox.setSelected(card.isDifficult());
        revisionStatusComboBox.setValue(card.getRevisionStatus());
    }

    @FXML
    private void handleAddFlashcard() {
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            flashcardService.addFlashcard(
                    subject == null ? 0 : subject.getId(),
                    topic == null ? null : topic.getId(),
                    frontArea.getText(),
                    backArea.getText());
            clearForm();
            refreshFlashcards();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not add flashcard", e.getMessage());
        }
    }

    @FXML
    private void handleImportJson() {
        Subject subject = subjectComboBox.getValue();
        if (subject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected",
                    "Choose a subject before importing JSON.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import flashcards from JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        java.io.File file = chooser.showOpenDialog(flashcardsTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        FlashcardImportDTO document;
        try {
            document = jsonImportService.readFlashcards(file);
        } catch (JsonImportException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid flashcard JSON", e.getMessage());
            return;
        }

        Topic topic;
        try {
            topic = resolveImportedTopic(subject, document.getTopic());
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid topic", e.getMessage());
            return;
        }

        List<ImportedFlashcardDTO> accepted = showImportedFlashcardsDialog(document.getFlashcards());
        if (accepted == null || accepted.isEmpty()) {
            return;
        }

        int saved = 0;
        for (ImportedFlashcardDTO card : accepted) {
            try {
                flashcardService.addFlashcard(subject.getId(), topic.getId(),
                        card.getQuestion(), card.getAnswer());
                saved++;
            } catch (IllegalArgumentException | SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Could not save imported flashcard", e.getMessage());
                break;
            }
        }
        refreshFlashcards();
        if (saved > 0) {
            showAlert(Alert.AlertType.INFORMATION, "Import complete",
                    saved + " flashcard(s) were added.");
        }
    }

    @FXML
    private void handleCopyJsonPrompt() {
        ClipboardContent content = new ClipboardContent();
        content.putString(JsonImportService.flashcardPrompt());
        Clipboard.getSystemClipboard().setContent(content);
        showAlert(Alert.AlertType.INFORMATION, "Prompt copied",
                "The flashcard JSON prompt was copied to the clipboard.");
    }

    private Topic resolveImportedTopic(Subject subject, String topicName) throws SQLException {
        String imported = topicName == null ? "" : topicName.trim();
        Topic selected = topicComboBox.getValue();
        if (selected != null && !selected.getName().equalsIgnoreCase(imported)) {
            throw new IllegalArgumentException(
                    "The JSON topic \"" + imported + "\" does not match the selected topic \""
                            + selected.getName() + "\".");
        }
        for (Topic topic : topicService.getTopicsForSubject(subject.getId())) {
            if (topic.getName().equalsIgnoreCase(imported)) {
                return topic;
            }
        }
        throw new IllegalArgumentException(
                "Topic \"" + imported + "\" does not exist for the selected subject.");
    }

    private List<ImportedFlashcardDTO> showImportedFlashcardsDialog(List<ImportedFlashcardDTO> imported) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/revisionassistant/fxml/ImportedFlashcardsDialog.fxml"));
            Parent root = loader.load();
            ImportedFlashcardsDialogController controller = loader.getController();
            controller.setCards(imported);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Preview imported flashcards");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            return controller.isConfirmed() ? controller.getSelectedCards() : null;
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Could not open preview",
                    "Could not show the imported flashcards: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void handleEditFlashcard() {
        Flashcard selected = flashcardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No flashcard selected", "Select a flashcard to edit first.");
            return;
        }
        try {
            Subject subject = subjectComboBox.getValue();
            Topic topic = topicComboBox.getValue();
            selected.setSubjectId(subject == null ? 0 : subject.getId());
            selected.setTopicId(topic == null ? null : topic.getId());
            selected.setFront(frontArea.getText());
            selected.setBack(backArea.getText());
            selected.setDifficult(difficultCheckBox.isSelected());
            selected.setRevisionStatus(revisionStatusComboBox.getValue());
            flashcardService.updateFlashcard(selected);
            refreshFlashcards();
        } catch (IllegalArgumentException | SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Could not update flashcard", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteFlashcard() {
        Flashcard selected = flashcardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No flashcard selected", "Select a flashcard to delete first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this flashcard?");
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().filter(response -> response == ButtonType.OK).isEmpty()) {
            return;
        }

        try {
            flashcardService.deleteFlashcard(selected.getId());
            clearForm();
            refreshFlashcards();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        filterSubjectComboBox.setValue(null);
        filterDifficultComboBox.setValue(null);
        filterStatusComboBox.setValue(null);
    }

    // ----- Study / browse panel ---------------------------------------

    @FXML
    private void handleShowAnswer() {
        if (studyIndex < 0) {
            return;
        }
        studyShowingBack = !studyShowingBack;
        updateStudyPanel();
    }

    @FXML
    private void handlePreviousCard() {
        if (flashcards.isEmpty()) {
            return;
        }
        studyIndex = studyIndex <= 0 ? flashcards.size() - 1 : studyIndex - 1;
        studyShowingBack = false;
        flashcardsTable.getSelectionModel().select(studyIndex);
    }

    @FXML
    private void handleNextCard() {
        if (flashcards.isEmpty()) {
            return;
        }
        studyIndex = studyIndex >= flashcards.size() - 1 ? 0 : studyIndex + 1;
        studyShowingBack = false;
        flashcardsTable.getSelectionModel().select(studyIndex);
    }

    private void updateStudyPanel() {
        if (flashcards.isEmpty() || studyIndex < 0 || studyIndex >= flashcards.size()) {
            studyFrontLabel.setText("Add or select a flashcard to start studying.");
            studyBackLabel.setText("");
            studyPositionLabel.setText("");
            return;
        }
        Flashcard card = flashcards.get(studyIndex);
        studyFrontLabel.setText(card.getFront());
        studyBackLabel.setText(studyShowingBack ? card.getBack() : "");
        studyPositionLabel.setText((studyIndex + 1) + " of " + flashcards.size());
    }

    private void applyFilters() {
        Subject subject = filterSubjectComboBox.getValue();
        Boolean difficult = filterDifficultComboBox.getValue();
        RevisionStatus status = filterStatusComboBox.getValue();

        try {
            List<Flashcard> filtered = flashcardService.getFilteredFlashcards(
                    subject == null ? null : subject.getId(), difficult, status);
            flashcards.setAll(filtered);
            studyIndex = flashcards.isEmpty() ? -1 : 0;
            studyShowingBack = false;
            updateStudyPanel();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load flashcards: " + e.getMessage());
        }
    }

    private void refreshSubjects() {
        try {
            List<Subject> all = subjectService.getAllSubjects();
            subjects.setAll(all);
            subjectsById.clear();
            for (Subject subject : all) {
                subjectsById.put(subject.getId(), subject);
            }

            filterSubjectComboBox.getItems().setAll(subjects);
            filterSubjectComboBox.getItems().add(0, null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load subjects: " + e.getMessage());
        }
    }

    private void refreshTopicChoices() {
        Subject subject = subjectComboBox.getValue();
        ObservableList<Topic> topics = FXCollections.observableArrayList();
        topics.add(null);
        if (subject != null) {
            try {
                topics.addAll(topicService.getTopicsForSubject(subject.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
            }
        }
        topicComboBox.setItems(topics);
        topicComboBox.setConverter(new StringConverter<Topic>() {
            @Override
            public String toString(Topic topic) {
                return topic == null ? "No specific topic" : topic.getName();
            }

            @Override
            public Topic fromString(String string) {
                return topicComboBox.getValue();
            }
        });
    }

    private void refreshFlashcards() {
        rebuildTopicIndex();
        applyFilters();
    }

    private void rebuildTopicIndex() {
        topicsById.clear();
        try {
            for (Subject subject : subjects) {
                List<Topic> topics = topicService.getTopicsForSubject(subject.getId());
                for (Topic topic : topics) {
                    topicsById.put(topic.getId(), topic);
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
        }
    }

    private void clearForm() {
        subjectComboBox.setValue(null);
        refreshTopicChoices();
        frontArea.clear();
        backArea.clear();
        difficultCheckBox.setSelected(false);
        revisionStatusComboBox.setValue(RevisionStatus.NOT_STARTED);
        flashcardsTable.getSelectionModel().clearSelection();
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replace("\n", " ");
        return singleLine.length() > 60 ? singleLine.substring(0, 57) + "..." : singleLine;
    }

    private StringConverter<Subject> subjectConverter(String nullLabel) {
        return new StringConverter<Subject>() {
            @Override
            public String toString(Subject subject) {
                if (subject == null) {
                    return nullLabel == null ? "" : nullLabel;
                }
                return subject.getName();
            }

            @Override
            public Subject fromString(String string) {
                return subjectComboBox.getValue();
            }
        };
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
