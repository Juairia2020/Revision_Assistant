package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** User-scoped persistence for mind-map snapshots and their node hierarchy. */
public class MindMapDAO {
    public record NodeRecord(String key, String label, double x, double y, String parentKey) {}
    public record MapRecord(int id, String name, String focusedKey, List<NodeRecord> nodes) {}

    public List<MapRecord> findAll() throws Exception {
        String sql = "SELECT id, name, focused_key FROM mind_maps WHERE user_id=? ORDER BY updated_at DESC, id DESC";
        List<MapRecord> maps = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) maps.add(loadMap(c, r.getInt("id"), r.getString("name"), r.getString("focused_key")));
            }
        }
        return maps;
    }

    public MapRecord findById(int id) throws Exception {
        String sql = "SELECT id, name, focused_key FROM mind_maps WHERE id=? AND user_id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id); s.setInt(2, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) return loadMap(c, r.getInt("id"), r.getString("name"), r.getString("focused_key"));
            }
        }
        return null;
    }

    public int save(int mapId, String name, String focusedKey, List<NodeRecord> nodes) throws Exception {
        if (!CurrentUser.isLoggedIn()) throw new IllegalStateException("A signed-in user is required.");
        int userId = CurrentUser.get().getId();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (mapId <= 0) {
                    try (PreparedStatement s = c.prepareStatement(
                            "INSERT INTO mind_maps(user_id,name,focused_key,created_at,updated_at) VALUES(?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        LocalDateTime now = LocalDateTime.now();
                        s.setInt(1, userId); s.setString(2, name); s.setString(3, focusedKey);
                        s.setString(4, now.toString()); s.setString(5, now.toString());
                        s.executeUpdate();
                        try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) mapId = keys.getInt(1); }
                    }
                } else {
                    try (PreparedStatement s = c.prepareStatement(
                            "UPDATE mind_maps SET name=?, focused_key=?, updated_at=? WHERE id=? AND user_id=?")) {
                        s.setString(1, name); s.setString(2, focusedKey); s.setString(3, LocalDateTime.now().toString());
                        s.setInt(4, mapId); s.setInt(5, userId); s.executeUpdate();
                    }
                    try (PreparedStatement s = c.prepareStatement("DELETE FROM mind_map_nodes WHERE map_id=?")) {
                        s.setInt(1, mapId); s.executeUpdate();
                    }
                }
                if (mapId <= 0) throw new IllegalStateException("Could not create the mind map.");
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO mind_map_nodes(map_id,node_key,label,x_pos,y_pos,parent_key) VALUES(?,?,?,?,?,?)")) {
                    for (NodeRecord n : nodes) {
                        s.setInt(1, mapId); s.setString(2, n.key()); s.setString(3, n.label());
                        s.setDouble(4, n.x()); s.setDouble(5, n.y());
                        if (n.parentKey() == null || n.parentKey().isBlank()) s.setNull(6, java.sql.Types.VARCHAR);
                        else s.setString(6, n.parentKey());
                        s.addBatch();
                    }
                    s.executeBatch();
                }
                c.commit();
                return mapId;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void delete(int id) throws Exception {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(
                "DELETE FROM mind_maps WHERE id=? AND user_id=?")) {
            s.setInt(1, id); s.setInt(2, CurrentUser.get().getId()); s.executeUpdate();
        }
    }

    private MapRecord loadMap(Connection c, int id, String name, String focusedKey) throws Exception {
        String sql = "SELECT node_key,label,x_pos,y_pos,parent_key FROM mind_map_nodes WHERE map_id=? ORDER BY id";
        List<NodeRecord> nodes = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) nodes.add(new NodeRecord(r.getString("node_key"), r.getString("label"),
                        r.getDouble("x_pos"), r.getDouble("y_pos"), r.getString("parent_key")));
            }
        }
        return new MapRecord(id, name, focusedKey, nodes);
    }
}
