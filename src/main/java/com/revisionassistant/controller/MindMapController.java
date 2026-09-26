package com.revisionassistant.controller;

import com.revisionassistant.dao.MindMapDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.*;

/** Interactive, persistent mind map where every node can become the focus/central node. */
public class MindMapController {

    @FXML private TextField mapNameField;
    @FXML private ComboBox<MindMapDAO.MapRecord> savedMapsCombo;
    @FXML private TextField rootTopicField;
    @FXML private TextField newNodeField;
    @FXML private Label selectedNodeLabel;
    @FXML private Label nodeCountLabel;
    @FXML private Label statusLabel;
    @FXML private Pane mapPane;
    @FXML private Canvas connectionCanvas;
    @FXML private Label emptyStateLabel;

    private static class MapNode {
        final String key;
        String label;
        String parentKey;
        double x, y;
        StackPane ui;

        MapNode(String key, String label, String parentKey, double x, double y) {
            this.key = key;
            this.label = label;
            this.parentKey = parentKey;
            this.x = x;
            this.y = y;
        }
    }

    private final MindMapDAO mindMapDAO = new MindMapDAO();
    private final LinkedHashMap<String, MapNode> nodes = new LinkedHashMap<>();
    private int currentMapId;
    private MapNode focusNode;
    private boolean loading;

    @FXML
    public void initialize() {
        savedMapsCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MindMapDAO.MapRecord map) { return map == null ? "Saved mind maps" : map.name(); }
            @Override public MindMapDAO.MapRecord fromString(String string) { return savedMapsCombo.getValue(); }
        });
        savedMapsCombo.valueProperty().addListener((obs, old, selected) -> {
            if (!loading && selected != null) loadMap(selected);
        });
        mapPane.widthProperty().addListener((obs, old, value) -> {
            positionEmptyState();
            redrawConnections();
        });
        mapPane.heightProperty().addListener((obs, old, value) -> {
            positionEmptyState();
            redrawConnections();
        });
        Platform.runLater(this::positionEmptyState);
        loadSavedMaps(true);
    }

    @FXML
    private void handleNewMap() {
        clearInMemory();
        currentMapId = 0;
        mapNameField.setText("New Study Map");
        setStatus("New map ready. Add a root topic, then select any node to branch from it.", true);
        refreshSavedMapsSelection();
    }

    @FXML
    private void handleSetRootTopic() {
        String text = rootTopicField.getText().trim();
        if (text.isEmpty()) { setStatus("Enter a root topic first.", false); return; }
        if (!nodes.isEmpty()) { setStatus("This map already has a root. Use 'Center Selected' to make any existing node central.", false); return; }

        MapNode root = new MapNode(UUID.randomUUID().toString(), text, null,
                Math.max(40, mapPane.getWidth() / 2 - 80), Math.max(40, mapPane.getHeight() / 2 - 30));
        nodes.put(root.key, root);
        focusNode = root;
        rootTopicField.clear();
        renderNodes();
        saveCurrentMap();
        setStatus("Root topic created.", true);
    }

    @FXML
    private void handleAddNode() {
        String text = newNodeField.getText().trim();
        if (text.isEmpty()) { setStatus("Enter a branch topic first.", false); return; }
        MapNode parent = focusNode;
        if (parent == null && !nodes.isEmpty()) parent = nodes.values().iterator().next();
        if (parent == null) { setStatus("Create a root topic first.", false); return; }

        int siblingIndex = childCount(parent);
        double verticalOffset = siblingIndex == 0 ? 0 : ((siblingIndex + 1) / 2) * 64.0 * (siblingIndex % 2 == 0 ? 1 : -1);
        double px = parent.x + 210;
        double py = parent.y + verticalOffset;
        MapNode node = new MapNode(UUID.randomUUID().toString(), text, parent.key, clampX(px, 24), clampY(py, 24));
        nodes.put(node.key, node);
        // Keep the chosen parent as the active/central node so repeated additions
        // naturally build multiple branches under the node the user selected.
        focusNode = parent;
        newNodeField.clear();
        renderNodes();
        saveCurrentMap();
        setStatus("Branch added to " + parent.label + ". Select any node to branch from it next.", true);
    }

    @FXML
    private void handleCenterSelected() {
        if (focusNode == null) { setStatus("Select a node first.", false); return; }
        centerFocusNode();
        renderNodes();
        saveCurrentMap();
        setStatus("Centered on: " + focusNode.label, true);
    }

    @FXML
    private void handleSaveMap() {
        if (nodes.isEmpty()) { setStatus("There is nothing to save yet.", false); return; }
        saveCurrentMap();
        setStatus("Mind map saved for this user.", true);
    }

    @FXML
    private void handleDeleteSavedMap() {
        if (currentMapId <= 0) { setStatus("This map has not been saved yet.", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete '" + safeMapName() + "'? This removes the saved map and its branches.");
        confirm.setHeaderText("Delete saved mind map");
        com.revisionassistant.util.DialogStyler.style(confirm);
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
        try {
            mindMapDAO.delete(currentMapId);
            handleNewMap();
            loadSavedMaps(false);
            setStatus("Saved map deleted.", true);
        } catch (Exception e) {
            setStatus("Could not delete the map: " + e.getMessage(), false);
        }
    }

    private void loadSavedMaps(boolean openLatest) {
        try {
            loading = true;
            List<MindMapDAO.MapRecord> maps = mindMapDAO.findAll();
            savedMapsCombo.getItems().setAll(maps);
            if (openLatest && !maps.isEmpty()) loadMap(maps.get(0));
            else refreshSavedMapsSelection();
        } catch (Exception e) {
            setStatus("Could not load saved maps: " + e.getMessage(), false);
        } finally {
            loading = false;
        }
    }

    private void loadMap(MindMapDAO.MapRecord map) {
        loading = true;
        try {
            currentMapId = map.id();
            mapNameField.setText(map.name());
            nodes.clear();
            for (MindMapDAO.NodeRecord record : map.nodes()) {
                // Persisted coordinates are the user's exact canvas positions.
                // Do not recalculate or clamp them on load.
                nodes.put(record.key(), new MapNode(record.key(), record.label(), record.parentKey(),
                        record.x(), record.y()));
            }
            focusNode = nodes.get(map.focusedKey());
            if (focusNode == null && !nodes.isEmpty()) focusNode = nodes.values().iterator().next();
            renderNodes();
            setStatus("Loaded saved map '" + map.name() + "'.", true);
        } finally {
            loading = false;
        }
    }

    private void saveCurrentMap() {
        if (loading || nodes.isEmpty()) return;
        try {
            String name = safeMapName();
            List<MindMapDAO.NodeRecord> records = nodes.values().stream()
                    .map(n -> new MindMapDAO.NodeRecord(n.key, n.label, n.x, n.y, n.parentKey)).toList();
            currentMapId = mindMapDAO.save(currentMapId, name,
                    focusNode == null ? null : focusNode.key, records);
            loading = true;
            List<MindMapDAO.MapRecord> maps = mindMapDAO.findAll();
            savedMapsCombo.getItems().setAll(maps);
            for (MindMapDAO.MapRecord m : maps) if (m.id() == currentMapId) savedMapsCombo.setValue(m);
            loading = false;
        } catch (Exception e) {
            loading = false;
            setStatus("Could not save the map: " + e.getMessage(), false);
        }
    }

    private void clearInMemory() {
        nodes.clear();
        focusNode = null;
        mapPane.getChildren().clear();
        mapPane.getChildren().add(connectionCanvas);
        mapPane.getChildren().add(emptyStateLabel);
        connectionCanvas.toBack();
        emptyStateLabel.setVisible(true);
        positionEmptyState();
        updateSelectionLabel();
        updateCount();
        redrawConnections();
    }

    private void renderNodes() {
        mapPane.getChildren().clear();
        mapPane.getChildren().add(connectionCanvas);
        connectionCanvas.toBack();
        for (MapNode node : nodes.values()) {
            node.ui = buildNodeUI(node);
            mapPane.getChildren().add(node.ui);
        }
        emptyStateLabel.setVisible(nodes.isEmpty());
        if (!nodes.isEmpty()) emptyStateLabel.setVisible(false);
        updateFocusStyles();
        positionEmptyState();
        updateSelectionLabel();
        updateCount();
        redrawConnections();
    }

    private StackPane buildNodeUI(MapNode node) {
        Label label = new Label(node.label);
        label.setWrapText(true);
        label.setMaxWidth(150);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("mindmap-node-label");

        StackPane pane = new StackPane(label);
        pane.setPadding(new Insets(node == focusNode ? 13 : 10, 16, node == focusNode ? 13 : 10, 16));
        pane.setMaxWidth(node == focusNode ? 180 : 160);
        pane.getStyleClass().add(node == focusNode ? "mindmap-focus-node" : "mindmap-branch-node");
        pane.setLayoutX(node.x);
        pane.setLayoutY(node.y);
        pane.setCursor(Cursor.MOVE);

        final double[] dragOffset = new double[2];
        pane.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            focusNode = node;
            dragOffset[0] = e.getX();
            dragOffset[1] = e.getY();
            updateFocusStyles();
            updateSelectionLabel();
        });
        pane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                focusNode = node;
                centerFocusNode();
                renderNodes();
                saveCurrentMap();
                setStatus("Centered on: " + node.label, true);
            }
        });
        pane.setOnMouseDragged(e -> {
            javafx.geometry.Point2D local = mapPane.sceneToLocal(e.getSceneX(), e.getSceneY());
            double nx = clampX(local.getX() - dragOffset[0], 4);
            double ny = clampY(local.getY() - dragOffset[1], 4);
            pane.setLayoutX(nx); pane.setLayoutY(ny);
            node.x = nx; node.y = ny;
            redrawConnections();
        });
        pane.setOnMouseReleased(e -> { if (e.getButton() == MouseButton.PRIMARY) saveCurrentMap(); });

        ContextMenu menu = new ContextMenu();
        MenuItem center = new MenuItem("Use as central node");
        center.setOnAction(e -> { focusNode = node; centerFocusNode(); renderNodes(); saveCurrentMap(); });
        MenuItem addChild = new MenuItem("Add branch to this node");
        addChild.setOnAction(e -> { focusNode = node; newNodeField.requestFocus(); updateSelectionLabel(); });
        MenuItem remove = new MenuItem("Remove node");
        remove.setOnAction(e -> removeNode(node));
        menu.getItems().addAll(center, addChild, new SeparatorMenuItem(), remove);
        pane.setOnContextMenuRequested(e -> menu.show(pane, e.getScreenX(), e.getScreenY()));
        return pane;
    }

    private void updateFocusStyles() {
        for (MapNode n : nodes.values()) {
            if (n.ui == null) continue;
            n.ui.getStyleClass().removeAll("mindmap-focus-node", "mindmap-branch-node");
            n.ui.getStyleClass().add(n == focusNode ? "mindmap-focus-node" : "mindmap-branch-node");
        }
    }

    private void positionEmptyState() {
        if (emptyStateLabel == null) return;
        double w = Math.max(0, mapPane.getWidth());
        double h = Math.max(0, mapPane.getHeight());
        emptyStateLabel.applyCss();
        double labelW = Math.min(360, Math.max(200, emptyStateLabel.prefWidth(-1)));
        double labelH = Math.max(34, emptyStateLabel.prefHeight(labelW));
        emptyStateLabel.setLayoutX(Math.max(12, (w - labelW) / 2));
        emptyStateLabel.setLayoutY(Math.max(12, (h - labelH) / 2));
        emptyStateLabel.setMaxWidth(labelW);
        emptyStateLabel.setAlignment(Pos.CENTER);
    }

    private void removeNode(MapNode node) {
        String newParent = node.parentKey;
        for (MapNode child : nodes.values()) if (node.key.equals(child.parentKey)) child.parentKey = newParent;
        nodes.remove(node.key);
        if (focusNode == node) {
            focusNode = newParent == null ? nodes.values().stream().findFirst().orElse(null) : nodes.get(newParent);
            if (focusNode == null && !nodes.isEmpty()) focusNode = nodes.values().iterator().next();
        }
        renderNodes();
        saveCurrentMap();
    }

    /**
     * Marks {@link #focusNode} as the map's central node WITHOUT touching any
     * other node's saved position. Every other node — including ones the user
     * has manually dragged — keeps its exact x/y. Only the newly-centered node
     * itself is moved to the middle of the canvas, the way a real desktop
     * mind-mapping app treats "center on this node": a highlight/anchor
     * change, not a full re-layout that would discard everyone else's
     * arrangement.
     */
    private void centerFocusNode() {
        if (focusNode == null || nodes.isEmpty()) return;
        double centerX = Math.max(100, mapPane.getWidth() / 2 - 90);
        double centerY = Math.max(90, mapPane.getHeight() / 2 - 30);
        focusNode.x = clampX(centerX, 4);
        focusNode.y = clampY(centerY, 4);
    }

    private void redrawConnections() {
        connectionCanvas.setWidth(Math.max(1, mapPane.getWidth()));
        connectionCanvas.setHeight(Math.max(1, mapPane.getHeight()));
        GraphicsContext gc = connectionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, connectionCanvas.getWidth(), connectionCanvas.getHeight());
        gc.setLineWidth(2.0);
        for (MapNode child : nodes.values()) {
            if (child.parentKey == null) continue;
            MapNode parent = nodes.get(child.parentKey);
            if (parent == null) continue;
            double x1 = parent.x + nodeWidth(parent) / 2;
            double y1 = parent.y + nodeHeight(parent) / 2;
            double x2 = child.x + nodeWidth(child) / 2;
            double y2 = child.y + nodeHeight(child) / 2;
            gc.setStroke(child == focusNode || parent == focusNode ? Color.web("#6366F1", .70) : Color.web("#A7B2C6", .62));
            double dx = x2 - x1;
            double dy = y2 - y1;
            double curve = Math.max(45, Math.min(150, Math.abs(dx) * 0.45 + Math.abs(dy) * 0.18));
            double c1x;
            double c1y;
            double c2x;
            double c2y;
            if (Math.abs(dx) >= Math.abs(dy)) {
                double direction = dx >= 0 ? 1 : -1;
                c1x = x1 + curve * direction;
                c1y = y1;
                c2x = x2 - curve * direction;
                c2y = y2;
            } else {
                double direction = dy >= 0 ? 1 : -1;
                c1x = x1;
                c1y = y1 + curve * direction;
                c2x = x2;
                c2y = y2 - curve * direction;
            }
            gc.beginPath();
            gc.moveTo(x1, y1);
            gc.bezierCurveTo(c1x, c1y, c2x, c2y, x2, y2);
            gc.stroke();
        }
    }

    private double clampX(double value, double margin) {
        double max = Math.max(margin, mapPane.getWidth() - 175);
        return Math.max(margin, Math.min(value, max));
    }

    private double clampY(double value, double margin) {
        double max = Math.max(margin, mapPane.getHeight() - 80);
        return Math.max(margin, Math.min(value, max));
    }

    private String safeMapName() {
        String value = mapNameField.getText() == null ? "" : mapNameField.getText().trim();
        return value.isEmpty() ? "Study Map" : value;
    }

    private void refreshSavedMapsSelection() {
        if (currentMapId <= 0) savedMapsCombo.getSelectionModel().clearSelection();
    }

    private void updateSelectionLabel() {
        selectedNodeLabel.setText(focusNode == null ? "Selected: none" : "Selected: " + focusNode.label);
    }

    private void updateCount() {
        nodeCountLabel.setText(nodes.size() + (nodes.size() == 1 ? " node" : " nodes"));
    }

    private void setStatus(String text, boolean success) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add(success ? "status-success" : "status-error");
    }

    private int childCount(MapNode parent) {
        int count = 0;
        for (MapNode n : nodes.values()) if (parent.key.equals(n.parentKey)) count++;
        return count;
    }

    private static double nodeWidth(MapNode n) { return n.ui == null ? 150 : Math.max(1, n.ui.getBoundsInParent().getWidth()); }
    private static double nodeHeight(MapNode n) { return n.ui == null ? 56 : Math.max(1, n.ui.getBoundsInParent().getHeight()); }

}
