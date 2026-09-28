package com.revisionassistant.service;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.QuizAttempt;
import com.revisionassistant.model.StudySession;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the data every other feature already stores - quiz attempts,
 * study sessions, tasks, exam/topic links and topic prerequisites -
 * into one ranked, explained signal: which topic is most worth
 * studying next, and why.
 * <p>
 * This class stores nothing of its own. It is a read-only aggregation
 * layer, the same role {@link DashboardService} plays for dashboard
 * statistics, so that {@code StudyPathController} and
 * {@code DashboardService} can share one definition of "what matters
 * right now" instead of each guessing separately.
 */
public class TopicMasteryService {

    /** How well a topic seems to be understood, from the evidence available so far. */
    public enum MasteryLevel {
        NOT_STARTED("Not started"),
        WEAK("Weak"),
        LEARNING("Learning"),
        STRONG("Strong"),
        MASTERED("Mastered");

        private final String label;

        MasteryLevel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** A quiz average below this is treated as evidence the topic is not yet understood. */
    private static final int WEAK_SCORE_THRESHOLD = 60;
    /** A quiz average at or above this, on a completed topic, counts as mastered rather than merely strong. */
    private static final int STRONG_SCORE_THRESHOLD = 80;

    /** How many days out an exam still makes the topics it covers more urgent. */
    private static final int EXAM_URGENT_WINDOW_DAYS = 14;

    private static final int BASE_WEAK = 55;
    private static final int BASE_NOT_STARTED = 40;
    private static final int BASE_LEARNING = 25;
    private static final int BASE_STRONG = 8;
    private static final int BASE_MASTERED = 0;
    private static final int EXAM_URGENCY_MAX_BOOST = 35;
    private static final int OVERDUE_TASK_BOOST = 12;

    private final TopicService topicService;
    private final SubjectService subjectService;
    private final QuizService quizService;
    private final StudySessionService studySessionService;
    private final TaskService taskService;
    private final ExamService examService;
    private final TopicDependencyService dependencyService;

    public TopicMasteryService() {
        this.topicService = new TopicService();
        this.subjectService = new SubjectService();
        this.quizService = new QuizService();
        this.studySessionService = new StudySessionService();
        this.taskService = new TaskService();
        this.examService = new ExamService();
        this.dependencyService = new TopicDependencyService();
    }

    /** One insight per topic in the system, most urgent to study first. */
    public List<TopicInsight> getInsights() throws SQLException {
        List<Topic> topics = topicService.getAllTopics();
        if (topics.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> subjectNamesById = new HashMap<>();
        for (Subject subject : subjectService.getAllSubjects()) {
            subjectNamesById.put(subject.getId(), subject.getName());
        }

        Map<Integer, List<QuizAttempt>> attemptsByTopic = quizService.getAllAttempts().stream()
                .filter(attempt -> attempt.getTopicId() != null)
                .collect(Collectors.groupingBy(QuizAttempt::getTopicId));

        Set<Integer> studiedTopicIds = studySessionService.getAllSessions().stream()
                .map(StudySession::getTopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Task> overdueTaskByTopic = new HashMap<>();
        for (Task task : taskService.getAllTasks()) {
            if (task.getTopicId() != null && task.isOverdue()) {
                overdueTaskByTopic.putIfAbsent(task.getTopicId(), task);
            }
        }

        Map<Integer, ExamUrgency> examUrgencyByTopic = new HashMap<>();
        for (Exam exam : examService.getAllExams()) {
            if (exam.isPast() || exam.getDaysRemaining() > EXAM_URGENT_WINDOW_DAYS) {
                continue;
            }
            for (Integer topicId : examService.getTopicIds(exam.getId())) {
                ExamUrgency current = examUrgencyByTopic.get(topicId);
                if (current == null || exam.getDaysRemaining() < current.daysRemaining) {
                    examUrgencyByTopic.put(topicId, new ExamUrgency(exam.getTitle(), exam.getDaysRemaining()));
                }
            }
        }

        List<TopicInsight> insights = new ArrayList<>();
        for (Topic topic : topics) {
            List<QuizAttempt> topicAttempts = attemptsByTopic.getOrDefault(topic.getId(), List.of());
            int attemptCount = topicAttempts.size();
            int avgScore = attemptCount == 0 ? -1 : (int) Math.round(
                    topicAttempts.stream().mapToInt(QuizAttempt::getScorePercent).average().orElse(0));
            boolean studied = studiedTopicIds.contains(topic.getId());

            boolean unmetPrerequisite = false;
            String blockingPrerequisiteName = null;
            for (Topic prerequisite : dependencyService.getDirectPrerequisites(topic.getId())) {
                if (!prerequisite.isCompleted()) {
                    unmetPrerequisite = true;
                    blockingPrerequisiteName = prerequisite.getName();
                    break;
                }
            }

            MasteryLevel level = deriveLevel(topic.isCompleted(), attemptCount, avgScore, studied);
            ExamUrgency urgency = examUrgencyByTopic.get(topic.getId());
            Task overdueTask = overdueTaskByTopic.get(topic.getId());

            int score = scoreTopic(level, urgency, overdueTask);
            String reason = buildReason(level, avgScore, urgency, overdueTask, unmetPrerequisite, blockingPrerequisiteName);
            String subjectName = subjectNamesById.getOrDefault(topic.getSubjectId(), "Subject");

            insights.add(new TopicInsight(topic, subjectName, level, score, reason,
                    attemptCount, avgScore, unmetPrerequisite, blockingPrerequisiteName));
        }

        insights.sort(Comparator.comparingInt(TopicInsight::getPriorityScore).reversed());
        return insights;
    }

    /**
     * The single most worthwhile topic to study next, across every
     * subject. Completed topics and topics whose prerequisites are
     * not yet done are skipped - a topic gated on a prerequisite
     * simply never outranks that prerequisite, so recommendations
     * naturally follow the dependency chain instead of needing
     * special-case logic here.
     */
    public Optional<TopicInsight> getRecommendedNext() throws SQLException {
        return eligible(getInsights().stream()).max(Comparator.comparingInt(TopicInsight::getPriorityScore));
    }

    /** The most worthwhile topic to study next within one subject. */
    public Optional<TopicInsight> getRecommendedNext(int subjectId) throws SQLException {
        return eligible(getInsights().stream().filter(insight -> insight.getTopic().getSubjectId() == subjectId))
                .max(Comparator.comparingInt(TopicInsight::getPriorityScore));
    }

    /**
     * This topic's own insight, computed the same way as every other
     * topic's - no separate scoring path. Used to answer "is this one
     * topic weak?" right after a quiz, without recomputing anything
     * differently than {@link #getInsights()} already does.
     */
    public Optional<TopicInsight> getInsightForTopic(int topicId) throws SQLException {
        return getInsights().stream().filter(insight -> insight.getTopic().getId() == topicId).findFirst();
    }

    private java.util.stream.Stream<TopicInsight> eligible(java.util.stream.Stream<TopicInsight> insights) {
        return insights
                .filter(insight -> !insight.getTopic().isCompleted())
                .filter(insight -> !insight.hasUnmetPrerequisite());
    }

    private MasteryLevel deriveLevel(boolean completed, int attemptCount, int avgScore, boolean studied) {
        if (attemptCount > 0) {
            if (avgScore < WEAK_SCORE_THRESHOLD) {
                return MasteryLevel.WEAK;
            }
            if (avgScore < STRONG_SCORE_THRESHOLD) {
                return completed ? MasteryLevel.STRONG : MasteryLevel.LEARNING;
            }
            return completed ? MasteryLevel.MASTERED : MasteryLevel.STRONG;
        }
        if (completed) {
            return MasteryLevel.STRONG;
        }
        return studied ? MasteryLevel.LEARNING : MasteryLevel.NOT_STARTED;
    }

    private int scoreTopic(MasteryLevel level, ExamUrgency urgency, Task overdueTask) {
        int base = switch (level) {
            case WEAK -> BASE_WEAK;
            case NOT_STARTED -> BASE_NOT_STARTED;
            case LEARNING -> BASE_LEARNING;
            case STRONG -> BASE_STRONG;
            case MASTERED -> BASE_MASTERED;
        };
        int examBoost = urgency == null ? 0 : (int) Math.max(0, EXAM_URGENCY_MAX_BOOST - urgency.daysRemaining * 2);
        int taskBoost = overdueTask != null ? OVERDUE_TASK_BOOST : 0;
        return base + examBoost + taskBoost;
    }

    private String buildReason(MasteryLevel level, int avgScore, ExamUrgency urgency,
                                Task overdueTask, boolean unmetPrerequisite, String blockingPrerequisiteName) {
        if (unmetPrerequisite) {
            return "Complete \"" + blockingPrerequisiteName + "\" first - it's a prerequisite for this topic.";
        }
        if (urgency != null) {
            String when = urgency.daysRemaining <= 0 ? "very soon"
                    : urgency.daysRemaining == 1 ? "in 1 day" : "in " + urgency.daysRemaining + " days";
            return "\"" + urgency.examTitle + "\" is " + when + " and covers this topic.";
        }
        if (level == MasteryLevel.WEAK) {
            return "Scoring " + avgScore + "% on quizzes here - worth revisiting.";
        }
        if (overdueTask != null) {
            return "An overdue task, \"" + overdueTask.getTitle() + "\", is linked to this topic.";
        }
        if (level == MasteryLevel.NOT_STARTED) {
            return "Not started yet.";
        }
        if (level == MasteryLevel.LEARNING) {
            return "You've studied this, but it hasn't been tested yet.";
        }
        return "Keeps your study path moving forward.";
    }

    private static final class ExamUrgency {
        private final String examTitle;
        private final long daysRemaining;

        private ExamUrgency(String examTitle, long daysRemaining) {
            this.examTitle = examTitle;
            this.daysRemaining = daysRemaining;
        }
    }

    /**
     * Read-only summary of one topic's studied-ness: how well it
     * seems to be understood, how urgent it is right now, and why.
     * Not a database entity - exists only to carry this service's
     * output to controllers, matching the {@code *Summary} /
     * {@code *Progress} pattern used by {@link DashboardService} and
     * {@link QuizService}.
     */
    public static class TopicInsight {
        private final Topic topic;
        private final String subjectName;
        private final MasteryLevel level;
        private final int priorityScore;
        private final String reason;
        private final int attemptCount;
        private final int averageScorePercent;
        private final boolean unmetPrerequisite;
        private final String blockingPrerequisiteName;

        public TopicInsight(Topic topic, String subjectName, MasteryLevel level, int priorityScore, String reason,
                             int attemptCount, int averageScorePercent, boolean unmetPrerequisite,
                             String blockingPrerequisiteName) {
            this.topic = topic;
            this.subjectName = subjectName;
            this.level = level;
            this.priorityScore = priorityScore;
            this.reason = reason;
            this.attemptCount = attemptCount;
            this.averageScorePercent = averageScorePercent;
            this.unmetPrerequisite = unmetPrerequisite;
            this.blockingPrerequisiteName = blockingPrerequisiteName;
        }

        public Topic getTopic() {
            return topic;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public MasteryLevel getLevel() {
            return level;
        }

        public int getPriorityScore() {
            return priorityScore;
        }

        public String getReason() {
            return reason;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        /** -1 when the topic has never been quizzed. */
        public int getAverageScorePercent() {
            return averageScorePercent;
        }

        public boolean hasUnmetPrerequisite() {
            return unmetPrerequisite;
        }

        public String getBlockingPrerequisiteName() {
            return blockingPrerequisiteName;
        }
    }
}
