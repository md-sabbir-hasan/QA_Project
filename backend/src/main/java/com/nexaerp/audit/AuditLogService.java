package com.nexaerp.audit;

import java.util.List;

public interface AuditLogService {
    // Save a log entry
    void log(AuditAction action, String entityName, Long entityId,
             String oldValue, String newValue);

    // Get history of a specific record
    List<AuditLog> getEntityHistory(String entityName, Long entityId);

    // Get activity of a specific user
    List<AuditLog> getUserActivity(Long userId);

    // Get all logs for an entity type
    List<AuditLog> getEntityLogs(String entityName);
}
