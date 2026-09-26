package com.revisionassistant.controller;

import com.revisionassistant.model.Resource;
import com.revisionassistant.model.Subject;
import com.revisionassistant.service.ResourceService;
import com.revisionassistant.service.SubjectService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource organizer - lets the user save useful study links,
 * organized by subject and category. Persisted per-user through
 * {@link ResourceService}, the same way every other feature in the
 * app stores its data.
 */
public class ResourcesController {

    @FXML private TextField titleField;
    @FXML private TextField urlField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<Subject> subjectCombo;
    @FXML private VBox resourcesBox;
    @FXML private ComboBox<Subject> filterSubjectCombo;
    @FXML private ComboBox<String> filterCategoryCombo;
    @FXML private Label emptyStateLabel;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private final List<Resource> resources = new ArrayList<>();
    private final SubjectService subjectService = new SubjectService();
    private final ResourceService resourceService = new ResourceService();
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final Map<Integer, Subject> subjectsById = new HashMap<>();

    private static final List<String> CATEGORIES = List.of(
            "Documentation", "Tutorial", "Article", "Video", "Course", "Reference", "Other"
    );

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(CATEGORIES));
        categoryCombo.setValue("Documentation");
        filterCategoryCombo.getItems().add(null);
        filterCategoryCombo.getItems().addAll(CATEGORIES);
        filterCategoryCombo.setConverter(new javafx.util.StringConverter<String>() {
            @Override public String toString(String s) { return s == null ? "All Categories" : s; }
            @Override public String fromString(String s) { return s; }
        });

        loadSubjects();
        loadResources();
    }

    private void loadSubjects() {
        try {
            subjects.setAll(subjectService.getAllSubjects());
            subjectsById.clear();
            for (Subject subject : subjects) {
                subjectsById.put(subject.getId(), subject);
            }
            subjectCombo.setItems(subjects);
            subjectCombo.setConverter(subjectConverter(null));

            filterSubjectCombo.getItems().clear();
            filterSubjectCombo.getItems().add(null);
            filterSubjectCombo.getItems().addAll(subjects);
            filterSubjectCombo.setConverter(subjectConverter("All Subjects"));
            filterSubjectCombo.valueProperty().addListener((obs, o, n) -> refreshDisplay());
            filterCategoryCombo.valueProperty().addListener((obs, o, n) -> refreshDisplay());
        } catch (SQLException e) {
            statusLabel.setText("Could not load subjects.");
        }
    }

    private void loadResources() {
        try {
            resources.clear();
            resources.addAll(resourceService.getAllResources());
        } catch (SQLException e) {
            setStatus("Could not load saved resources.", false);
        }
        refreshDisplay();
    }

    @FXML
    private void handleAddResource() {
        String title = titleField.getText().trim();
        String url = urlField.getText().trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        String description = descriptionField.getText().trim();
        String category = categoryCombo.getValue();
        Subject subject = subjectCombo.getValue();

        try {
            resourceService.addResource(subject == null ? null : subject.getId(), title, url, description, category);
            titleField.clear(); urlField.clear(); descriptionField.clear();
            setStatus("Resource saved.", true);
            loadResources();
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage(), false);
        } catch (SQLException e) {
            setStatus("Could not save resource: " + e.getMessage(), false);
        }
    }

    private void refreshDisplay() {
        Subject filterSubject = filterSubjectCombo.getValue();
        String filterCategory = filterCategoryCombo.getValue();

        List<Resource> filtered = new ArrayList<>();
        for (Resource r : resources) {
            if (filterSubject != null && (r.getSubjectId() == null || r.getSubjectId() != filterSubject.getId())) continue;
            if (filterCategory != null && !filterCategory.equals(r.getCategory())) continue;
            filtered.add(r);
        }

        resourcesBox.getChildren().clear();
        if (filtered.isEmpty()) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            countLabel.setText("");
        } else {
            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            countLabel.setText(filtered.size() + " resource" + (filtered.size() == 1 ? "" : "s"));
            for (Resource r : filtered) {
                resourcesBox.getChildren().add(buildResourceCard(r));
            }
        }
    }

    private VBox buildResourceCard(Resource r) {
        VBox card = new VBox(6);
        card.getStyleClass().add("resource-card");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(r.getCategory() == null ? "Other" : r.getCategory());
        categoryBadge.getStyleClass().add("badge-accent");

        Label titleLabel = new Label(r.getTitle());
        titleLabel.getStyleClass().add("resource-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Subject subject = r.getSubjectId() == null ? null : subjectsById.get(r.getSubjectId());
        Label subjectLabel = new Label(subject == null ? "No subject" : subject.getName());
        subjectLabel.getStyleClass().add("row-meta");

        topRow.getChildren().addAll(categoryBadge, titleLabel, subjectLabel);

        String url = r.getUrl();
        Hyperlink link = new Hyperlink(url.length() > 60 ? url.substring(0, 57) + "..." : url);
        link.getStyleClass().add("resource-link");
        link.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ex) {
                setStatus("Could not open link. Copy it manually: " + url, false);
            }
        });

        card.getChildren().addAll(topRow, link);
        if (r.getDescription() != null && !r.getDescription().isEmpty()) {
            Label desc = new Label(r.getDescription());
            desc.getStyleClass().add("row-meta");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setOnAction(e -> {
            try {
                resourceService.deleteResource(r.getId());
                loadResources();
            } catch (SQLException ex) {
                setStatus("Could not remove resource: " + ex.getMessage(), false);
            }
        });
        HBox footer = new HBox(removeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().add(footer);

        return card;
    }

    private void setStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(success ? "-fx-text-fill: #34D399;" : "-fx-text-fill: #FB7185;");
    }

    private javafx.util.StringConverter<Subject> subjectConverter(String nullLabel) {
        return new javafx.util.StringConverter<Subject>() {
            @Override public String toString(Subject s) { return s == null ? (nullLabel == null ? "" : nullLabel) : s.getName(); }
            @Override public Subject fromString(String s) { return null; }
        };
    }
}
