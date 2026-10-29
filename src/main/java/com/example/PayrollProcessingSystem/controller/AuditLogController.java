package com.example.PayrollProcessingSystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.PayrollProcessingSystem.entity.AuditLog;
import com.example.PayrollProcessingSystem.repository.AuditLogRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Endpoints for viewing and tracking system audit logs")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Operation(summary = "Get all audit logs", description = "Retrieves all audit logs ordered by newest first. Can optionally filter by entity name (e.g. Employee).")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'FINANCE')")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam(required = false) String entityName) {
        if (entityName != null && !entityName.trim().isEmpty()) {
            return ResponseEntity.ok(auditLogRepository.findByEntityNameIgnoreCaseOrderByChangedAtDesc(entityName.trim()));
        }
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByChangedAtDesc());
    }

    @Operation(summary = "Get audit log by ID", description = "Retrieves a single audit log entry by its primary key ID.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'FINANCE')")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
