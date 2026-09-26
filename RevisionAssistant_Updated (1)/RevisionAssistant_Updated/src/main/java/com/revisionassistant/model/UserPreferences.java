package com.revisionassistant.model;

/** Per-user appearance, reminder, and Pomodoro preferences. */
public class UserPreferences {
    private int userId;
    private String theme = "LIGHT";
    private String fontSize = "MEDIUM";
    private boolean studyReminders = true;
    private boolean examReminders = true;
    private boolean achievementNotifications = true;
    private boolean streakNotifications = true;
    private int pomodoroDuration = 25;
    private int pomodoroShortBreak = 5;
    private int pomodoroLongBreak = 15;

    public UserPreferences() {}

    public UserPreferences(int userId) { this.userId = userId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme == null ? "LIGHT" : theme.toUpperCase(); }
    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize == null ? "MEDIUM" : fontSize.toUpperCase(); }
    public boolean isStudyReminders() { return studyReminders; }
    public void setStudyReminders(boolean value) { studyReminders = value; }
    public boolean isExamReminders() { return examReminders; }
    public void setExamReminders(boolean value) { examReminders = value; }
    public boolean isAchievementNotifications() { return achievementNotifications; }
    public void setAchievementNotifications(boolean value) { achievementNotifications = value; }
    public boolean isStreakNotifications() { return streakNotifications; }
    public void setStreakNotifications(boolean value) { streakNotifications = value; }
    public int getPomodoroDuration() { return pomodoroDuration; }
    public void setPomodoroDuration(int value) { pomodoroDuration = value; }
    public int getPomodoroShortBreak() { return pomodoroShortBreak; }
    public void setPomodoroShortBreak(int value) { pomodoroShortBreak = value; }
    public int getPomodoroLongBreak() { return pomodoroLongBreak; }
    public void setPomodoroLongBreak(int value) { pomodoroLongBreak = value; }
}
