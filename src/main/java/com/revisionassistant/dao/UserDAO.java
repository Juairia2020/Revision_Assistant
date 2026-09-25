package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Direct SQL access for local user accounts. Controllers never execute SQL.
 */
public class UserDAO {

    public User insert(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
        return user;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, email, password_hash, onboarding_completed FROM users WHERE email = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }


    /** Claims study records created before account ownership existed for the first registered user. */
    public void claimLegacyStudyData(int userId) throws SQLException {
        String[] tables = {
                "subjects", "topics", "tasks", "exams", "study_sessions",
                "flashcards", "quiz_questions", "quiz_attempts",
                "quiz_attempt_answers", "topic_dependencies"
        };
        try (Connection connection = DatabaseManager.getConnection()) {
            for (String table : tables) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + table + " SET user_id = ? WHERE user_id IS NULL")) {
                    statement.setInt(1, userId);
                    statement.executeUpdate();
                }
            }
        }
    }

    public void markOnboardingCompleted(int userId) throws SQLException {
        String sql = "UPDATE users SET onboarding_completed = 1 WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getInt("onboarding_completed") != 0
        );
    }
}
