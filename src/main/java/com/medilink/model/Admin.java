package com.medilink.model;

import com.medilink.service.AuditService;
import com.medilink.service.UserService;

import java.util.List;

public class Admin extends User {
    public Admin(String userId, String fullName, String email, String passwordHash) {
        super(userId, fullName, email, Role.ADMIN, passwordHash);
    }

    @Override
    public boolean hasAccessTo(MedicalRecord record) {
        return false; // admins manage accounts and audit logs, not medical data
    }

    // Operations from the class diagram
    public List<User> manageUserAccounts(UserService service) { return service.listUsers(this); }

    public List<AuditLog> viewAuditLogs(AuditService audit) { return audit.getAllLogs(this); }
}