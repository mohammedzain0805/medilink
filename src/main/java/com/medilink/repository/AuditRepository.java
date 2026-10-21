package com.medilink.repository;

import com.medilink.model.AuditLog;
import java.util.List;

public interface AuditRepository {
    void save(AuditLog log);
    List<AuditLog> findByRecord(String recordId);
    List<AuditLog> findAll();
}