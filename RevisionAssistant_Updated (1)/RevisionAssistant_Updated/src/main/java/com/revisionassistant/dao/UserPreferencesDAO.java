package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.UserPreferences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Persists per-account appearance and reminder settings. */
public class UserPreferencesDAO {
    public UserPreferences findByUserId(int userId) throws SQLException {
        String sql = "SELECT user_id, theme, font_size, study_reminders, exam_reminders, " +
                "achievement_notifications, streak_notifications, pomodoro_duration, " +
                "pomodoro_short_break, pomodoro_long_break FROM user_preferences WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) return mapRow(r);
            }
        }
        UserPreferences defaults = new UserPreferences(userId);
        save(defaults);
        return defaults;
    }

    public void save(UserPreferences p) throws SQLException {
        String sql = "INSERT INTO user_preferences " +
                "(user_id, theme, font_size, study_reminders, exam_reminders, achievement_notifications, " +
                "streak_notifications, pomodoro_duration, pomodoro_short_break, pomodoro_long_break) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET " +
                "theme=excluded.theme, font_size=excluded.font_size, " +
                "study_reminders=excluded.study_reminders, exam_reminders=excluded.exam_reminders, " +
                "achievement_notifications=excluded.achievement_notifications, streak_notifications=excluded.streak_notifications, " +
                "pomodoro_duration=excluded.pomodoro_duration, pomodoro_short_break=excluded.pomodoro_short_break, " +
                "pomodoro_long_break=excluded.pomodoro_long_break";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, p.getUserId());
            s.setString(2, p.getTheme());
            s.setString(3, p.getFontSize());
            s.setInt(4, p.isStudyReminders() ? 1 : 0);
            s.setInt(5, p.isExamReminders() ? 1 : 0);
            s.setInt(6, p.isAchievementNotifications() ? 1 : 0);
            s.setInt(7, p.isStreakNotifications() ? 1 : 0);
            s.setInt(8, p.getPomodoroDuration());
            s.setInt(9, p.getPomodoroShortBreak());
            s.setInt(10, p.getPomodoroLongBreak());
            s.executeUpdate();
        }
    }

    private UserPreferences mapRow(ResultSet r) throws SQLException {
        UserPreferences p = new UserPreferences(r.getInt("user_id"));
        p.setTheme(r.getString("theme"));
        p.setFontSize(r.getString("font_size"));
        p.setStudyReminders(r.getInt("study_reminders") != 0);
        p.setExamReminders(r.getInt("exam_reminders") != 0);
        p.setAchievementNotifications(r.getInt("achievement_notifications") != 0);
        p.setStreakNotifications(r.getInt("streak_notifications") != 0);
        p.setPomodoroDuration(r.getInt("pomodoro_duration"));
        p.setPomodoroShortBreak(r.getInt("pomodoro_short_break"));
        p.setPomodoroLongBreak(r.getInt("pomodoro_long_break"));
        return p;
    }
}
