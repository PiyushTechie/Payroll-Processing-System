package com.example.PayrollProcessingSystem.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.PayrollProcessingSystem.entity.PayrollRecord;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByPayrollRunPayrollRunId(Long runId);

    Optional<PayrollRecord> findByPayrollRunPayrollRunIdAndEmployeeEmployeeId(Long runId, Long employeeId);
}