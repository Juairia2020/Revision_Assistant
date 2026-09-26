package com.revisionassistant.service;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.StudySession;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates existing application services into read-only dashboard summaries.
 * No dashboard-specific database schema or DAO operations are required.
 */
public class DashboardService {

    private final SubjectService subjectService = new SubjectService();
    private final TopicService topicService = new TopicService();
    private final TaskService taskService = new TaskService();
    private final ExamService examService = new ExamService();
    private final StudySessionService studySessionService = new StudySessionService();
    private final FlashcardService flashcardService = new FlashcardService();
    private final QuizService quizService = new QuizService();

    public List<Task> getTodaysTasks() throws SQLException {
        return taskService.getAllTasks().stream()
                .filter(task -> !task.isCompleted() && task.isDueToday())
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueTasks() throws SQLException {
        return taskService.getAllTasks().stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
    }

    public List<Exam> getUpcomingExams(int maxCount) throws SQLException {
        return examService.getUpcomingExams(maxCount);
    }

    public int getMinutesStudiedToday() throws SQLException {
        return studySessionService.getMinutesStudiedToday();
    }

    public int getMinutesStudiedThisWeek() throws SQLException {
        return studySessionService.getMinutesStudiedThisWeek();
    }

    public int getPendingTaskCount() throws SQLException {
        return (int) taskService.getAllTasks().stream().filter(task -> !task.isCompleted()).count();
    }

    public int getCompletedTaskCount() throws SQLException {
        return (int) taskService.getAllTasks().stream().filter(Task::isCompleted).count();
    }

    /** Overall progress = completed topics / all topics. */
    public int getOverallProgressPercent() throws SQLException {
        List<Topic> topics = topicService.getAllTopics();
        if (topics.isEmpty()) {
            return 0;
        }
        long completed = topics.stream().filter(Topic::isCompleted).count();
        return (int) Math.round(completed * 100.0 / topics.size());
    }

    public int getTotalTopicCount() throws SQLException {
        return topicService.getAllTopics().size();
    }

    public int getCompletedTopicCount() throws SQLException {
        return (int) topicService.getAllTopics().stream().filter(Topic::isCompleted).count();
    }

    /** Per-subject progress based on completed topics, not manual percentages. */
    public List<SubjectProgress> getSubjectProgress() throws SQLException {
        List<Subject> subjects = subjectService.getAllSubjects();
        List<SubjectProgress> result = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Topic> topics = topicService.getTopicsForSubject(subject.getId());
            int total = topics.size();
            int completed = (int) topics.stream().filter(Topic::isCompleted).count();
            result.add(new SubjectProgress(subject.getName(), completed, total));
        }
        result.sort(Comparator.comparing(SubjectProgress::getSubjectName));
        return result;
    }

    /**
     * Exam preparation shown here is the exam's own {@code progress} value -
     * the same one set by the preparation slider on the Exam screen - not a
     * value recalculated from topic completion. Topic counts are included
     * only as supporting context (how much of the subject is covered), so
     * the percentage shown always matches what the user actually set.
     */
    public List<ExamProgress> getUpcomingExamProgress(int maxCount) throws SQLException {
        List<Subject> subjects = subjectService.getAllSubjects();
        List<ExamProgress> result = new ArrayList<>();
        for (Exam exam : examService.getUpcomingExams(maxCount)) {
            Subject subject = subjects.stream()
                    .filter(s -> s.getId() == exam.getSubjectId())
                    .findFirst().orElse(null);
            String subjectName = subject == null ? "Subject" : subject.getName();
            List<Topic> topics = topicService.getTopicsForSubject(exam.getSubjectId());
            int total = topics.size();
            int completed = (int) topics.stream().filter(Topic::isCompleted).count();
            int progress = Math.max(0, Math.min(100, exam.getProgress()));
            result.add(new ExamProgress(exam.getTitle(), subjectName, exam.getExamDate(),
                    exam.getDaysRemaining(), completed, total, progress));
        }
        return result;
    }

    /** Study minutes grouped by date for the requested number of recent days. */
    public List<StudyActivity> getStudyActivity(int days) throws SQLException {
        int safeDays = Math.max(1, days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(safeDays - 1L);
        Map<LocalDate, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            totals.put(start.plusDays(i), 0);
        }
        for (StudySession session : studySessionService.getAllSessions()) {
            LocalDate date = session.getDate();
            if (date != null && !date.isBefore(start) && !date.isAfter(end)) {
                totals.computeIfPresent(date, (key, value) -> value + session.getDurationMinutes());
            }
        }
        return totals.entrySet().stream()
                .map(entry -> new StudyActivity(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /** First useful existing item to continue with; no new recommendation algorithm. */
    public ContinueItem getContinueItem() throws SQLException {
        List<Task> tasks = taskService.getAllTasks().stream()
                .filter(task -> !task.isCompleted())
                .sorted(Comparator.comparing(Task::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!tasks.isEmpty()) {
            return new ContinueItem("Study Planner", tasks.get(0).getTitle(), "task");
        }

        for (SubjectProgress subject : getSubjectProgress()) {
            if (subject.getCompletedTopics() < subject.getTotalTopics()) {
                return new ContinueItem("Topics", subject.getSubjectName(), "topics");
            }
        }

        List<Exam> exams = examService.getUpcomingExams(1);
        if (!exams.isEmpty()) {
            return new ContinueItem("Exams", exams.get(0).getTitle(), "exams");
        }
        return new ContinueItem("Subjects", "Add your first subject to begin studying", "subjects");
    }

    public int getDifficultFlashcardCount() throws SQLException { return flashcardService.getDifficultCount(); }
    public int getCardsToReviseCount() throws SQLException { return flashcardService.getCardsToReviseCount(); }
    public List<FlashcardService.DifficultCardSummary> getMostDifficultFlashcards(int maxCount) throws SQLException {
        return flashcardService.getMostDifficultCards(maxCount);
    }
    public int getQuizAttemptCount() throws SQLException { return quizService.getAttemptCount(); }
    public int getAverageQuizScore() throws SQLException { return quizService.getAverageScore(); }
    public List<QuizService.MissedQuestionSummary> getFrequentlyMissedQuestions(int maxCount) throws SQLException {
        return quizService.getMostMissedQuestions(maxCount);
    }

    public static class SubjectProgress {
        private final String subjectName;
        private final int completedTopics;
        private final int totalTopics;
        public SubjectProgress(String subjectName, int completedTopics, int totalTopics) {
            this.subjectName = subjectName;
            this.completedTopics = completedTopics;
            this.totalTopics = totalTopics;
        }
        public String getSubjectName() { return subjectName; }
        public int getCompletedTopics() { return completedTopics; }
        public int getTotalTopics() { return totalTopics; }
        public double getFraction() { return totalTopics == 0 ? 0.0 : (double) completedTopics / totalTopics; }
        public int getPercent() { return (int) Math.round(getFraction() * 100); }
    }

    public static class ExamProgress {
        private final String title;
        private final String subjectName;
        private final LocalDate examDate;
        private final long daysRemaining;
        private final int completedTopics;
        private final int totalTopics;
        private final int progressPercent;
        public ExamProgress(String title, String subjectName, LocalDate examDate, long daysRemaining,
                            int completedTopics, int totalTopics, int progressPercent) {
            this.title = title;
            this.subjectName = subjectName;
            this.examDate = examDate;
            this.daysRemaining = daysRemaining;
            this.completedTopics = completedTopics;
            this.totalTopics = totalTopics;
            this.progressPercent = progressPercent;
        }
        public String getTitle() { return title; }
        public String getSubjectName() { return subjectName; }
        public LocalDate getExamDate() { return examDate; }
        public long getDaysRemaining() { return daysRemaining; }
        public int getCompletedTopics() { return completedTopics; }
        public int getTotalTopics() { return totalTopics; }
        public int getProgressPercent() { return progressPercent; }
    }

    public static class StudyActivity {
        private final LocalDate date;
        private final int minutes;
        public StudyActivity(LocalDate date, int minutes) { this.date = date; this.minutes = minutes; }
        public LocalDate getDate() { return date; }
        public int getMinutes() { return minutes; }
    }

    public static class ContinueItem {
        private final String section;
        private final String title;
        private final String action;
        public ContinueItem(String section, String title, String action) {
            this.section = section; this.title = title; this.action = action;
        }
        public String getSection() { return section; }
        public String getTitle() { return title; }
        public String getAction() { return action; }
    }
}
