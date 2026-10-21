package com.medilink.repository;

import com.medilink.model.AuditLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryAuditRepository implements AuditRepository {
    private final List<AuditLog> logs = new ArrayList<>();

    @Override public void save(AuditLog log) { logs.add(log); }

    @Override
    public List<AuditLog> findByRecord(String recordId) {
        List<AuditLog> result = new ArrayList<>();
        for (AuditLog l : logs)
            if (recordId.equals(l.getTargetRecordId())) result.add(l);
        return result;
    }

    @Override public List<AuditLog> findAll() { return Collections.unmodifiableList(logs); }
}