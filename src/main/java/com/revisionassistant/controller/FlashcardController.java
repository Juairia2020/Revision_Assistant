package com.revisionassistant.controller;

import com.revisionassistant.dto.FlashcardImportDTO;
import com.revisionassistant.dto.ImportedFlashcardDTO;
import com.revisionassistant.model.Flashcard;
import com.revisionassistant.model.RevisionStatus;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.service.JsonImportException;
import com.revisionassistant.util.DialogStyler;
import com.revisionassistant.service.JsonImportService;
import com.revisionassistant.service.FlashcardService;
import com.revisionassistant.service.SubjectService;
import com.revisionassistant.service.TopicService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.concurrent.Task;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
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
    private FlowPane cardsPane;

    // ----- Study Cards tab ----------------------------------------------

    @FXML
    private ComboBox<Subject> studySubjectComboBox;
    @FXML
    private ComboBox<Topic> studyTopicComboBox;
    @FXML
    private ComboBox<RevisionStatus> studyStatusComboBox;
    @FXML
    private Label studyStatusLabel;
    @FXML
    private VBox studyPanel;
    @FXML
    private StackPane flipCardStack;
    @FXML
    private VBox flipCardFront;
    @FXML
    private VBox flipCardBack;
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

    @FXML
    private javafx.scene.control.Button importJsonButton;
    @FXML
    private ProgressIndicator importProgressIndicator;
    @FXML
    private Label importStatusLabel;

    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ObservableList<Flashcard> flashcards = FXCollections.observableArrayList();
    private final ObservableList<Flashcard> studyCards = FXCollections.observableArrayList();

    private final Map<Integer, Subject> subjectsById = new HashMap<>();
    private final Map<Integer, Topic> topicsById = new HashMap<>();

    /** Index into {@code flashcards}, for the Manage Cards form/edit selection. */
    private int selectedIndex = -1;

    /** Index into {@code studyCards} and its flip state, for the Study Cards tab. */
    private int studyCardIndex = -1;
    private boolean studyShowingBack = false;

    @FXML
    public void initialize() {
        setUpFormControls();
        setUpFilterControls();
        setUpCardLibrary();
        setUpStudyControls();
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

    private void setUpStudyControls() {
        studySubjectComboBox.setItems(subjects);
        studySubjectComboBox.setConverter(subjectConverter(null));
        studySubjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshStudyTopicChoices());

        ObservableList<Topic> emptyTopics = FXCollections.observableArrayList();
        emptyTopics.add(null);
        studyTopicComboBox.setItems(emptyTopics);
        studyTopicComboBox.setConverter(new StringConverter<Topic>() {
            @Override
            public String toString(Topic topic) {
                return topic == null ? "All topics" : topic.getName();
            }

            @Override
            public Topic fromString(String string) {
                return studyTopicComboBox.getValue();
            }
        });

        ObservableList<RevisionStatus> statusOptions = FXCollections.observableArrayList();
        statusOptions.add(null);
        statusOptions.addAll(RevisionStatus.values());
        studyStatusComboBox.setItems(statusOptions);
        studyStatusComboBox.setConverter(new StringConverter<RevisionStatus>() {
            @Override
            public String toString(RevisionStatus status) {
                return status == null ? "All statuses" : status.getLabel();
            }

            @Override
            public RevisionStatus fromString(String string) {
                return studyStatusComboBox.getValue();
            }
        });
        studyStatusComboBox.setValue(null);
    }

    private void refreshStudyTopicChoices() {
        Subject subject = studySubjectComboBox.getValue();
        ObservableList<Topic> topics = FXCollections.observableArrayList();
        topics.add(null);
        if (subject != null) {
            try {
                topics.addAll(topicService.getTopicsForSubject(subject.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database error", "Could not load topics: " + e.getMessage());
            }
        }
        studyTopicComboBox.setItems(topics);
        studyTopicComboBox.setValue(null);
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

    private void setUpCardLibrary() {
        cardsPane.setHgap(14);
        cardsPane.setVgap(14);
        cardsPane.setPrefWrapLength(760);
        cardsPane.widthProperty().addListener((obs, oldValue, newValue) ->
                cardsPane.setPrefWrapLength(Math.max(420, newValue.doubleValue() - 24)));
        cardsPane.setStyle("-fx-alignment: TOP_LEFT;");
    }

    private void rebuildCardLibrary() {
        cardsPane.getChildren().clear();
        for (int i = 0; i < flashcards.size(); i++) {
            Flashcard card = flashcards.get(i);
            VBox tile = new VBox(8);
            tile.getStyleClass().add("flashcard-tile");
            tile.setPrefWidth(250);
            tile.setMinHeight(150);
            Label badge = new Label(card.isDifficult() ? "DIFFICULT" : card.getRevisionStatus().getLabel());
            badge.getStyleClass().add(card.isDifficult() ? "badge-warning" : "badge-accent");
            Label front = new Label(truncate(card.getFront()));
            front.setWrapText(true);
            front.getStyleClass().add("flashcard-tile-front");
            Label meta = new Label((subjectsById.get(card.getSubjectId()) == null ? "" : subjectsById.get(card.getSubjectId()).getName())
                    + (card.getTopicId() == null ? "" : " • " + (topicsById.get(card.getTopicId()) == null ? "" : topicsById.get(card.getTopicId()).getName())));
            meta.getStyleClass().add("row-meta");
            tile.getChildren().addAll(badge, front, meta);
            int finalI = i;
            tile.setOnMouseClicked(e -> selectFlashcard(finalI));
            cardsPane.getChildren().add(tile);
        }
    }

    private void selectFlashcard(int index) {
        if (index < 0 || index >= flashcards.size()) return;
        selectedIndex = index;
        populateForm(flashcards.get(index));
        for (int i = 0; i < cardsPane.getChildren().size(); i++) {
            cardsPane.getChildren().get(i).pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), i == index);
        }
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
        java.io.File file = chooser.showOpenDialog(cardsPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        importJsonButton.setDisable(true);
        importProgressIndicator.setVisible(true);
        importStatusLabel.setText("Reading and validating JSON…");

        final Task<FlashcardImportDTO> task = new Task<>() {
            @Override
            protected FlashcardImportDTO call() throws Exception {
                if (isCancelled()) {
                    return null;
                }
                return jsonImportService.readFlashcards(file);
            }
        };

        task.setOnSucceeded(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");

            FlashcardImportDTO document = task.getValue();
            if (document == null) {
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
        });

        task.setOnFailed(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");
            Throwable error = task.getException();
            String message = error instanceof JsonImportException
                    ? error.getMessage()
                    : "Could not process the JSON file.";
            showAlert(Alert.AlertType.ERROR, "Invalid flashcard JSON", message);
        });

        task.setOnCancelled(event -> {
            importJsonButton.setDisable(false);
            importProgressIndicator.setVisible(false);
            importStatusLabel.setText("");
        });

        Thread worker = new Thread(task, "flashcard-json-import");
        worker.setDaemon(true);
        worker.start();
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
        Flashcard selected = getSelectedFlashcard();
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
        Flashcard selected = getSelectedFlashcard();
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

    // ----- Manage Cards: selection for the edit form -----------------------

    private Flashcard getSelectedFlashcard() {
        return selectedIndex >= 0 && selectedIndex < flashcards.size() ? flashcards.get(selectedIndex) : null;
    }

    private void clearSelectedFlashcard() {
        selectedIndex = -1;
        for (var node : cardsPane.getChildren()) node.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), false);
    }

    private void applyFilters() {
        Subject subject = filterSubjectComboBox.getValue();
        Boolean difficult = filterDifficultComboBox.getValue();
        RevisionStatus status = filterStatusComboBox.getValue();

        try {
            List<Flashcard> filtered = flashcardService.getFilteredFlashcards(
                    subject == null ? null : subject.getId(), difficult, status);
            flashcards.setAll(filtered);
            selectedIndex = -1;
            rebuildCardLibrary();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load flashcards: " + e.getMessage());
        }
    }

    // ----- Study Cards tab: subject/topic pick + one-at-a-time flip card ---

    @FXML
    private void handleStartStudying() {
        Subject subject = studySubjectComboBox.getValue();
        if (subject == null) {
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Choose a subject to study first.");
            return;
        }

        Topic topic = studyTopicComboBox.getValue();
        RevisionStatus status = studyStatusComboBox.getValue();
        try {
            List<Flashcard> matches = flashcardService.getFilteredFlashcards(subject.getId(), null, status);
            if (topic != null) {
                List<Flashcard> byTopic = new java.util.ArrayList<>();
                for (Flashcard card : matches) {
                    if (card.getTopicId() != null && card.getTopicId().intValue() == topic.getId()) {
                        byTopic.add(card);
                    }
                }
                matches = byTopic;
            }
            studyCards.setAll(matches);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not load flashcards: " + e.getMessage());
            return;
        }

        if (studyCards.isEmpty()) {
            studyPanel.setVisible(false);
            studyPanel.setManaged(false);
            studyStatusLabel.setText(status == null
                    ? "No flashcards found for that selection yet."
                    : "No " + status.getLabel().toLowerCase() + " flashcards found for that selection.");
            return;
        }

        studyStatusLabel.setText(status == null
                ? "Studying " + studyCards.size() + " card" + (studyCards.size() == 1 ? "" : "s") + "."
                : "Studying " + studyCards.size() + " " + status.getLabel().toLowerCase()
                + " card" + (studyCards.size() == 1 ? "" : "s") + ".");
        studyPanel.setVisible(true);
        studyPanel.setManaged(true);
        studyCardIndex = 0;
        resetToFront();
        updateStudyPanel();
    }

    @FXML
    private void handleFlipCard() {
        if (studyCards.isEmpty()) {
            return;
        }
        animateFlip();
    }

    @FXML
    private void handlePreviousStudyCard() {
        if (studyCards.isEmpty()) {
            return;
        }
        studyCardIndex = studyCardIndex <= 0 ? studyCards.size() - 1 : studyCardIndex - 1;
        resetToFront();
        updateStudyPanel();
    }

    @FXML
    private void handleNextStudyCard() {
        if (studyCards.isEmpty()) {
            return;
        }
        studyCardIndex = studyCardIndex >= studyCards.size() - 1 ? 0 : studyCardIndex + 1;
        resetToFront();
        updateStudyPanel();
    }

    /** Plays a quick squash/un-squash rotation, swapping the visible face at the midpoint. */
    private void animateFlip() {
        Rotate rotate = new Rotate(0, Rotate.Y_AXIS);
        rotate.pivotXProperty().bind(flipCardStack.widthProperty().divide(2));
        rotate.pivotYProperty().bind(flipCardStack.heightProperty().divide(2));
        flipCardStack.getTransforms().setAll(rotate);

        Timeline halfway = new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(rotate.angleProperty(), 90, Interpolator.EASE_IN)));
        halfway.setOnFinished(event -> {
            studyShowingBack = !studyShowingBack;
            flipCardFront.setVisible(!studyShowingBack);
            flipCardBack.setVisible(studyShowingBack);
            flipCardFront.setManaged(true);
            flipCardBack.setManaged(true);

            Timeline secondHalf = new Timeline(new KeyFrame(Duration.millis(150),
                    new KeyValue(rotate.angleProperty(), 0, Interpolator.EASE_OUT)));
            secondHalf.play();
        });
        halfway.play();
    }

    private void resetToFront() {
        studyShowingBack = false;
        flipCardStack.getTransforms().clear();
        flipCardFront.setVisible(true);
        flipCardFront.setManaged(true);
        flipCardBack.setVisible(false);
        flipCardBack.setManaged(true);
    }

    private void updateStudyPanel() {
        if (studyCards.isEmpty() || studyCardIndex < 0 || studyCardIndex >= studyCards.size()) {
            studyFrontLabel.setText("");
            studyBackLabel.setText("");
            studyPositionLabel.setText("");
            return;
        }
        Flashcard card = studyCards.get(studyCardIndex);
        studyFrontLabel.setText(card.getFront());
        studyBackLabel.setText(card.getBack());
        studyPositionLabel.setText((studyCardIndex + 1) + " of " + studyCards.size());
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
        clearSelectedFlashcard();
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
        DialogStyler.style(alert);
        alert.showAndWait();
    }
}