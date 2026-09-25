package com.revisionassistant.service;

import com.revisionassistant.algorithm.Knapsack;
import com.revisionassistant.algorithm.PlanningItem;
import com.revisionassistant.algorithm.SumOfSubsets;
import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Task;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the student's pending tasks into a recommended study plan
 * that fits their available time. This is the service layer between
 * the controller and the algorithm package: it is responsible for
 * everything algorithm-agnostic (which tasks are eligible, how
 * "valuable" each one is right now) while the actual optimisation is
 * delegated to {@link Knapsack} or {@link SumOfSubsets}.
 */
public class StudyPlannerService {

    /** Which combinatorial optimisation problem the planner solves. */
    public enum Strategy {
        /** 0/1 Knapsack: maximise priority/urgency/exam-relevance score within the time limit. */
        PRIORITY_BASED,
        /** Sum of Subsets: maximise the total time actually used, ignoring task importance. */
        MAXIMIZE_TIME_USED
    }

    /** How many days out an exam still counts as "coming up soon" for the exam-relevance score. */
    private static final int EXAM_RELEVANCE_WINDOW_DAYS = 14;

    /** Upper bound on the planner's time budget, so the DP tables stay a sane size. */
    private static final int MAX_AVAILABLE_MINUTES = 24 * 60;

    private static final int PRIORITY_HIGH_SCORE = 30;
    private static final int PRIORITY_MEDIUM_SCORE = 20;
    private static final int PRIORITY_LOW_SCORE = 10;
    private static final int MAX_URGENCY_SCORE = 30;
    private static final int EXAM_RELEVANCE_BOOST = 15;

    private final TaskService taskService;
    private final ExamService examService;

    public StudyPlannerService() {
        this.taskService = new TaskService();
        this.examService = new ExamService();
    }

    /**
     * Builds a study plan for the given amount of free time.
     * Completed tasks, and tasks too long to ever fit, are excluded
     * before the algorithm even runs.
     */
    public StudyPlan generatePlan(int availableMinutes, Strategy strategy) throws SQLException {
        if (availableMinutes <= 0) {
            throw new IllegalArgumentException("Enter how many minutes you have available.");
        }
        int capacity = Math.min(availableMinutes, MAX_AVAILABLE_MINUTES);

        List<Task> eligibleTasks = taskService.getAllTasks().stream()
                .filter(task -> !task.isCompleted())
                .filter(task -> task.getEstimatedMinutes() > 0 && task.getEstimatedMinutes() <= capacity)
                .collect(Collectors.toList());

        if (eligibleTasks.isEmpty()) {
            return new StudyPlan(List.of(), 0, capacity, 0, strategy);
        }

        Set<Integer> subjectsWithUpcomingExam = subjectsWithUpcomingExam();

        Map<Integer, Task> tasksById = new HashMap<>();
        List<PlanningItem> items = new ArrayList<>();
        for (Task task : eligibleTasks) {
            items.add(new PlanningItem(task.getId(), task.getEstimatedMinutes(),
                    scoreTask(task, subjectsWithUpcomingExam)));
            tasksById.put(task.getId(), task);
        }

        if (strategy == Strategy.PRIORITY_BASED) {
            Knapsack.Result result = Knapsack.solve(items, capacity);
            List<Task> selected = mapToTasks(result.getSelectedItems(), tasksById);
            return new StudyPlan(selected, result.getTotalWeight(), capacity, result.getTotalValue(), strategy);
        }

        SumOfSubsets.Result result = SumOfSubsets.solve(items, capacity);
        List<Task> selected = mapToTasks(result.getSelectedItems(), tasksById);
        int totalValue = selected.stream().mapToInt(task -> scoreTask(task, subjectsWithUpcomingExam)).sum();
        return new StudyPlan(selected, result.getTotalWeight(), capacity, totalValue, strategy);
    }

    private List<Task> mapToTasks(List<PlanningItem> items, Map<Integer, Task> tasksById) {
        return items.stream().map(item -> tasksById.get(item.getId())).collect(Collectors.toList());
    }

    private Set<Integer> subjectsWithUpcomingExam() throws SQLException {
        return examService.getAllExams().stream()
                .filter(exam -> !exam.isPast() && exam.getDaysRemaining() <= EXAM_RELEVANCE_WINDOW_DAYS)
                .map(Exam::getSubjectId)
                .collect(Collectors.toSet());
    }

    /**
     * How valuable it is to study this task right now: base points
     * for its priority, more points the closer (or more overdue) its
     * deadline is, and a flat boost if its subject has an exam coming
     * up soon. Purely additive and easy to explain: higher is better,
     * nothing here is a probability or a percentage.
     */
    private int scoreTask(Task task, Set<Integer> subjectsWithUpcomingExam) {
        int priorityScore;
        switch (task.getPriority()) {
            case HIGH:
                priorityScore = PRIORITY_HIGH_SCORE;
                break;
            case MEDIUM:
                priorityScore = PRIORITY_MEDIUM_SCORE;
                break;
            default:
                priorityScore = PRIORITY_LOW_SCORE;
        }

        int urgencyScore = 0;
        if (task.getDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());
            urgencyScore = daysLeft < 0 ? MAX_URGENCY_SCORE : (int) Math.max(0, MAX_URGENCY_SCORE - daysLeft);
        }

        int examBoost = subjectsWithUpcomingExam.contains(task.getSubjectId()) ? EXAM_RELEVANCE_BOOST : 0;

        return priorityScore + urgencyScore + examBoost;
    }

    /** The result of generating a plan: which tasks were recommended, and how the time budget was used. */
    public static class StudyPlan {
        private final List<Task> recommendedTasks;
        private final int minutesUsed;
        private final int minutesAvailable;
        private final int totalValue;
        private final Strategy strategy;

        public StudyPlan(List<Task> recommendedTasks, int minutesUsed, int minutesAvailable,
                          int totalValue, Strategy strategy) {
            this.recommendedTasks = recommendedTasks;
            this.minutesUsed = minutesUsed;
            this.minutesAvailable = minutesAvailable;
            this.totalValue = totalValue;
            this.strategy = strategy;
        }

        public List<Task> getRecommendedTasks() {
            return recommendedTasks;
        }

        public int getMinutesUsed() {
            return minutesUsed;
        }

        public int getMinutesAvailable() {
            return minutesAvailable;
        }

        public int getTotalValue() {
            return totalValue;
        }

        public Strategy getStrategy() {
            return strategy;
        }
    }
}
