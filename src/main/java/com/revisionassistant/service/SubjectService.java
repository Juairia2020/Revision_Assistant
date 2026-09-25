package com.revisionassistant.service;

import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TopicDAO;
import com.revisionassistant.model.Subject;

import java.sql.SQLException;
import java.util.List;

/**
 * Validation and business rules for subjects. Controllers talk to this
 * class instead of the DAOs directly, so no SQL ever needs to appear
 * in a controller.
 */
public class SubjectService {

    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;

    public SubjectService() {
        this.subjectDAO = new SubjectDAO();
        this.topicDAO = new TopicDAO();
    }

    public Subject addSubject(String name, String color) throws SQLException {
        validateName(name);
        Subject subject = new Subject(name.trim(), color);
        return subjectDAO.insert(subject);
    }

    public List<Subject> getAllSubjects() throws SQLException {
        return subjectDAO.findAll();
    }

    public void updateSubject(Subject subject) throws SQLException {
        validateName(subject.getName());
        subjectDAO.update(subject);
    }

    /**
     * Deletes a subject only if it has no topics left. This is checked
     * here (for a friendly message) and is also backed by the foreign
     * key constraint at the database level as a safety net.
     */
    public void deleteSubject(int subjectId) throws SQLException {
        int topicCount = topicDAO.countBySubjectId(subjectId);
        if (topicCount > 0) {
            throw new IllegalStateException(
                    "This subject still has " + topicCount + " topic(s). "
                            + "Delete or move its topics before removing the subject.");
        }
        subjectDAO.delete(subjectId);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be empty.");
        }
    }
}
