package com.revisionassistant.service;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pulls together data from the other services into the small set of
 * summaries the Dashboard screen needs. Keeps that aggregation out of
 * the controller and out of the individual services, which only know
 * about their own table.
 */
public class DashboardService {

    private final SubjectService subjectService;
    private final TopicService topicService;
    private final TaskService taskService;
    private final ExamService examService;
    private final StudySessionService studySessionService;
    private final FlashcardService flashcardService;
    private final QuizService quizService;

    public DashboardService() {
        this.subjectService = new SubjectService();
        this.topicService = new TopicService();
        this.taskService = new TaskService();
        this.examService = new ExamService();
        this.studySessionService = new StudySessionService();
        this.flashcardService = new FlashcardService();
        this.quizService = new QuizService();
    }

    /** Tasks that are due today and not yet completed, most urgent-looking first. */
    public List<Task> getTodaysTasks() throws SQLException {
        return taskService.getAllTasks().stream()
                .filter(task -> !task.isCompleted() && task.isDueToday())
                .collect(Collectors.toList());
    }

    /** Tasks that are overdue and not yet completed. */
    public List<Task> getOverdueTasks() throws SQLException {
        return taskService.getAllTasks().stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
    }

    /** The next few upcoming exams, soonest first. */
    public List<Exam> getUpcomingExams(int maxCount) throws SQLException {
        return examService.getUpcomingExams(maxCount);
    }

    public int getMinutesStudiedToday() throws SQLException {
        return studySessionService.getMinutesStudiedToday();
    }

    public int getMinutesStudiedThisWeek() throws SQLException {
        return studySessionService.getMinutesStudiedThisWeek();
    }

    /** How many pending (not completed) tasks exist in total. */
    public int getPendingTaskCount() throws SQLException {
        return (int) taskService.getAllTasks().stream().filter(task -> !task.isCompleted()).count();
    }

    /** How many tasks have been completed in total. */
    public int getCompletedTaskCount() throws SQLException {
        return (int) taskService.getAllTasks().stream().filter(Task::isCompleted).count();
    }

    /** Per-subject progress based on how many of its topics are marked completed. */
    public List<SubjectProgress> getSubjectProgress() throws SQLException {
        List<Subject> subjects = subjectService.getAllSubjects();
        List<SubjectProgress> result = new ArrayList<>();

        for (Subject subject : subjects) {
            List<Topic> topics = topicService.getTopicsForSubject(subject.getId());
            int total = topics.size();
            long completed = topics.stream().filter(Topic::isCompleted).count();
            result.add(new SubjectProgress(subject.getName(), (int) completed, total));
        }

        result.sort(Comparator.comparing(SubjectProgress::getSubjectName));
        return result;
    }

    // ----- Milestone 3: flashcards and quiz summaries -----------------

    /** How many flashcards are currently marked difficult. */
    public int getDifficultFlashcardCount() throws SQLException {
        return flashcardService.getDifficultCount();
    }

    /** How many flashcards still need revision (not marked REVISED). */
    public int getCardsToReviseCount() throws SQLException {
        return flashcardService.getCardsToReviseCount();
    }

    /** The most difficult flashcards, for the "Difficult Flashcards" dashboard section. */
    public List<FlashcardService.DifficultCardSummary> getMostDifficultFlashcards(int maxCount) throws SQLException {
        return flashcardService.getMostDifficultCards(maxCount);
    }

    /** How many quizzes have been taken in total. */
    public int getQuizAttemptCount() throws SQLException {
        return quizService.getAttemptCount();
    }

    /** Average score (0-100) across every stored quiz attempt. */
    public int getAverageQuizScore() throws SQLException {
        return quizService.getAverageScore();
    }

    /** The most recent quiz attempts, with subject/topic names resolved, for the Dashboard. */
    public List<RecentAttempt> getRecentQuizAttempts(int maxCount) throws SQLException {
        List<RecentAttempt> result = new ArrayList<>();
        for (com.revisionassistant.model.QuizAttempt attempt : quizService.getRecentAttempts(maxCount)) {
            Subject subject = subjectService.getAllSubjects().stream()
                    .filter(s -> s.getId() == attempt.getSubjectId())
                    .findFirst().orElse(null);
            String subjectName = subject == null ? "Unknown subject" : subject.getName();

            String topicName = null;
            if (attempt.getTopicId() != null) {
                Topic topic = topicService.getTopicsForSubject(attempt.getSubjectId()).stream()
                        .filter(t -> t.getId() == attempt.getTopicId())
                        .findFirst().orElse(null);
                topicName = topic == null ? null : topic.getName();
            }

            result.add(new RecentAttempt(subjectName, topicName, attempt.getAttemptDate(),
                    attempt.getCorrectAnswers(), attempt.getTotalQuestions(), attempt.getScorePercent()));
        }
        return result;
    }

    /** The questions answered incorrectly most often, for the "Frequently Missed" dashboard section. */
    public List<QuizService.MissedQuestionSummary> getFrequentlyMissedQuestions(int maxCount) throws SQLException {
        return quizService.getMostMissedQuestions(maxCount);
    }

    /**
     * Read-only summary of one subject's topic completion. Not a
     * database entity - it only exists to carry dashboard data from
     * this service to the controller.
     */
    public static class SubjectProgress {
        private final String subjectName;
        private final int completedTopics;
        private final int totalTopics;

        public SubjectProgress(String subjectName, int completedTopics, int totalTopics) {
            this.subjectName = subjectName;
            this.completedTopics = completedTopics;
            this.totalTopics = totalTopics;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public int getCompletedTopics() {
            return completedTopics;
        }

        public int getTotalTopics() {
            return totalTopics;
        }

        /** Fraction completed, from 0.0 to 1.0, safe when there are no topics yet. */
        public double getFraction() {
            return totalTopics == 0 ? 0.0 : (double) completedTopics / totalTopics;
        }
    }

    /**
     * Read-only summary of one quiz attempt, with its subject/topic
     * names already resolved. Not a database entity - it only exists
     * to carry dashboard data from this service to the controller.
     */
    public static class RecentAttempt {
        private final String subjectName;
        private final String topicName;
        private final LocalDate attemptDate;
        private final int correctAnswers;
        private final int totalQuestions;
        private final int scorePercent;

        public RecentAttempt(String subjectName, String topicName, LocalDate attemptDate,
                              int correctAnswers, int totalQuestions, int scorePercent) {
            this.subjectName = subjectName;
            this.topicName = topicName;
            this.attemptDate = attemptDate;
            this.correctAnswers = correctAnswers;
            this.totalQuestions = totalQuestions;
            this.scorePercent = scorePercent;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTopicName() {
            return topicName;
        }

        public LocalDate getAttemptDate() {
            return attemptDate;
        }

        public int getCorrectAnswers() {
            return correctAnswers;
        }

        public int getTotalQuestions() {
            return totalQuestions;
        }

        public int getScorePercent() {
            return scorePercent;
        }
    }
}
