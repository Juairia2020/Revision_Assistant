package com.revisionassistant.service;

import com.revisionassistant.dao.FlashcardDAO;
import com.revisionassistant.dao.QuizAttemptDAO;
import com.revisionassistant.dao.QuizQuestionDAO;
import com.revisionassistant.dao.StudySessionDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TaskDAO;
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
    private final TaskDAO taskDAO;
    private final StudySessionDAO studySessionDAO;
    private final FlashcardDAO flashcardDAO;
    private final QuizQuestionDAO quizQuestionDAO;
    private final QuizAttemptDAO quizAttemptDAO;

    public TopicService() {
        this.topicDAO = new TopicDAO();
        this.subjectDAO = new SubjectDAO();
        this.taskDAO = new TaskDAO();
        this.studySessionDAO = new StudySessionDAO();
        this.flashcardDAO = new FlashcardDAO();
        this.quizQuestionDAO = new QuizQuestionDAO();
        this.quizAttemptDAO = new QuizAttemptDAO();
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

    /**
     * Deletes a topic only if no task or study session still points
     * to it. Tasks and sessions can also be logged against a subject
     * with no specific topic, so this does not block deleting the
     * subject itself.
     */
    public void deleteTopic(int topicId) throws SQLException {
        int taskCount = taskDAO.countByTopicId(topicId);
        int sessionCount = studySessionDAO.countByTopicId(topicId);
        int flashcardCount = flashcardDAO.countByTopicId(topicId);
        int quizQuestionCount = quizQuestionDAO.countByTopicId(topicId);
        int quizAttemptCount = quizAttemptDAO.countByTopicId(topicId);
        if (taskCount > 0 || sessionCount > 0
                || flashcardCount > 0 || quizQuestionCount > 0 || quizAttemptCount > 0) {
            throw new IllegalStateException(
                    "This topic still has tasks, study sessions, flashcards or quiz data linked to it. "
                            + "Remove those first before deleting the topic.");
        }
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
