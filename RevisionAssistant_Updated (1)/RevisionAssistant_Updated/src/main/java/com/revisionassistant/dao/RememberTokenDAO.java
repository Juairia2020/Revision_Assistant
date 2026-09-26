package com.revisionassistant.dao;

import com.revisionassistant.database.DatabaseManager;
import com.revisionassistant.model.RememberToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Direct SQL access for persisted "remember me" login tokens. Controllers
 * never execute SQL - {@code UserService} is the only caller.
 * <p>
 * Only a selector (a lookup key, safe to store in plain text) and a hash
 * of the validator are ever written to the database; the plain validator
 * itself is never persisted here - see
 * {@code com.revisionassistant.security.RememberMeStore}.
 */
public class RememberTokenDAO {

    public void insert(RememberToken token) throws SQLException {
        String sql = "INSERT INTO remember_tokens (user_id, selector, validator_hash, expires_at) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, token.getUserId());
            statement.setString(2, token.getSelector());
            statement.setString(3, token.getValidatorHash());
            statement.setString(4, token.getExpiresAt().toString());
            statement.executeUpdate();
        }
    }

    public RememberToken findBySelector(String selector) throws SQLException {
        String sql = "SELECT user_id, selector, validator_hash, expires_at "
                + "FROM remember_tokens WHERE selector = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selector);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new RememberToken(
                            resultSet.getInt("user_id"),
                            resultSet.getString("selector"),
                            resultSet.getString("validator_hash"),
                            Instant.parse(resultSet.getString("expires_at"))
                    );
                }
            }
        }
        return null;
    }

    public void deleteBySelector(String selector) throws SQLException {
        String sql = "DELETE FROM remember_tokens WHERE selector = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selector);
            statement.executeUpdate();
        }
    }

    /** Removes tokens whose expiry has already passed, so the table does not grow forever. */
    public void deleteExpired() throws SQLException {
        String sql = "DELETE FROM remember_tokens WHERE expires_at < ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Instant.now().toString());
            statement.executeUpdate();
        }
    }
}
