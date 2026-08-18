package com.example.PayrollProcessingSystem.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.PayrollProcessingSystem.entity.Payslip;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    Optional<Payslip> findByPayrollRecordPayrollRecordId(Long recordId);

    List<Payslip> findByPayrollRecordEmployeeEmployeeId(Long employeeId);
}