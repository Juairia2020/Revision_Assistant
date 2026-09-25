package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.QuizAttemptAnswer;
import com.revisionassistant.model.QuizOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the quiz_attempt_answers table. No validation
 * or business rules live here - callers (the service layer) are
 * responsible for that. Every statement is a PreparedStatement.
 */
public class QuizAttemptAnswerDAO {

    private static final String COLUMNS = "id, attempt_id, question_id, selected_option, correct";

    public QuizAttemptAnswer insert(QuizAttemptAnswer answer) throws SQLException {
        String sql = "INSERT INTO quiz_attempt_answers (attempt_id, question_id, selected_option, correct) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, answer.getAttemptId());
            statement.setInt(2, answer.getQuestionId());
            statement.setString(3, answer.getSelectedOption().name());
            statement.setInt(4, answer.isCorrect() ? 1 : 0);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    answer.setId(keys.getInt(1));
                }
            }
        }
        return answer;
    }

    public List<QuizAttemptAnswer> findByAttemptId(int attemptId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM quiz_attempt_answers WHERE attempt_id = ? ORDER BY id";
        List<QuizAttemptAnswer> answers = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    answers.add(mapRow(resultSet));
                }
            }
        }
        return answers;
    }

    /** Every incorrect answer ever recorded, across all attempts - used to find frequently missed questions. */
    public List<QuizAttemptAnswer> findAllIncorrect() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM quiz_attempt_answers WHERE correct = 0";
        List<QuizAttemptAnswer> answers = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                answers.add(mapRow(resultSet));
            }
        }
        return answers;
    }

    /** Used by the service layer to decide whether a quiz question is safe to delete. */
    public int countByQuestionId(int questionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM quiz_attempt_answers WHERE question_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private QuizAttemptAnswer mapRow(ResultSet resultSet) throws SQLException {
        return new QuizAttemptAnswer(
                resultSet.getInt("id"),
                resultSet.getInt("attempt_id"),
                resultSet.getInt("question_id"),
                QuizOption.fromString(resultSet.getString("selected_option")),
                resultSet.getInt("correct") == 1
        );
    }
}
