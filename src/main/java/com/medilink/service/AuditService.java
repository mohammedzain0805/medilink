package com.medilink.service;

import com.medilink.model.*;
import com.medilink.repository.AuditRepository;
import java.util.List;

public class AuditService {
    private final AuditRepository repo;

    public AuditService(AuditRepository repo) { this.repo = repo; }

    // actor may be null (e.g. a failed login by someone unknown); target may be null
    public void log(User actor, String action, MedicalRecord target) {
        String actorId = actor == null ? "anonymous" : actor.getUserId();
        String targetId = target == null ? null : target.getRecordId();
        repo.save(new AuditLog(actorId, action, targetId));
    }

    public List<AuditLog> getLogsFor(MedicalRecord record) {
        return repo.findByRecord(record.getRecordId());
    }

    // Role-based access control: only admins may read the full log.
    public List<AuditLog> getAllLogs(User requester) {
        if (requester == null || requester.getRole() != Role.ADMIN)
            throw new SecurityException("Only admins can view audit logs");
        log(requester, "VIEW_AUDIT_LOGS", null);   // even the admins get watched
        return repo.findAll();
    }
}