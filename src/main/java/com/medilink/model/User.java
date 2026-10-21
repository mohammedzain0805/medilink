package com.medilink.model;

import com.medilink.util.Validator;

public abstract class User {
    private final String userId;
    private String fullName;
    private String email;
    private final Role role;
    private final String passwordHash;

    protected User(String userId, String fullName, String email, Role role, String passwordHash) {
        this.userId = userId;
        this.role = role;
        this.passwordHash = passwordHash;
        setFullName(fullName);
        setEmail(email);
    }

    @Override
    public String toString() { return fullName; }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }

    public final void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");
        this.fullName = fullName;
    }

    public final void setEmail(String email) {
        if (!Validator.isValidEmail(email))
            throw new IllegalArgumentException("Invalid email: " + email);
        this.email = email;
    }

    // Each role decides for itself - this is runtime polymorphism too.
    public abstract boolean hasAccessTo(MedicalRecord record);
}