package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.TopicDependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the topic_dependencies table. No validation
 * or business rules live here (in particular, cycle prevention lives
 * in {@code TopicDependencyService}, using the {@code algorithm}
 * package) - callers are responsible for that. Every statement is a
 * PreparedStatement.
 */
public class TopicDependencyDAO {

    public TopicDependency insert(int topicId, int prerequisiteId) throws SQLException {
        String sql = "INSERT INTO topic_dependencies (topic_id, prerequisite_id) VALUES (?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, topicId);
            statement.setInt(2, prerequisiteId);
            statement.executeUpdate();

            TopicDependency dependency = new TopicDependency(topicId, prerequisiteId);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    dependency.setId(keys.getInt(1));
                }
            }
            return dependency;
        }
    }

    public List<TopicDependency> findAll() throws SQLException {
        String sql = "SELECT id, topic_id, prerequisite_id FROM topic_dependencies";
        List<TopicDependency> dependencies = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                dependencies.add(mapRow(resultSet));
            }
        }
        return dependencies;
    }

    /** The ids of the topics directly required before {@code topicId} can be studied. */
    public List<Integer> findPrerequisitesOf(int topicId) throws SQLException {
        String sql = "SELECT prerequisite_id FROM topic_dependencies WHERE topic_id = ?";
        return queryIds(sql, topicId);
    }

    /** The ids of the topics that directly require {@code topicId} as a prerequisite. */
    public List<Integer> findDependentsOf(int topicId) throws SQLException {
        String sql = "SELECT topic_id FROM topic_dependencies WHERE prerequisite_id = ?";
        return queryIds(sql, topicId);
    }

    public void delete(int topicId, int prerequisiteId) throws SQLException {
        String sql = "DELETE FROM topic_dependencies WHERE topic_id = ? AND prerequisite_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, topicId);
            statement.setInt(2, prerequisiteId);
            statement.executeUpdate();
        }
    }

    /** Removes every dependency row that mentions this topic, on either side. Used when a topic is deleted. */
    public void deleteAllForTopic(int topicId) throws SQLException {
        String sql = "DELETE FROM topic_dependencies WHERE topic_id = ? OR prerequisite_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, topicId);
            statement.setInt(2, topicId);
            statement.executeUpdate();
        }
    }

    private List<Integer> queryIds(String sql, int topicId) throws SQLException {
        List<Integer> ids = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, topicId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        }
        return ids;
    }

    private TopicDependency mapRow(ResultSet resultSet) throws SQLException {
        return new TopicDependency(
                resultSet.getInt("id"),
                resultSet.getInt("topic_id"),
                resultSet.getInt("prerequisite_id")
        );
    }
}
