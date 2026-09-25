package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.model.QuizQuestion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the quiz_questions table. No validation or
 * business rules live here - callers (the service layer) are
 * responsible for that. Every statement is a PreparedStatement.
 */
public class QuizQuestionDAO {

    private static final String COLUMNS =
            "id, subject_id, topic_id, question_text, option_a, option_b, option_c, option_d, correct_option";

    public QuizQuestion insert(QuizQuestion question) throws SQLException {
        String sql = "INSERT INTO quiz_questions (subject_id, topic_id, question_text, "
                + "option_a, option_b, option_c, option_d, correct_option) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindQuestion(statement, question);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    question.setId(keys.getInt(1));
                }
            }
        }
        return question;
    }

    public List<QuizQuestion> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM quiz_questions ORDER BY subject_id, id";
        List<QuizQuestion> questions = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                questions.add(mapRow(resultSet));
            }
        }
        return questions;
    }

    public QuizQuestion findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM quiz_questions WHERE id = ?";

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

    public void update(QuizQuestion question) throws SQLException {
        String sql = "UPDATE quiz_questions SET subject_id = ?, topic_id = ?, question_text = ?, "
                + "option_a = ?, option_b = ?, option_c = ?, option_d = ?, correct_option = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindQuestion(statement, question);
            statement.setInt(9, question.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM quiz_questions WHERE id = ?";

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
        String sql = "SELECT COUNT(*) FROM quiz_questions WHERE " + whereClause;
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

    private void bindQuestion(PreparedStatement statement, QuizQuestion question) throws SQLException {
        statement.setInt(1, question.getSubjectId());
        if (question.getTopicId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, question.getTopicId());
        }
        statement.setString(3, question.getQuestionText());
        statement.setString(4, question.getOptionA());
        statement.setString(5, question.getOptionB());
        statement.setString(6, question.getOptionC());
        statement.setString(7, question.getOptionD());
        statement.setString(8, question.getCorrectOption().name());
    }

    private QuizQuestion mapRow(ResultSet resultSet) throws SQLException {
        int topicIdValue = resultSet.getInt("topic_id");
        Integer topicId = resultSet.wasNull() ? null : topicIdValue;

        return new QuizQuestion(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                topicId,
                resultSet.getString("question_text"),
                resultSet.getString("option_a"),
                resultSet.getString("option_b"),
                resultSet.getString("option_c"),
                resultSet.getString("option_d"),
                QuizOption.fromString(resultSet.getString("correct_option"))
        );
    }
}
