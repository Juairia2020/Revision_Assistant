package com.revisionassistant.service;

import com.revisionassistant.dao.UserDAO;
import com.revisionassistant.model.User;
import com.revisionassistant.security.PasswordHasher;
import com.revisionassistant.session.CurrentUser;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registration and authentication rules for local desktop users.
 */
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
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
        CurrentUser.clear();
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
