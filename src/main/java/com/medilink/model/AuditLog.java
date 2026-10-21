package com.medilink.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLog {
    private final String logId = UUID.randomUUID().toString();
    private final String actorId;
    private final String action;
    private final String targetRecordId;   // null when the action isn't about a record
    private final LocalDateTime timestamp = LocalDateTime.now();

    public AuditLog(String actorId, String action, String targetRecordId) {
        this.actorId = actorId;
        this.action = action;
        this.targetRecordId = targetRecordId;
    }

    public String getLogId() { return logId; }
    public String getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTargetRecordId() { return targetRecordId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return timestamp + " | " + actorId + " | " + action
                + (targetRecordId != null ? " | record " + targetRecordId : "");
    }
}