package com.revisionassistant.model;

/**
 * Minimal local user account used by the desktop authentication flow.
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private boolean onboardingCompleted;

    public User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public User(int id, String name, String email, String passwordHash) {
        this(id, name, email, passwordHash, true);
    }

    public User(int id, String name, String email, String passwordHash, boolean onboardingCompleted) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.onboardingCompleted = onboardingCompleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }
}
