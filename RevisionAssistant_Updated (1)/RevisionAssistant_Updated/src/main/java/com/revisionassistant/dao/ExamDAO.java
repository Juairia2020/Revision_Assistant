package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.Exam;
import com.revisionassistant.session.CurrentUser;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {
    private static final String COLUMNS = "id,subject_id,title,exam_date,progress";

    public Exam insert(Exam exam) throws SQLException {
        String sql = "INSERT INTO exams(user_id,subject_id,title,exam_date,progress) VALUES(?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, CurrentUser.get().getId());
            bind(s, exam, 2);
            s.executeUpdate();
            try (ResultSet k = s.getGeneratedKeys()) {
                if (k.next()) exam.setId(k.getInt(1));
            }
        }
        return exam;
    }

    public List<Exam> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM exams WHERE user_id=? ORDER BY exam_date";
        List<Exam> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) out.add(mapRow(r));
            }
        }
        return out;
    }

    public void update(Exam exam) throws SQLException {
        String sql = "UPDATE exams SET subject_id=?,title=?,exam_date=?,progress=? WHERE id=? AND user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, exam, 1);
            s.setInt(5, exam.getId());
            s.setInt(6, CurrentUser.get().getId());
            s.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id=? AND user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            s.setInt(2, CurrentUser.get().getId());
            s.executeUpdate();
        }
    }

    public int countBySubjectId(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM exams WHERE subject_id=? AND user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            s.setInt(2, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) return r.getInt(1);
            }
        }
        return 0;
    }

    public void replaceTopics(int examId, List<Integer> topicIds) throws SQLException {
        int userId = CurrentUser.get().getId();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement d = c.prepareStatement(
                        "DELETE FROM exam_topics WHERE exam_id=? AND user_id=?")) {
                    d.setInt(1, examId);
                    d.setInt(2, userId);
                    d.executeUpdate();
                }
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO exam_topics(user_id,exam_id,topic_id) VALUES(?,?,?)")) {
                    for (Integer topicId : topicIds) {
                        ins.setInt(1, userId);
                        ins.setInt(2, examId);
                        ins.setInt(3, topicId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public List<Integer> findTopicIds(int examId) throws SQLException {
        String sql = "SELECT et.topic_id FROM exam_topics et " +
                "WHERE et.exam_id=? AND et.user_id=? ORDER BY et.id";
        List<Integer> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, examId);
            s.setInt(2, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) out.add(r.getInt(1));
            }
        }
        return out;
    }

    public int calculateProgress(int examId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total, " +
                "COALESCE(SUM(CASE WHEN t.completed=1 THEN 1 ELSE 0 END),0) AS done " +
                "FROM exam_topics et " +
                "JOIN topics t ON t.id=et.topic_id AND t.user_id=et.user_id " +
                "WHERE et.exam_id=? AND et.user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, examId);
            s.setInt(2, CurrentUser.get().getId());
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    int total = r.getInt("total");
                    int done = r.getInt("done");
                    return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
                }
            }
        }
        return 0;
    }

    private void bind(PreparedStatement s, Exam e, int o) throws SQLException {
        s.setInt(o, e.getSubjectId());
        s.setString(o + 1, e.getTitle());
        s.setString(o + 2, e.getExamDate().toString());
        s.setInt(o + 3, e.getProgress());
    }

    private Exam mapRow(ResultSet r) throws SQLException {
        return new Exam(r.getInt("id"), r.getInt("subject_id"), r.getString("title"),
                LocalDate.parse(r.getString("exam_date")), r.getInt("progress"));
    }
}
