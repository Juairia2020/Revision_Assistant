package com.revisionassistant.service;

import com.revisionassistant.dao.RememberTokenDAO;
import com.revisionassistant.dao.UserDAO;
import com.revisionassistant.model.RememberToken;
import com.revisionassistant.model.User;
import com.revisionassistant.security.PasswordHasher;
import com.revisionassistant.security.RememberMeStore;
import com.revisionassistant.session.CurrentUser;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registration and authentication rules for local desktop users.
 */
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserDAO userDAO;
    private final RememberTokenDAO rememberTokenDAO;

    private static final int REMEMBER_ME_DAYS = 30;

    public UserService() {
        this.userDAO = new UserDAO();
        this.rememberTokenDAO = new RememberTokenDAO();
    }

    public User register(String name, String email, String password, String confirmation)
            throws SQLException {
        validateRegistration(name, email, password, confirmation);

        String normalizedEmail = normalizeEmail(email);
        if (userDAO.findByEmail(normalizedEmail) != null) {
            throw new IllegalStateException("An account with this email already exists.");
        }

        User user = new User(
                name.trim(),
                normalizedEmail,
                PasswordHasher.hash(password)
        );

        try {
            User savedUser = userDAO.insert(user);
            userDAO.claimLegacyStudyData(savedUser.getId());
            CurrentUser.set(savedUser);
            return savedUser;
        } catch (SQLException e) {
            // Keep the database UNIQUE constraint as the final protection.
            if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("unique")) {
                throw new IllegalStateException("An account with this email already exists.", e);
            }
            throw e;
        }
    }

    public User login(String email, String password) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        User user = userDAO.findByEmail(normalizeEmail(email));
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect email or password.");
        }

        CurrentUser.set(user);
        return user;
    }

    public void markOnboardingCompleted() throws SQLException {
        User user = CurrentUser.get();
        if (user == null) {
            return;
        }
        userDAO.markOnboardingCompleted(user.getId());
        user.setOnboardingCompleted(true);
    }

    public void logout() {
        forgetRememberedSession();
        CurrentUser.clear();
    }

    /**
     * Persists the current session so the application can sign the user
     * back in automatically the next time it starts, until they explicitly
     * log out. Call this only after {@link CurrentUser#set} has already run
     * (i.e. after a successful {@link #login} or {@link #register}).
     */
    public void rememberCurrentSession() throws SQLException {
        User user = CurrentUser.get();
        if (user == null) {
            return;
        }
        String selector = RememberMeStore.newSelector();
        String validator = RememberMeStore.newValidator();
        Instant expiresAt = Instant.now().plus(REMEMBER_ME_DAYS, ChronoUnit.DAYS);

        rememberTokenDAO.insert(new RememberToken(user.getId(), selector,
                RememberMeStore.hashValidator(validator), expiresAt));
        RememberMeStore.save(selector, validator);
    }

    /**
     * Signs the user back in from a previously remembered session, if a
     * valid one exists on this device. Returns {@code null} (and leaves
     * {@link CurrentUser} unset) if there is no remembered session, it has
     * expired, or it no longer matches what is stored in the database -
     * the normal login screen is shown in every one of those cases.
     */
    public User tryAutoLogin() throws SQLException {
        RememberMeStore.StoredToken stored = RememberMeStore.read();
        if (stored == null) {
            return null;
        }

        RememberToken record = rememberTokenDAO.findBySelector(stored.getSelector());
        if (record == null || record.isExpired()
                || !RememberMeStore.matches(stored.getValidator(), record.getValidatorHash())) {
            // Either never valid, expired, or someone tampered with the local file -
            // forget it on both sides and fall back to a normal login.
            if (record != null) {
                rememberTokenDAO.deleteBySelector(record.getSelector());
            }
            RememberMeStore.clear();
            return null;
        }

        User user = userDAO.findById(record.getUserId());
        if (user == null) {
            rememberTokenDAO.deleteBySelector(record.getSelector());
            RememberMeStore.clear();
            return null;
        }

        CurrentUser.set(user);
        return user;
    }

    /** Forgets any persisted "remember me" session for this device, without otherwise changing the session. */
    public void forgetRememberedSession() {
        RememberMeStore.StoredToken stored = RememberMeStore.read();
        if (stored != null) {
            try {
                rememberTokenDAO.deleteBySelector(stored.getSelector());
            } catch (SQLException e) {
                // Best-effort cleanup - the local file is cleared regardless below,
                // so the device will not auto sign in again either way.
            }
        }
        RememberMeStore.clear();
    }

    private void validateRegistration(String name, String email,
                                      String password, String confirmation) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (name.trim().length() > 80) {
            throw new IllegalArgumentException("Name must be 80 characters or fewer.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (confirmation == null || !password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
