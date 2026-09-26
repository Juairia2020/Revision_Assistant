package com.revisionassistant.service;

import com.revisionassistant.dao.ExamDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TopicDAO;
import com.revisionassistant.model.Exam;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Validation, persistence, and topic-derived progress rules for exams. */
public class ExamService {

    private final ExamDAO examDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;

    public ExamService() {
        this.examDAO = new ExamDAO();
        this.subjectDAO = new SubjectDAO();
        this.topicDAO = new TopicDAO();
    }

    /** Backward-compatible overload; exams created without topics start at 0%. */
    public Exam addExam(int subjectId, String title, LocalDate examDate, int ignoredProgress)
            throws SQLException {
        return addExam(subjectId, title, examDate, List.of());
    }

    public Exam addExam(int subjectId, String title, LocalDate examDate, List<Integer> topicIds)
            throws SQLException {
        validateSubject(subjectId);
        validateTitle(title);
        validateDate(examDate);
        validateTopicSelection(subjectId, topicIds);
        Exam exam = new Exam(subjectId, title.trim(), examDate, 0);
        examDAO.insert(exam);
        examDAO.replaceTopics(exam.getId(), topicIds == null ? List.of() : topicIds);
        exam.setProgress(examDAO.calculateProgress(exam.getId()));
        return exam;
    }

    public List<Exam> getAllExams() throws SQLException {
        List<Exam> exams = examDAO.findAll();
        for (Exam exam : exams) {
            exam.setProgress(examDAO.calculateProgress(exam.getId()));
        }
        return exams;
    }

    public List<Exam> getUpcomingExams(int maxCount) throws SQLException {
        return getAllExams().stream()
                .filter(exam -> !exam.isPast())
                .sorted(Comparator.comparing(Exam::getExamDate))
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopicIds(int examId) throws SQLException {
        return examDAO.findTopicIds(examId);
    }

    public int getProgress(int examId) throws SQLException {
        return examDAO.calculateProgress(examId);
    }

    public void updateExam(Exam exam, List<Integer> topicIds) throws SQLException {
        validateSubject(exam.getSubjectId());
        validateTitle(exam.getTitle());
        validateDate(exam.getExamDate());
        validateTopicSelection(exam.getSubjectId(), topicIds);
        exam.setProgress(0);
        examDAO.update(exam);
        examDAO.replaceTopics(exam.getId(), topicIds == null ? List.of() : topicIds);
        exam.setProgress(examDAO.calculateProgress(exam.getId()));
    }

    /** Backward-compatible overload for older callers. */
    public void updateExam(Exam exam) throws SQLException {
        updateExam(exam, examDAO.findTopicIds(exam.getId()));
    }

    public void deleteExam(int examId) throws SQLException {
        examDAO.delete(examId);
    }

    private void validateTopicSelection(int subjectId, List<Integer> topicIds) throws SQLException {
        if (topicIds == null || topicIds.isEmpty()) return;
        Set<Integer> ids = new HashSet<>(topicIds);
        for (Integer id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("Invalid topic selection.");
            }
            var topic = topicDAO.findById(id);
            if (topic == null || topic.getSubjectId() != subjectId) {
                throw new IllegalArgumentException("Every selected topic must belong to the selected subject.");
            }
        }
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
