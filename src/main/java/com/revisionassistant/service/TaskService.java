package com.revisionassistant.service;

import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TaskDAO;
import com.revisionassistant.model.Priority;
import com.revisionassistant.model.Task;
import com.revisionassistant.model.TaskStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validation and business rules for revision tasks, including the
 * basic in-memory filtering used by the Tasks screen. Controllers
 * talk to this class instead of the DAOs directly, so no SQL ever
 * needs to appear in a controller.
 */
public class TaskService {

    private final TaskDAO taskDAO;
    private final SubjectDAO subjectDAO;

    public TaskService() {
        this.taskDAO = new TaskDAO();
        this.subjectDAO = new SubjectDAO();
    }

    /**
     * Adds a task with an explicit starting status (e.g. Not started,
     * In progress, Completed).
     */
    public Task addTask(int subjectId, Integer topicId, String title, int estimatedMinutes,
                         Priority priority, LocalDate deadline, TaskStatus startingStatus) throws SQLException {
        validateSubject(subjectId);
        validateTitle(title);
        Task task = new Task(subjectId, topicId, title.trim(), Math.max(estimatedMinutes, 0),
                priority == null ? Priority.MEDIUM : priority, deadline,
                startingStatus == null ? TaskStatus.NOT_STARTED : startingStatus);
        return taskDAO.insert(task);
    }

    /** Adds a task that starts out as "Not started". */
    public Task addTask(int subjectId, Integer topicId, String title, int estimatedMinutes,
                         Priority priority, LocalDate deadline) throws SQLException {
        return addTask(subjectId, topicId, title, estimatedMinutes, priority, deadline, TaskStatus.NOT_STARTED);
    }

    public List<Task> getAllTasks() throws SQLException {
        return taskDAO.findAll();
    }

    /**
     * Applies the basic filters used by the Tasks screen. Any
     * parameter left as {@code null} means "no filter on this field".
     * Filtering happens in memory since task lists stay small for a
     * single user, which keeps the SQL in the DAO simple.
     */
    public List<Task> getFilteredTasks(Integer subjectId, Boolean completed, Priority priority)
            throws SQLException {
        return taskDAO.findAll().stream()
                .filter(task -> subjectId == null || task.getSubjectId() == subjectId)
                .filter(task -> completed == null || task.isCompleted() == completed)
                .filter(task -> priority == null || task.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public void updateTask(Task task) throws SQLException {
        validateSubject(task.getSubjectId());
        validateTitle(task.getTitle());
        taskDAO.update(task);
    }

    public void setCompleted(int taskId, boolean completed) throws SQLException {
        taskDAO.updateCompleted(taskId, completed);
    }

    public void deleteTask(int taskId) throws SQLException {
        taskDAO.delete(taskId);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }
}
