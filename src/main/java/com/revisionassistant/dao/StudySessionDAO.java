package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.StudySession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the study_sessions table. No validation or
 * business rules live here - callers (the service layer) are
 * responsible for that. Every statement is a PreparedStatement.
 */
public class StudySessionDAO {

    private static final String COLUMNS =
            "id, subject_id, topic_id, session_date, duration_minutes, notes";

    public StudySession insert(StudySession session) throws SQLException {
        String sql = "INSERT INTO study_sessions (subject_id, topic_id, session_date, "
                + "duration_minutes, notes) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindSession(statement, session);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    session.setId(keys.getInt(1));
                }
            }
        }
        return session;
    }

    public List<StudySession> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM study_sessions ORDER BY session_date DESC, id DESC";
        List<StudySession> sessions = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                sessions.add(mapRow(resultSet));
            }
        }
        return sessions;
    }

    public void update(StudySession session) throws SQLException {
        String sql = "UPDATE study_sessions SET subject_id = ?, topic_id = ?, session_date = ?, "
                + "duration_minutes = ?, notes = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindSession(statement, session);
            statement.setInt(6, session.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM study_sessions WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /** Used by the service layer to decide whether a subject/topic is safe to delete. */
    public int countBySubjectId(int subjectId) throws SQLException {
        return countWhere("subject_id = ?", subjectId);
    }

    public int countByTopicId(int topicId) throws SQLException {
        return countWhere("topic_id = ?", topicId);
    }

    private int countWhere(String whereClause, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM study_sessions WHERE " + whereClause;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private void bindSession(PreparedStatement statement, StudySession session) throws SQLException {
        statement.setInt(1, session.getSubjectId());
        if (session.getTopicId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, session.getTopicId());
        }
        statement.setString(3, session.getDate().toString());
        statement.setInt(4, session.getDurationMinutes());
        statement.setString(5, session.getNotes());
    }

    private StudySession mapRow(ResultSet resultSet) throws SQLException {
        int topicIdValue = resultSet.getInt("topic_id");
        Integer topicId = resultSet.wasNull() ? null : topicIdValue;

        return new StudySession(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                topicId,
                LocalDate.parse(resultSet.getString("session_date")),
                resultSet.getInt("duration_minutes"),
                resultSet.getString("notes")
        );
    }
}
