package com.example.PayrollProcessingSystem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByChangedAtDesc();
    List<AuditLog> findByEntityNameIgnoreCaseOrderByChangedAtDesc(String entityName);
}
