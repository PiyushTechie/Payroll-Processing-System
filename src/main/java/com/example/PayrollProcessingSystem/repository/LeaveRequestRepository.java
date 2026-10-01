package com.example.PayrollProcessingSystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.LeaveRequest;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployee_EmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployee_EmployeeIdAndStatus(Long employeeId, LeaveRequestStatus status);

    List<LeaveRequest> findByStatus(LeaveRequestStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.employeeId = :employeeId " +
           "AND lr.status IN (com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus.PENDING, " +
           "com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus.APPROVED) " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

