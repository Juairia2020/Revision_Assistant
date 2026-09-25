package com.revisionassistant.service;

import com.revisionassistant.dao.StudySessionDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.model.StudySession;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Validation and business rules for study sessions. Controllers talk
 * to this class instead of the DAOs directly, so no SQL ever needs to
 * appear in a controller.
 */
public class StudySessionService {

    private final StudySessionDAO studySessionDAO;
    private final SubjectDAO subjectDAO;

    public StudySessionService() {
        this.studySessionDAO = new StudySessionDAO();
        this.subjectDAO = new SubjectDAO();
    }

    public StudySession addSession(int subjectId, Integer topicId, LocalDate date,
                                    int durationMinutes, String notes) throws SQLException {
        validateSubject(subjectId);
        validateDate(date);
        validateDuration(durationMinutes);
        StudySession session = new StudySession(subjectId, topicId, date,
                durationMinutes, notes == null ? null : notes.trim());
        return studySessionDAO.insert(session);
    }

    public List<StudySession> getAllSessions() throws SQLException {
        return studySessionDAO.findAll();
    }

    public void updateSession(StudySession session) throws SQLException {
        validateSubject(session.getSubjectId());
        validateDate(session.getDate());
        validateDuration(session.getDurationMinutes());
        studySessionDAO.update(session);
    }

    public void deleteSession(int sessionId) throws SQLException {
        studySessionDAO.delete(sessionId);
    }

    /** Total minutes studied today, for the dashboard. */
    public int getMinutesStudiedToday() throws SQLException {
        return getMinutesStudiedSince(LocalDate.now());
    }

    /** Total minutes studied in the last 7 days (including today), for the dashboard. */
    public int getMinutesStudiedThisWeek() throws SQLException {
        return getMinutesStudiedSince(LocalDate.now().minusDays(6));
    }

    private int getMinutesStudiedSince(LocalDate earliestDate) throws SQLException {
        return studySessionDAO.findAll().stream()
                .filter(session -> !session.getDate().isBefore(earliestDate)
                        && !session.getDate().isAfter(LocalDate.now()))
                .mapToInt(StudySession::getDurationMinutes)
                .sum();
    }

    private void validateDuration(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero minutes.");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Please choose a session date.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }
}
