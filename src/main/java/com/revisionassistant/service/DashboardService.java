package com.revisionassistant.service;

import com.revisionassistant.model.Exam;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
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

    public DashboardService() {
        this.subjectService = new SubjectService();
        this.topicService = new TopicService();
        this.taskService = new TaskService();
        this.examService = new ExamService();
        this.studySessionService = new StudySessionService();
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
}
