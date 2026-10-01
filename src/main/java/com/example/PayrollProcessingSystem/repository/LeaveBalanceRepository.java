package com.example.PayrollProcessingSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.LeaveBalance;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveType;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployee_EmployeeIdAndLeaveType(Long employeeId, LeaveType leaveType);

    List<LeaveBalance> findByEmployee_EmployeeId(Long employeeId);

    boolean existsByEmployee_EmployeeIdAndLeaveType(Long employeeId, LeaveType leaveType);
}

