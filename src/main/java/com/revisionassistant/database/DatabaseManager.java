package com.revisionassistant.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the connection to the SQLite database and the creation of the
 * schema. DAOs ask this class for connections instead of talking to
 * DriverManager directly, so the JDBC URL and setup logic live in one
 * place only.
 */
public final class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:revision_assistant.db";

    private DatabaseManager() {
        // Utility class - no instances.
    }

    /**
     * Opens a new connection to the database. SQLite enforces foreign
     * keys per-connection, so the pragma is set here every time a
     * connection is created.
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }

    /**
     * Creates the SQLite file (if it does not already exist) and makes
     * sure both tables are present. Safe to call every time the
     * application starts - existing data is left untouched.
     */
    public static void initializeDatabase() throws SQLException {
        String createSubjectsTable =
                "CREATE TABLE IF NOT EXISTS subjects (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    name TEXT NOT NULL," +
                "    color TEXT" +
                ")";

        String createTopicsTable =
                "CREATE TABLE IF NOT EXISTS topics (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    subject_id INTEGER NOT NULL," +
                "    name TEXT NOT NULL," +
                "    completed INTEGER NOT NULL DEFAULT 0," +
                "    FOREIGN KEY(subject_id) REFERENCES subjects(id)" +
                ")";

        // --- Milestone 2 tables ---------------------------------------

        String createTasksTable =
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    subject_id INTEGER NOT NULL," +
                "    topic_id INTEGER," +
                "    title TEXT NOT NULL," +
                "    estimated_minutes INTEGER NOT NULL DEFAULT 0," +
                "    priority TEXT NOT NULL DEFAULT 'MEDIUM'," +
                "    deadline TEXT," +
                "    completed INTEGER NOT NULL DEFAULT 0," +
                "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                ")";

        String createExamsTable =
                "CREATE TABLE IF NOT EXISTS exams (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    subject_id INTEGER NOT NULL," +
                "    title TEXT NOT NULL," +
                "    exam_date TEXT NOT NULL," +
                "    progress INTEGER NOT NULL DEFAULT 0," +
                "    FOREIGN KEY(subject_id) REFERENCES subjects(id)" +
                ")";

        String createStudySessionsTable =
                "CREATE TABLE IF NOT EXISTS study_sessions (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    subject_id INTEGER NOT NULL," +
                "    topic_id INTEGER," +
                "    session_date TEXT NOT NULL," +
                "    duration_minutes INTEGER NOT NULL," +
                "    notes TEXT," +
                "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createSubjectsTable);
            statement.execute(createTopicsTable);
            statement.execute(createTasksTable);
            statement.execute(createExamsTable);
            statement.execute(createStudySessionsTable);
        }
    }
}
