package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.QuizAttempt;

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
 * Direct SQL access for the quiz_attempts table. No validation or
 * business rules live here - callers (the service layer) are
 * responsible for that. Every statement is a PreparedStatement.
 */
public class QuizAttemptDAO {

    private static final String COLUMNS =
            "id, subject_id, topic_id, attempt_date, total_questions, correct_answers, score_percent";

    public QuizAttempt insert(QuizAttempt attempt) throws SQLException {
        String sql = "INSERT INTO quiz_attempts (subject_id, topic_id, attempt_date, "
                + "total_questions, correct_answers, score_percent) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, attempt.getSubjectId());
            if (attempt.getTopicId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, attempt.getTopicId());
            }
            statement.setString(3, attempt.getAttemptDate().toString());
            statement.setInt(4, attempt.getTotalQuestions());
            statement.setInt(5, attempt.getCorrectAnswers());
            statement.setInt(6, attempt.getScorePercent());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    attempt.setId(keys.getInt(1));
                }
            }
        }
        return attempt;
    }

    /** All attempts, most recent first. */
    public List<QuizAttempt> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM quiz_attempts ORDER BY id DESC";
        List<QuizAttempt> attempts = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                attempts.add(mapRow(resultSet));
            }
        }
        return attempts;
    }

    /** Used by the service layer to decide whether a subject/topic is safe to delete. */
    public int countBySubjectId(int subjectId) throws SQLException {
        return countWhere("subject_id = ?", subjectId);
    }

    public int countByTopicId(int topicId) throws SQLException {
        return countWhere("topic_id = ?", topicId);
    }

    private int countWhere(String whereClause, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM quiz_attempts WHERE " + whereClause;
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

    private QuizAttempt mapRow(ResultSet resultSet) throws SQLException {
        int topicIdValue = resultSet.getInt("topic_id");
        Integer topicId = resultSet.wasNull() ? null : topicIdValue;

        return new QuizAttempt(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                topicId,
                LocalDate.parse(resultSet.getString("attempt_date")),
                resultSet.getInt("total_questions"),
                resultSet.getInt("correct_answers"),
                resultSet.getInt("score_percent")
        );
    }
}
