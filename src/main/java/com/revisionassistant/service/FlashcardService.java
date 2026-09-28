package com.revisionassistant.service;

import com.revisionassistant.dao.FlashcardDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.dao.TopicDAO;
import com.revisionassistant.model.Flashcard;
import com.revisionassistant.model.RevisionStatus;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validation and business rules for flashcards. Controllers talk to
 * this class instead of the DAOs directly, so no SQL ever needs to
 * appear in a controller.
 */
public class FlashcardService {

    private final FlashcardDAO flashcardDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;

    public FlashcardService() {
        this.flashcardDAO = new FlashcardDAO();
        this.subjectDAO = new SubjectDAO();
        this.topicDAO = new TopicDAO();
    }

    public Flashcard addFlashcard(int subjectId, Integer topicId, String front, String back) throws SQLException {
        validateSubject(subjectId);
        validateContent(front, back);
        Flashcard flashcard = new Flashcard(subjectId, topicId, front.trim(), back.trim(),
                false, RevisionStatus.NOT_STARTED);
        return flashcardDAO.insert(flashcard);
    }

    public List<Flashcard> getAllFlashcards() throws SQLException {
        return flashcardDAO.findAll();
    }

    /**
     * Applies the basic filters used by the Flashcards screen. Any
     * parameter left as {@code null} means "no filter on this field".
     * Filtering happens in memory since flashcard lists stay small
     * for a single user, matching {@code TaskService.getFilteredTasks}.
     */
    public List<Flashcard> getFilteredFlashcards(Integer subjectId, Boolean difficult, RevisionStatus status)
            throws SQLException {
        return flashcardDAO.findAll().stream()
                .filter(card -> subjectId == null || card.getSubjectId() == subjectId)
                .filter(card -> difficult == null || card.isDifficult() == difficult)
                .filter(card -> status == null || card.getRevisionStatus() == status)
                .collect(Collectors.toList());
    }

    public void updateFlashcard(Flashcard flashcard) throws SQLException {
        validateSubject(flashcard.getSubjectId());
        validateContent(flashcard.getFront(), flashcard.getBack());
        flashcardDAO.update(flashcard);
    }

    public void setDifficult(int flashcardId, boolean difficult) throws SQLException {
        flashcardDAO.updateDifficult(flashcardId, difficult);
    }

    public void setRevisionStatus(int flashcardId, RevisionStatus status) throws SQLException {
        flashcardDAO.updateRevisionStatus(flashcardId, status);
    }

    public void deleteFlashcard(int flashcardId) throws SQLException {
        flashcardDAO.delete(flashcardId);
    }

    /** How many flashcards are currently marked difficult. */
    public int getDifficultCount() throws SQLException {
        return (int) flashcardDAO.findAll().stream().filter(Flashcard::isDifficult).count();
    }

    /** How many flashcards have not yet been fully revised (not marked REVISED). */
    public int getCardsToReviseCount() throws SQLException {
        return (int) flashcardDAO.findAll().stream()
                .filter(card -> card.getRevisionStatus() != RevisionStatus.REVISED)
                .count();
    }

    /**
     * The most difficult flashcards, ready for display on the
     * Dashboard, with their subject/topic names already resolved.
     */
    public List<DifficultCardSummary> getMostDifficultCards(int maxCount) throws SQLException {
        List<Flashcard> difficultCards = flashcardDAO.findAll().stream()
                .filter(Flashcard::isDifficult)
                .collect(Collectors.toList());

        List<DifficultCardSummary> result = new ArrayList<>();
        for (Flashcard card : difficultCards) {
            if (result.size() >= maxCount) {
                break;
            }
            result.add(new DifficultCardSummary(card.getFront(), subjectName(card.getSubjectId()),
                    topicName(card.getTopicId())));
        }
        return result;
    }

    private String subjectName(int subjectId) throws SQLException {
        Subject subject = subjectDAO.findById(subjectId);
        return subject == null ? "Unknown subject" : subject.getName();
    }

    private String topicName(Integer topicId) throws SQLException {
        if (topicId == null) {
            return null;
        }
        Topic topic = topicDAO.findById(topicId);
        return topic == null ? null : topic.getName();
    }

    private void validateContent(String front, String back) {
        if (front == null || front.trim().isEmpty()) {
            throw new IllegalArgumentException("Flashcard front cannot be empty.");
        }
        if (back == null || back.trim().isEmpty()) {
            throw new IllegalArgumentException("Flashcard back cannot be empty.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }

    /**
     * Read-only summary of one difficult flashcard, with its subject
     * and (optional) topic name already resolved. Not a database
     * entity - it only exists to carry dashboard data from this
     * service to the controller, matching
     * {@code DashboardService.SubjectProgress}.
     */
    public static class DifficultCardSummary {
        private final String front;
        private final String subjectName;
        private final String topicName;

        public DifficultCardSummary(String front, String subjectName, String topicName) {
            this.front = front;
            this.subjectName = subjectName;
            this.topicName = topicName;
        }

        public String getFront() {
            return front;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTopicName() {
            return topicName;
        }
    }
}
