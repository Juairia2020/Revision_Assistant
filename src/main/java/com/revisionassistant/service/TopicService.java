package com.revisionassistant.service;

import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TopicDAO;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
import java.util.List;

/**
 * Validation and business rules for topics. Controllers talk to this
 * class instead of the DAOs directly, so no SQL ever needs to appear
 * in a controller.
 */
public class TopicService {

    private final TopicDAO topicDAO;
    private final SubjectDAO subjectDAO;

    public TopicService() {
        this.topicDAO = new TopicDAO();
        this.subjectDAO = new SubjectDAO();
    }

    public Topic addTopic(int subjectId, String name) throws SQLException {
        validateSubject(subjectId);
        validateName(name);
        Topic topic = new Topic(subjectId, name.trim(), false);
        return topicDAO.insert(topic);
    }

    public List<Topic> getTopicsForSubject(int subjectId) throws SQLException {
        return topicDAO.findBySubjectId(subjectId);
    }

    public void updateTopic(Topic topic) throws SQLException {
        validateSubject(topic.getSubjectId());
        validateName(topic.getName());
        topicDAO.update(topic);
    }

    public void setCompleted(int topicId, boolean completed) throws SQLException {
        topicDAO.updateCompleted(topicId, completed);
    }

    public void deleteTopic(int topicId) throws SQLException {
        topicDAO.delete(topicId);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic name cannot be empty.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }
}
