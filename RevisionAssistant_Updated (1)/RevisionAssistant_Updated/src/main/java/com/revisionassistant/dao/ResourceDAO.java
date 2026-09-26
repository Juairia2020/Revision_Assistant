package com.revisionassistant.dao;
import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.revisionassistant.model.Resource;

public class ResourceDAO {
    private static final String COLUMNS = "id,subject_id,title,url,description,category";

    public Resource insert(Resource resource) throws SQLException {
        String sql = "INSERT INTO resources(user_id,subject_id,title,url,description,category) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, CurrentUser.get().getId());
            bindResource(s, resource, 2);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) resource.setId(keys.getInt(1));
            }
        }
        return resource;
    }

    public List<Resource> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM resources WHERE user_id=? ORDER BY id DESC";
        List<Resource> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) out.add(mapRow(r));
            }
        }
        return out;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM resources WHERE id=? AND user_id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            s.setInt(2, CurrentUser.get().getId());
            s.executeUpdate();
        }
    }

    private void bindResource(PreparedStatement s, Resource r, int offset) throws SQLException {
        if (r.getSubjectId() == null) s.setNull(offset, Types.INTEGER); else s.setInt(offset, r.getSubjectId());
        s.setString(offset + 1, r.getTitle());
        s.setString(offset + 2, r.getUrl());
        s.setString(offset + 3, r.getDescription());
        s.setString(offset + 4, r.getCategory());
    }

    private Resource mapRow(ResultSet r) throws SQLException {
        int subjectValue = r.getInt("subject_id");
        Integer subjectId = r.wasNull() ? null : subjectValue;
        return new Resource(r.getInt("id"), subjectId, r.getString("title"), r.getString("url"),
                r.getString("description"), r.getString("category"));
    }
}
