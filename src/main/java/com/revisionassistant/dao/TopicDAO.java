package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Topic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the topics table. No validation or business
 * rules live here - callers (the service layer) are responsible for
 * that. Every statement is a PreparedStatement.
 */
public class TopicDAO {

    public Topic insert(Topic topic) throws SQLException {
        String sql = "INSERT INTO topics (subject_id, name, completed) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, topic.getSubjectId());
            statement.setString(2, topic.getName());
            statement.setInt(3, topic.isCompleted() ? 1 : 0);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    topic.setId(keys.getInt(1));
                }
            }
        }
        return topic;
    }

    public List<Topic> findBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT id, subject_id, name, completed FROM topics "
                + "WHERE subject_id = ? ORDER BY name";
        List<Topic> topics = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    topics.add(mapRow(resultSet));
                }
            }
        }
        return topics;
    }

    public Topic findById(int id) throws SQLException {
        String sql = "SELECT id, subject_id, name, completed FROM topics WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    public List<Topic> findAll() throws SQLException {
        String sql = "SELECT id, subject_id, name, completed FROM topics ORDER BY subject_id, name";
        List<Topic> topics = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                topics.add(mapRow(resultSet));
            }
        }
        return topics;
    }

    /**
     * Used by the service layer to decide whether a subject is safe
     * to delete.
     */
    public int countBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM topics WHERE subject_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public void update(Topic topic) throws SQLException {
        String sql = "UPDATE topics SET subject_id = ?, name = ?, completed = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, topic.getSubjectId());
            statement.setString(2, topic.getName());
            statement.setInt(3, topic.isCompleted() ? 1 : 0);
            statement.setInt(4, topic.getId());
            statement.executeUpdate();
        }
    }

    public void updateCompleted(int id, boolean completed) throws SQLException {
        String sql = "UPDATE topics SET completed = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, completed ? 1 : 0);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM topics WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Topic mapRow(ResultSet resultSet) throws SQLException {
        return new Topic(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                resultSet.getString("name"),
                resultSet.getInt("completed") == 1
        );
    }
}
