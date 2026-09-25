package com.revisionassistant.service;

import com.revisionassistant.dao.ExamDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.model.Exam;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validation and business rules for exams. Controllers talk to this
 * class instead of the DAOs directly, so no SQL ever needs to appear
 * in a controller.
 */
public class ExamService {

    private final ExamDAO examDAO;
    private final SubjectDAO subjectDAO;

    public ExamService() {
        this.examDAO = new ExamDAO();
        this.subjectDAO = new SubjectDAO();
    }

    public Exam addExam(int subjectId, String title, LocalDate examDate, int progress)
            throws SQLException {
        validateSubject(subjectId);
        validateTitle(title);
        validateDate(examDate);
        Exam exam = new Exam(subjectId, title.trim(), examDate, clampProgress(progress));
        return examDAO.insert(exam);
    }

    public List<Exam> getAllExams() throws SQLException {
        return examDAO.findAll();
    }

    /** Exams that have not happened yet, soonest first, for the dashboard. */
    public List<Exam> getUpcomingExams(int maxCount) throws SQLException {
        return examDAO.findAll().stream()
                .filter(exam -> !exam.isPast())
                .sorted(Comparator.comparing(Exam::getExamDate))
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    public void updateExam(Exam exam) throws SQLException {
        validateSubject(exam.getSubjectId());
        validateTitle(exam.getTitle());
        validateDate(exam.getExamDate());
        exam.setProgress(clampProgress(exam.getProgress()));
        examDAO.update(exam);
    }

    public void deleteExam(int examId) throws SQLException {
        examDAO.delete(examId);
    }

    private int clampProgress(int progress) {
        return Math.max(0, Math.min(100, progress));
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Exam title cannot be empty.");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Please choose an exam date.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }
}
