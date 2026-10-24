package com.example.PayrollProcessingSystem.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByPayrollRecordPayrollRunPayrollRunId(Long runId);

    List<PaymentTransaction> findByPayrollRecordPayrollRecordId(Long payrollRecordId);

    List<PaymentTransaction> findByEmployeeEmployeeId(Long employeeId);
}
