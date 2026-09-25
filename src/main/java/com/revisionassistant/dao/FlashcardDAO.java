package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Flashcard;
import com.revisionassistant.model.RevisionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct SQL access for the flashcards table. No validation or
 * business rules live here - callers (the service layer) are
 * responsible for that. Every statement is a PreparedStatement.
 */
public class FlashcardDAO {

    private static final String COLUMNS =
            "id, subject_id, topic_id, front, back, difficult, revision_status";

    public Flashcard insert(Flashcard flashcard) throws SQLException {
        String sql = "INSERT INTO flashcards (subject_id, topic_id, front, back, difficult, "
                + "revision_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindFlashcard(statement, flashcard);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    flashcard.setId(keys.getInt(1));
                }
            }
        }
        return flashcard;
    }

    public List<Flashcard> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM flashcards ORDER BY subject_id, id";
        List<Flashcard> flashcards = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                flashcards.add(mapRow(resultSet));
            }
        }
        return flashcards;
    }

    public void update(Flashcard flashcard) throws SQLException {
        String sql = "UPDATE flashcards SET subject_id = ?, topic_id = ?, front = ?, back = ?, "
                + "difficult = ?, revision_status = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindFlashcard(statement, flashcard);
            statement.setInt(7, flashcard.getId());
            statement.executeUpdate();
        }
    }

    public void updateDifficult(int id, boolean difficult) throws SQLException {
        String sql = "UPDATE flashcards SET difficult = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, difficult ? 1 : 0);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public void updateRevisionStatus(int id, RevisionStatus status) throws SQLException {
        String sql = "UPDATE flashcards SET revision_status = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM flashcards WHERE id = ?";

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
        String sql = "SELECT COUNT(*) FROM flashcards WHERE " + whereClause;
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

    private void bindFlashcard(PreparedStatement statement, Flashcard flashcard) throws SQLException {
        statement.setInt(1, flashcard.getSubjectId());
        if (flashcard.getTopicId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, flashcard.getTopicId());
        }
        statement.setString(3, flashcard.getFront());
        statement.setString(4, flashcard.getBack());
        statement.setInt(5, flashcard.isDifficult() ? 1 : 0);
        statement.setString(6, flashcard.getRevisionStatus().name());
    }

    private Flashcard mapRow(ResultSet resultSet) throws SQLException {
        int topicIdValue = resultSet.getInt("topic_id");
        Integer topicId = resultSet.wasNull() ? null : topicIdValue;

        return new Flashcard(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                topicId,
                resultSet.getString("front"),
                resultSet.getString("back"),
                resultSet.getInt("difficult") == 1,
                RevisionStatus.fromString(resultSet.getString("revision_status"))
        );
    }
}
