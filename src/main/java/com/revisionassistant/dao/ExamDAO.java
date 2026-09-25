package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Exam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the exams table. No validation or business
 * rules live here - callers (the service layer) are responsible for
 * that. Every statement is a PreparedStatement.
 */
public class ExamDAO {

    private static final String COLUMNS = "id, subject_id, title, exam_date, progress";

    public Exam insert(Exam exam) throws SQLException {
        String sql = "INSERT INTO exams (subject_id, title, exam_date, progress) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindExam(statement, exam);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    exam.setId(keys.getInt(1));
                }
            }
        }
        return exam;
    }

    public List<Exam> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM exams ORDER BY exam_date";
        List<Exam> exams = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                exams.add(mapRow(resultSet));
            }
        }
        return exams;
    }

    public void update(Exam exam) throws SQLException {
        String sql = "UPDATE exams SET subject_id = ?, title = ?, exam_date = ?, progress = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindExam(statement, exam);
            statement.setInt(5, exam.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /** Used by the service layer to decide whether a subject is safe to delete. */
    public int countBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM exams WHERE subject_id = ?";
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

    private void bindExam(PreparedStatement statement, Exam exam) throws SQLException {
        statement.setInt(1, exam.getSubjectId());
        statement.setString(2, exam.getTitle());
        statement.setString(3, exam.getExamDate().toString());
        statement.setInt(4, exam.getProgress());
    }

    private Exam mapRow(ResultSet resultSet) throws SQLException {
        return new Exam(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                resultSet.getString("title"),
                LocalDate.parse(resultSet.getString("exam_date")),
                resultSet.getInt("progress")
        );
    }
}
