package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Priority;
import com.revisionassistant.model.Task;

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
 * Direct SQL access for the tasks table. No validation or business
 * rules live here - callers (the service layer) are responsible for
 * that. Every statement is a PreparedStatement.
 */
public class TaskDAO {

    private static final String COLUMNS =
            "id, subject_id, topic_id, title, estimated_minutes, priority, deadline, completed";

    public Task insert(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (subject_id, topic_id, title, estimated_minutes, "
                + "priority, deadline, completed) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTask(statement, task);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getInt(1));
                }
            }
        }
        return task;
    }

    public List<Task> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM tasks ORDER BY completed, deadline IS NULL, deadline";
        List<Task> tasks = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tasks.add(mapRow(resultSet));
            }
        }
        return tasks;
    }

    public void update(Task task) throws SQLException {
        String sql = "UPDATE tasks SET subject_id = ?, topic_id = ?, title = ?, "
                + "estimated_minutes = ?, priority = ?, deadline = ?, completed = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindTask(statement, task);
            statement.setInt(8, task.getId());
            statement.executeUpdate();
        }
    }

    public void updateCompleted(int id, boolean completed) throws SQLException {
        String sql = "UPDATE tasks SET completed = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, completed ? 1 : 0);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

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
        String sql = "SELECT COUNT(*) FROM tasks WHERE " + whereClause;
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

    private void bindTask(PreparedStatement statement, Task task) throws SQLException {
        statement.setInt(1, task.getSubjectId());
        if (task.getTopicId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, task.getTopicId());
        }
        statement.setString(3, task.getTitle());
        statement.setInt(4, task.getEstimatedMinutes());
        statement.setString(5, task.getPriority().name());
        statement.setString(6, task.getDeadline() == null ? null : task.getDeadline().toString());
        statement.setInt(7, task.isCompleted() ? 1 : 0);
    }

    private Task mapRow(ResultSet resultSet) throws SQLException {
        int topicIdValue = resultSet.getInt("topic_id");
        Integer topicId = resultSet.wasNull() ? null : topicIdValue;

        String deadlineText = resultSet.getString("deadline");
        LocalDate deadline = deadlineText == null ? null : LocalDate.parse(deadlineText);

        return new Task(
                resultSet.getInt("id"),
                resultSet.getInt("subject_id"),
                topicId,
                resultSet.getString("title"),
                resultSet.getInt("estimated_minutes"),
                Priority.fromString(resultSet.getString("priority")),
                deadline,
                resultSet.getInt("completed") == 1
        );
    }
}
