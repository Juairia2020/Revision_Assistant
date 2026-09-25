package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the subjects table. No validation or business
 * rules live here - callers (the service layer) are responsible for
 * that. Every statement is a PreparedStatement.
 */
public class SubjectDAO {

    public Subject insert(Subject subject) throws SQLException {
        String sql = "INSERT INTO subjects (name, color) VALUES (?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, subject.getName());
            statement.setString(2, subject.getColor());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    subject.setId(keys.getInt(1));
                }
            }
        }
        return subject;
    }

    public List<Subject> findAll() throws SQLException {
        String sql = "SELECT id, name, color FROM subjects ORDER BY name";
        List<Subject> subjects = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                subjects.add(mapRow(resultSet));
            }
        }
        return subjects;
    }

    public Subject findById(int id) throws SQLException {
        String sql = "SELECT id, name, color FROM subjects WHERE id = ?";

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

    public void update(Subject subject) throws SQLException {
        String sql = "UPDATE subjects SET name = ?, color = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, subject.getName());
            statement.setString(2, subject.getColor());
            statement.setInt(3, subject.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Subject mapRow(ResultSet resultSet) throws SQLException {
        return new Subject(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("color")
        );
    }
}
