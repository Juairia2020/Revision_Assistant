package com.revisionassistant.service;

import com.revisionassistant.dao.QuizAttemptAnswerDAO;
import com.revisionassistant.dao.QuizAttemptDAO;
import com.revisionassistant.dao.QuizQuestionDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.model.QuizAttempt;
import com.revisionassistant.model.QuizAttemptAnswer;
import com.revisionassistant.model.QuizOption;
import com.revisionassistant.model.QuizQuestion;
import com.revisionassistant.model.Subject;
import com.revisionassistant.model.Topic;
import com.revisionassistant.dao.TopicDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validation and business rules for quiz questions and quiz attempts.
 * Combines question management with taking a quiz and scoring it,
 * since both revolve around the same small set of tables and are
 * always used together. Controllers talk to this class instead of
 * the DAOs directly, so no SQL ever needs to appear in a controller.
 */
public class QuizService {

    private final QuizQuestionDAO questionDAO;
    private final QuizAttemptDAO attemptDAO;
    private final QuizAttemptAnswerDAO attemptAnswerDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;

    public QuizService() {
        this.questionDAO = new QuizQuestionDAO();
        this.attemptDAO = new QuizAttemptDAO();
        this.attemptAnswerDAO = new QuizAttemptAnswerDAO();
        this.subjectDAO = new SubjectDAO();
        this.topicDAO = new TopicDAO();
    }

    // ----- Question management -------------------------------------

    public QuizQuestion addQuestion(int subjectId, Integer topicId, String questionText,
                                    String optionA, String optionB, String optionC, String optionD,
                                    QuizOption correctOption) throws SQLException {
        validateSubject(subjectId);
        validateQuestion(questionText, optionA, optionB, optionC, optionD, correctOption);
        QuizQuestion question = new QuizQuestion(subjectId, topicId, questionText.trim(),
                optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(), correctOption);
        return questionDAO.insert(question);
    }

    public List<QuizQuestion> getAllQuestions() throws SQLException {
        return questionDAO.findAll();
    }

    /** Any parameter left as {@code null} means "no filter on this field". */
    public List<QuizQuestion> getFilteredQuestions(Integer subjectId, Integer topicId) throws SQLException {
        return questionDAO.findAll().stream()
                .filter(q -> subjectId == null || q.getSubjectId() == subjectId)
                .filter(q -> topicId == null || topicId.equals(q.getTopicId()))
                .collect(Collectors.toList());
    }

    public void updateQuestion(QuizQuestion question) throws SQLException {
        validateSubject(question.getSubjectId());
        validateQuestion(question.getQuestionText(), question.getOptionA(), question.getOptionB(),
                question.getOptionC(), question.getOptionD(), question.getCorrectOption());
        questionDAO.update(question);
    }

    /**
     * Deletes a question only if it was never answered in a stored
     * quiz attempt, so past results stay meaningful.
     */
    public void deleteQuestion(int questionId) throws SQLException {
        if (attemptAnswerDAO.countByQuestionId(questionId) > 0) {
            throw new IllegalStateException(
                    "This question is part of a stored quiz result and cannot be deleted.");
        }
        questionDAO.delete(questionId);
    }

    // ----- Taking a quiz ---------------------------------------------

    /** The questions available for a quiz on a subject, optionally narrowed to one topic. */
    public List<QuizQuestion> getQuestionsForQuiz(int subjectId, Integer topicId) throws SQLException {
        validateSubject(subjectId);
        return getFilteredQuestions(subjectId, topicId);
    }

    /**
     * Scores a completed quiz and stores the attempt together with
     * each individual answer. Every question passed in must have a
     * matching entry in {@code answers}.
     */
    public QuizAttempt submitAttempt(int subjectId, Integer topicId, List<QuizQuestion> questions,
                                     Map<Integer, QuizOption> answers, int timeTakenSeconds) throws SQLException {
        return submitAttempt(subjectId, topicId, questions, answers, timeTakenSeconds, false);
    }

    /**
     * Scores a completed quiz and stores the attempt together with
     * each individual answer.
     * <p>
     * When {@code allowUnanswered} is {@code false} (a normal manual
     * submission), every question passed in must have a matching
     * entry in {@code answers}. When {@code true} (an automatic
     * submission because the quiz's time limit ran out), any question
     * missing an answer is simply scored as incorrect instead of
     * rejecting the submission.
     */
    public QuizAttempt submitAttempt(int subjectId, Integer topicId, List<QuizQuestion> questions,
                                     Map<Integer, QuizOption> answers, int timeTakenSeconds,
                                     boolean allowUnanswered) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("There are no questions to submit.");
        }

        int correctCount = 0;
        Map<Integer, QuizOption> storedSelections = new HashMap<>();
        Map<Integer, Boolean> correctness = new HashMap<>();
        for (QuizQuestion question : questions) {
            QuizOption selected = answers.get(question.getId());
            if (selected == null) {
                if (!allowUnanswered) {
                    throw new IllegalArgumentException("Please answer every question before submitting.");
                }
                // No answer was picked before time ran out - record it as
                // incorrect. The placeholder option stored below is only
                // there to satisfy the database's "an option was picked"
                // column; it is never compared back against the correct
                // option, so it can't accidentally look like a lucky guess.
                selected = firstOptionOtherThan(question.getCorrectOption());
                storedSelections.put(question.getId(), selected);
                correctness.put(question.getId(), false);
                continue;
            }
            storedSelections.put(question.getId(), selected);
            boolean correct = selected == question.getCorrectOption();
            correctness.put(question.getId(), correct);
            if (correct) {
                correctCount++;
            }
        }

        int scorePercent = Math.round(100f * correctCount / questions.size());
        QuizAttempt attempt = new QuizAttempt(subjectId, topicId, LocalDate.now(),
                questions.size(), correctCount, scorePercent, Math.max(0, timeTakenSeconds));
        attemptDAO.insert(attempt);

        for (QuizQuestion question : questions) {
            QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt.getId(), question.getId(),
                    storedSelections.get(question.getId()), correctness.get(question.getId()));
            attemptAnswerDAO.insert(answer);
        }

        return attempt;
    }

    private QuizOption firstOptionOtherThan(QuizOption excluded) {
        for (QuizOption option : QuizOption.values()) {
            if (option != excluded) {
                return option;
            }
        }
        return QuizOption.A;
    }

    // ----- Attempt history / dashboard aggregation --------------------

    /** The most recent quiz attempts, most recent first. */
    public List<QuizAttempt> getRecentAttempts(int maxCount) throws SQLException {
        List<QuizAttempt> all = attemptDAO.findAll();
        return all.size() <= maxCount ? all : all.subList(0, maxCount);
    }

    public int getAttemptCount() throws SQLException {
        return attemptDAO.findAll().size();
    }

    /** Average score across every stored attempt, 0 if none have been taken yet. */
    public int getAverageScore() throws SQLException {
        List<QuizAttempt> attempts = attemptDAO.findAll();
        if (attempts.isEmpty()) {
            return 0;
        }
        double average = attempts.stream().mapToInt(QuizAttempt::getScorePercent).average().orElse(0);
        return (int) Math.round(average);
    }

    /**
     * The questions answered incorrectly most often, ready for
     * display on the Dashboard, with subject/topic names resolved.
     */
    public List<MissedQuestionSummary> getMostMissedQuestions(int maxCount) throws SQLException {
        List<QuizAttemptAnswer> incorrectAnswers = attemptAnswerDAO.findAllIncorrect();

        Map<Integer, Long> missCountByQuestionId = incorrectAnswers.stream()
                .collect(Collectors.groupingBy(QuizAttemptAnswer::getQuestionId, Collectors.counting()));

        List<Map.Entry<Integer, Long>> ranked = missCountByQuestionId.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed())
                .collect(Collectors.toList());

        List<MissedQuestionSummary> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : ranked) {
            if (result.size() >= maxCount) {
                break;
            }
            QuizQuestion question = questionDAO.findById(entry.getKey());
            if (question == null) {
                continue;
            }
            result.add(new MissedQuestionSummary(question.getQuestionText(), subjectName(question.getSubjectId()),
                    topicName(question.getTopicId()), entry.getValue().intValue()));
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

    private void validateQuestion(String questionText, String optionA, String optionB,
                                  String optionC, String optionD, QuizOption correctOption) {
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty.");
        }
        if (isBlank(optionA) || isBlank(optionB) || isBlank(optionC) || isBlank(optionD)) {
            throw new IllegalArgumentException("All four options must be filled in.");
        }
        if (correctOption == null) {
            throw new IllegalArgumentException("Please select the correct option.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectId <= 0 || subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject first.");
        }
    }

    /**
     * Read-only summary of one frequently-missed question, with its
     * subject/topic names already resolved and how many times it has
     * been answered incorrectly. Not a database entity - it only
     * exists to carry dashboard data from this service to the
     * controller, matching {@code DashboardService.SubjectProgress}.
     */
    public static class MissedQuestionSummary {
        private final String questionText;
        private final String subjectName;
        private final String topicName;
        private final int missCount;

        public MissedQuestionSummary(String questionText, String subjectName, String topicName, int missCount) {
            this.questionText = questionText;
            this.subjectName = subjectName;
            this.topicName = topicName;
            this.missCount = missCount;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getTopicName() {
            return topicName;
        }

        public int getMissCount() {
            return missCount;
        }
    }
}