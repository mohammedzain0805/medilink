package com.medilink.service;

import com.medilink.model.Patient;
import com.medilink.model.Role;
import com.medilink.model.User;
import com.medilink.repository.UserRepository;
import com.medilink.util.PasswordHasher;

import java.time.LocalDate;

public class AuthenticationService {
    private final UserRepository users;
    private final AuditService audit;
    private final UserService userService;

    public AuthenticationService(UserRepository users, AuditService audit, UserService userService) {
        this.users = users;
        this.audit = audit;
        this.userService = userService;
    }

    // Returns the user on success, null on failure. Both outcomes are audited.
    public User login(String email, String password) {
        User user = users.findByEmail(email);
        if (user == null || !PasswordHasher.matches(password, user.getPasswordHash())) {
            audit.log(null, "LOGIN_FAILED for " + email, null);
            return null;
        }
        audit.log(user, "LOGIN", null);
        return user;
    }

    // Patients can sign themselves up; doctors and admins are created by an admin.
    public Patient register(String name, String email, String password,
                            LocalDate dob, String bloodGroup) {
        return userService.registerPatient(name, email, password, dob, bloodGroup);
    }

    public void logout(User user) { audit.log(user, "LOGOUT", null); }

    public boolean verifyRole(User user, Role role) {
        return user != null && user.getRole() == role;
    }
}