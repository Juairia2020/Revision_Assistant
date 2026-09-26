package com.revisionassistant.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
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
                        "    user_id INTEGER," +
                        "    name TEXT NOT NULL," +
                        "    color TEXT" +
                        ")";

        String createTopicsTable =
                "CREATE TABLE IF NOT EXISTS topics (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    name TEXT NOT NULL," +
                        "    completed INTEGER NOT NULL DEFAULT 0," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)" +
                        ")";

        // --- Milestone 2 tables ---------------------------------------

        String createTasksTable =
                "CREATE TABLE IF NOT EXISTS tasks (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    topic_id INTEGER," +
                        "    title TEXT NOT NULL," +
                        "    estimated_minutes INTEGER NOT NULL DEFAULT 0," +
                        "    priority TEXT NOT NULL DEFAULT 'MEDIUM'," +
                        "    deadline TEXT," +
                        "    completed INTEGER NOT NULL DEFAULT 0," +
                        "    status TEXT NOT NULL DEFAULT 'NOT_STARTED'," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                        ")";

        String createExamsTable =
                "CREATE TABLE IF NOT EXISTS exams (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    title TEXT NOT NULL," +
                        "    exam_date TEXT NOT NULL," +
                        "    progress INTEGER NOT NULL DEFAULT 0," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)" +
                        ")";

        String createStudySessionsTable =
                "CREATE TABLE IF NOT EXISTS study_sessions (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    topic_id INTEGER," +
                        "    session_date TEXT NOT NULL," +
                        "    duration_minutes INTEGER NOT NULL," +
                        "    notes TEXT," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                        ")";

        // --- Milestone 3 tables ---------------------------------------

        String createFlashcardsTable =
                "CREATE TABLE IF NOT EXISTS flashcards (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    topic_id INTEGER," +
                        "    front TEXT NOT NULL," +
                        "    back TEXT NOT NULL," +
                        "    difficult INTEGER NOT NULL DEFAULT 0," +
                        "    revision_status TEXT NOT NULL DEFAULT 'NOT_STARTED'," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                        ")";

        String createQuizQuestionsTable =
                "CREATE TABLE IF NOT EXISTS quiz_questions (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    topic_id INTEGER," +
                        "    question_text TEXT NOT NULL," +
                        "    option_a TEXT NOT NULL," +
                        "    option_b TEXT NOT NULL," +
                        "    option_c TEXT NOT NULL," +
                        "    option_d TEXT NOT NULL," +
                        "    correct_option TEXT NOT NULL," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                        ")";

        String createQuizAttemptsTable =
                "CREATE TABLE IF NOT EXISTS quiz_attempts (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    subject_id INTEGER NOT NULL," +
                        "    topic_id INTEGER," +
                        "    attempt_date TEXT NOT NULL," +
                        "    total_questions INTEGER NOT NULL," +
                        "    correct_answers INTEGER NOT NULL," +
                        "    score_percent INTEGER NOT NULL," +
                        "    time_taken_seconds INTEGER NOT NULL DEFAULT 0," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)" +
                        ")";

        String createQuizAttemptAnswersTable =
                "CREATE TABLE IF NOT EXISTS quiz_attempt_answers (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    attempt_id INTEGER NOT NULL," +
                        "    question_id INTEGER NOT NULL," +
                        "    selected_option TEXT NOT NULL," +
                        "    correct INTEGER NOT NULL," +
                        "    FOREIGN KEY(attempt_id) REFERENCES quiz_attempts(id)," +
                        "    FOREIGN KEY(question_id) REFERENCES quiz_questions(id)" +
                        ")";

        // --- Topic dependency graph (Milestone 3) ------------------------

        String createTopicDependenciesTable =
                "CREATE TABLE IF NOT EXISTS topic_dependencies (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER," +
                        "    topic_id INTEGER NOT NULL," +
                        "    prerequisite_id INTEGER NOT NULL," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id)," +
                        "    FOREIGN KEY(prerequisite_id) REFERENCES topics(id)," +
                        "    UNIQUE(topic_id, prerequisite_id)" +
                        ")";

        String createUsersTable =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    name TEXT NOT NULL," +
                        "    email TEXT NOT NULL COLLATE NOCASE UNIQUE," +
                        "    password_hash TEXT NOT NULL," +
                        "    onboarding_completed INTEGER NOT NULL DEFAULT 0" +
                        ");";

        // --- Persistent "remember me" login sessions ----------------------

        String createRememberTokensTable =
                "CREATE TABLE IF NOT EXISTS remember_tokens (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER NOT NULL," +
                        "    selector TEXT NOT NULL UNIQUE," +
                        "    validator_hash TEXT NOT NULL," +
                        "    expires_at TEXT NOT NULL," +
                        "    FOREIGN KEY(user_id) REFERENCES users(id)" +
                        ")";

        String createUserPreferencesTable =
                "CREATE TABLE IF NOT EXISTS user_preferences (" +
                        "    user_id INTEGER PRIMARY KEY," +
                        "    theme TEXT NOT NULL DEFAULT 'LIGHT'," +
                        "    font_size TEXT NOT NULL DEFAULT 'MEDIUM'," +
                        "    study_reminders INTEGER NOT NULL DEFAULT 1," +
                        "    exam_reminders INTEGER NOT NULL DEFAULT 1," +
                        "    achievement_notifications INTEGER NOT NULL DEFAULT 1," +
                        "    streak_notifications INTEGER NOT NULL DEFAULT 1," +
                        "    pomodoro_duration INTEGER NOT NULL DEFAULT 25," +
                        "    pomodoro_short_break INTEGER NOT NULL DEFAULT 5," +
                        "    pomodoro_long_break INTEGER NOT NULL DEFAULT 15," +
                        "    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")";

        String createMindMapsTable =
                "CREATE TABLE IF NOT EXISTS mind_maps (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER NOT NULL," +
                        "    name TEXT NOT NULL," +
                        "    focused_key TEXT," +
                        "    created_at TEXT NOT NULL," +
                        "    updated_at TEXT NOT NULL," +
                        "    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")";

        String createMindMapNodesTable =
                "CREATE TABLE IF NOT EXISTS mind_map_nodes (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    map_id INTEGER NOT NULL," +
                        "    node_key TEXT NOT NULL," +
                        "    label TEXT NOT NULL," +
                        "    x_pos REAL NOT NULL," +
                        "    y_pos REAL NOT NULL," +
                        "    parent_key TEXT," +
                        "    FOREIGN KEY(map_id) REFERENCES mind_maps(id) ON DELETE CASCADE," +
                        "    UNIQUE(map_id,node_key)" +
                        ")";

        String createExamTopicsTable =
                "CREATE TABLE IF NOT EXISTS exam_topics (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER NOT NULL," +
                        "    exam_id INTEGER NOT NULL," +
                        "    topic_id INTEGER NOT NULL," +
                        "    FOREIGN KEY(exam_id) REFERENCES exams(id) ON DELETE CASCADE," +
                        "    FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE," +
                        "    UNIQUE(exam_id,topic_id)" +
                        ")";

        String createResourcesTable =
                "CREATE TABLE IF NOT EXISTS resources (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    user_id INTEGER NOT NULL," +
                        "    subject_id INTEGER," +
                        "    title TEXT NOT NULL," +
                        "    url TEXT NOT NULL," +
                        "    description TEXT," +
                        "    category TEXT," +
                        "    FOREIGN KEY(subject_id) REFERENCES subjects(id)," +
                        "    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createSubjectsTable);
            statement.execute(createTopicsTable);
            statement.execute(createTasksTable);
            statement.execute(createExamsTable);
            statement.execute(createStudySessionsTable);
            statement.execute(createFlashcardsTable);
            statement.execute(createQuizQuestionsTable);
            statement.execute(createQuizAttemptsTable);
            statement.execute(createQuizAttemptAnswersTable);
            statement.execute(createTopicDependenciesTable);
            statement.execute(createUsersTable);
            statement.execute(createRememberTokensTable);
            statement.execute(createUserPreferencesTable);
            statement.execute(createMindMapsTable);
            statement.execute(createMindMapNodesTable);
            statement.execute(createExamTopicsTable);
            statement.execute(createResourcesTable);
            migrateUserOnboardingState(statement);
            migrateStudyDataOwnership(statement);
            migrateTaskStatusColumn(statement);
            migrateQuizAttemptTimeColumn(statement);
        }
    }

    private static void migrateQuizAttemptTimeColumn(Statement statement) throws SQLException {
        boolean hasColumn = false;
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(quiz_attempts)")) {
            while (columns.next()) {
                if ("time_taken_seconds".equalsIgnoreCase(columns.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
        }
        if (!hasColumn) {
            // Attempts recorded before timing was tracked default to 0 seconds,
            // which the UI treats as "no recorded time" rather than an instant quiz.
            statement.execute("ALTER TABLE quiz_attempts ADD COLUMN time_taken_seconds INTEGER NOT NULL DEFAULT 0");
        }
    }

    private static void migrateTaskStatusColumn(Statement statement) throws SQLException {
        boolean hasColumn = false;
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(tasks)")) {
            while (columns.next()) {
                if ("status".equalsIgnoreCase(columns.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
        }
        if (!hasColumn) {
            statement.execute("ALTER TABLE tasks ADD COLUMN status TEXT NOT NULL DEFAULT 'NOT_STARTED'");
            // Existing tasks predate the status field - line their status
            // up with whatever "completed" already recorded for them.
            statement.executeUpdate("UPDATE tasks SET status='COMPLETED' WHERE completed=1");
        }
    }

    private static void migrateStudyDataOwnership(Statement statement) throws SQLException {
        String[] tables = {
                "subjects", "topics", "tasks", "exams", "study_sessions",
                "flashcards", "quiz_questions", "quiz_attempts",
                "quiz_attempt_answers", "topic_dependencies"
        };
        for (String table : tables) {
            boolean hasColumn = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (columns.next()) {
                    if ("user_id".equalsIgnoreCase(columns.getString("name"))) {
                        hasColumn = true;
                        break;
                    }
                }
            }
            if (!hasColumn) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN user_id INTEGER");
            }
        }

        // Data created before account support belongs to the original account.
        // If an account already exists, assign all unowned legacy rows to the
        // oldest account. If no account exists yet, UserService performs the
        // same claim immediately after the first registration.
        try (ResultSet users = statement.executeQuery("SELECT id FROM users ORDER BY id LIMIT 1")) {
            if (users.next()) {
                int firstUserId = users.getInt(1);
                for (String table : tables) {
                    statement.executeUpdate("UPDATE " + table + " SET user_id = " + firstUserId + " WHERE user_id IS NULL");
                }
            }
        }
    }

    private static void migrateUserOnboardingState(Statement statement) throws SQLException {
        // Existing Milestone 5A-2 users predate onboarding. Mark them complete
        // so the new first-launch tour is only shown to newly registered users.
        try (var columns = statement.executeQuery("PRAGMA table_info(users)")) {
            boolean hasColumn = false;
            while (columns.next()) {
                if ("onboarding_completed".equalsIgnoreCase(columns.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
            if (!hasColumn) {
                statement.execute("ALTER TABLE users ADD COLUMN onboarding_completed INTEGER NOT NULL DEFAULT 1");
            }
        }
    }
}