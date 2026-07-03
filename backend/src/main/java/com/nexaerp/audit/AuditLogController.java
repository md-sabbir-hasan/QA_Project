package com.nexaerp.audit;


import com.nexaerp.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;


    // Get all logs for a specific record
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/entity/{entityName}/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityHistory(
            @PathVariable String entityName,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getEntityHistory(entityName, entityId)));
    }

    // Get all activity of a specific user
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getUserActivity(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getUserActivity(userId)));
    }

    // Get all logs for an entity type
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/entity/{entityName}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityLogs(
            @PathVariable String entityName) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getEntityLogs(entityName)));
    }

}
