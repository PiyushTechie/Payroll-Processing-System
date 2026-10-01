package com.example.PayrollProcessingSystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.LeaveBalance;
import com.example.PayrollProcessingSystem.entity.LeaveRequest;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveType;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import com.example.PayrollProcessingSystem.exception.ResourceNotFoundException;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.LeaveBalanceRepository;
import com.example.PayrollProcessingSystem.repository.LeaveRequestRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import com.example.PayrollProcessingSystem.response.LeaveApplicationPayload;
import com.example.PayrollProcessingSystem.response.LeaveApprovalPayload;
import com.example.PayrollProcessingSystem.response.LeaveBalancePayload;
import com.example.PayrollProcessingSystem.response.LeaveBalanceResponsePayload;
import com.example.PayrollProcessingSystem.response.LeaveResponsePayload;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for employee leave management.
 * Implements complex business logic for leave application validation,
 * balance verification, approval workflow, balance deduction,
 * cancellation, and attendance synchronization.
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    /**
     * Submit a new leave application.
     * Validates employee existence, date continuity, overlapping leaves,
     * and verifies sufficient leave balance before permitting the application.
     */
    @PostMapping("/apply")
    public ResponseEntity<LeaveResponsePayload> applyLeave(@Valid @RequestBody LeaveApplicationPayload payload) {
        Employee employee = employeeRepository.findById(payload.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + payload.getEmployeeId()));

        if (payload.getStartDate().isAfter(payload.getEndDate())) {
            throw new BadRequestException("Leave start date cannot be after end date.");
        }

        long requestedDays = ChronoUnit.DAYS.between(payload.getStartDate(), payload.getEndDate()) + 1;

        // Check for overlapping active requests
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingRequests(
                employee.getEmployeeId(), payload.getStartDate(), payload.getEndDate());

        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Employee already has an active (PENDING or APPROVED) leave in the requested date range.");
        }

        // Leave balance check for paid leave types
        if (payload.getLeaveType() != LeaveType.UNPAID) {
            LeaveBalance balance = leaveBalanceRepository
                    .findByEmployee_EmployeeIdAndLeaveType(employee.getEmployeeId(), payload.getLeaveType())
                    .orElseThrow(() -> new BadRequestException("No leave balance record found for employee for leave type: " + payload.getLeaveType()));

            if (balance.getRemainingLeaves() < requestedDays) {
                throw new BadRequestException(String.format(
                        "Insufficient leave balance for %s. Available: %d days, Requested: %d days.",
                        payload.getLeaveType(), balance.getRemainingLeaves(), requestedDays));
            }
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(payload.getLeaveType())
                .startDate(payload.getStartDate())
                .endDate(payload.getEndDate())
                .reason(payload.getReason())
                .status(LeaveRequestStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return new ResponseEntity<>(mapToResponsePayload(saved), HttpStatus.CREATED);
    }

    /**
     * Review (Approve or Reject) a leave request.
     * Restricted to HR or ADMIN roles.
     * Deducts leave balance when approved and synchronizes attendance records.
     */
    @PutMapping("/review")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<LeaveResponsePayload> reviewLeave(
            @Valid @RequestBody LeaveApprovalPayload payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LeaveRequest leaveRequest = leaveRequestRepository.findById(payload.getLeaveRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + payload.getLeaveRequestId()));

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BadRequestException("Leave request cannot be reviewed. Current status is already: " + leaveRequest.getStatus());
        }

        if (payload.getNewStatus() != LeaveRequestStatus.APPROVED && payload.getNewStatus() != LeaveRequestStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED.");
        }

        User reviewer = null;
        if (userDetails != null && userDetails.getUser() != null) {
            reviewer = userDetails.getUser();
        } else {
            // Fallback: look up user from database or use first admin if testing without session
            reviewer = userRepository.findAll().stream().findFirst().orElse(null);
        }

        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;

        if (payload.getNewStatus() == LeaveRequestStatus.APPROVED) {
            // Deduct leave balance for paid leaves
            if (leaveRequest.getLeaveType() != LeaveType.UNPAID) {
                LeaveBalance balance = leaveBalanceRepository
                        .findByEmployee_EmployeeIdAndLeaveType(leaveRequest.getEmployee().getEmployeeId(), leaveRequest.getLeaveType())
                        .orElseThrow(() -> new BadRequestException("Leave balance not found for leave type: " + leaveRequest.getLeaveType()));

                if (balance.getRemainingLeaves() < days) {
                    throw new BadRequestException(String.format(
                            "Cannot approve leave: insufficient balance remaining. Available: %d, Required: %d",
                            balance.getRemainingLeaves(), days));
                }

                balance.setUsedLeaves(balance.getUsedLeaves() + (int) days);
                leaveBalanceRepository.save(balance);
            }

            leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
            leaveRequest.setApprovedBy(reviewer);

            // Synchronize attendance records: mark dates as ON_LEAVE
            LocalDate current = leaveRequest.getStartDate();
            while (!current.isAfter(leaveRequest.getEndDate())) {
                LocalDate curDate = current;
                Attendance attendance = attendanceRepository
                        .findByEmployee_EmployeeIdAndAttendanceDate(leaveRequest.getEmployee().getEmployeeId(), curDate)
                        .orElse(Attendance.builder()
                                .employee(leaveRequest.getEmployee())
                                .attendanceDate(curDate)
                                .build());

                attendance.setStatus(AttendanceStatus.ON_LEAVE);
                attendance.setWorkingHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                attendance.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                attendance.setRemarks("Approved Leave: " + leaveRequest.getLeaveType()
                        + (payload.getRemarks() != null ? " - " + payload.getRemarks() : ""));
                attendanceRepository.save(attendance);

                current = current.plusDays(1);
            }
        } else {
            leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
            leaveRequest.setApprovedBy(reviewer);
        }

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return ResponseEntity.ok(mapToResponsePayload(updated));
    }

    /**
     * Cancel a leave request.
     * Restores leave balance and cleans up attendance if previously approved.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveResponsePayload> cancelLeave(@PathVariable Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));

        if (leaveRequest.getStatus() == LeaveRequestStatus.CANCELLED || leaveRequest.getStatus() == LeaveRequestStatus.REJECTED) {
            throw new BadRequestException("Leave request cannot be cancelled from current status: " + leaveRequest.getStatus());
        }

        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;

        // If it was already approved, revert the deducted balance
        if (leaveRequest.getStatus() == LeaveRequestStatus.APPROVED && leaveRequest.getLeaveType() != LeaveType.UNPAID) {
            leaveBalanceRepository
                    .findByEmployee_EmployeeIdAndLeaveType(leaveRequest.getEmployee().getEmployeeId(), leaveRequest.getLeaveType())
                    .ifPresent(balance -> {
                        balance.setUsedLeaves(Math.max(0, balance.getUsedLeaves() - (int) days));
                        leaveBalanceRepository.save(balance);
                    });

            // Revert attendance records for the dates
            LocalDate current = leaveRequest.getStartDate();
            while (!current.isAfter(leaveRequest.getEndDate())) {
                attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(
                        leaveRequest.getEmployee().getEmployeeId(), current)
                        .ifPresent(att -> {
                            if (att.getStatus() == AttendanceStatus.ON_LEAVE) {
                                attendanceRepository.delete(att);
                            }
                        });
                current = current.plusDays(1);
            }
        }

        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return ResponseEntity.ok(mapToResponsePayload(updated));
    }

    /**
     * Allocate or update leave balance for an employee (HR / Admin).
     */
    @PostMapping("/balance/allocate")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<LeaveBalanceResponsePayload> allocateLeaveBalance(@Valid @RequestBody LeaveBalancePayload payload) {
        Employee employee = employeeRepository.findById(payload.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + payload.getEmployeeId()));

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_EmployeeIdAndLeaveType(employee.getEmployeeId(), payload.getLeaveType())
                .orElse(LeaveBalance.builder()
                        .employee(employee)
                        .leaveType(payload.getLeaveType())
                        .usedLeaves(0)
                        .build());

        balance.setTotalLeaves(payload.getTotalLeaves());
        LeaveBalance saved = leaveBalanceRepository.save(balance);

        return ResponseEntity.ok(mapToBalanceResponse(saved));
    }

    /**
     * Get all leave balances for an employee.
     */
    @GetMapping("/balance/employee/{employeeId}")
    public ResponseEntity<List<LeaveBalanceResponsePayload>> getEmployeeLeaveBalances(@PathVariable Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployee_EmployeeId(employeeId);
        List<LeaveBalanceResponsePayload> payloads = balances.stream()
                .map(this::mapToBalanceResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    /**
     * Get a leave request by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponsePayload> getLeaveById(@PathVariable Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
        return ResponseEntity.ok(mapToResponsePayload(leaveRequest));
    }

    /**
     * Get all leave requests for an employee (optional status filter).
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponsePayload>> getEmployeeLeaveRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LeaveRequestStatus status) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        List<LeaveRequest> requests;
        if (status != null) {
            requests = leaveRequestRepository.findByEmployee_EmployeeIdAndStatus(employeeId, status);
        } else {
            requests = leaveRequestRepository.findByEmployee_EmployeeId(employeeId);
        }

        List<LeaveResponsePayload> payloads = requests.stream()
                .map(this::mapToResponsePayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    /**
     * Get all pending leave requests requiring review (HR / Admin).
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<List<LeaveResponsePayload>> getPendingLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestRepository.findByStatus(LeaveRequestStatus.PENDING);
        List<LeaveResponsePayload> payloads = requests.stream()
                .map(this::mapToResponsePayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    private LeaveResponsePayload mapToResponsePayload(LeaveRequest request) {
        long days = 0;
        if (request.getStartDate() != null && request.getEndDate() != null) {
            days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        }

        String employeeName = "";
        Long employeeId = null;
        if (request.getEmployee() != null) {
            employeeId = request.getEmployee().getEmployeeId();
            employeeName = request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName();
        }

        Long approvedById = null;
        String approvedByUsername = null;
        if (request.getApprovedBy() != null) {
            approvedById = request.getApprovedBy().getUserId();
            approvedByUsername = request.getApprovedBy().getUsername();
        }

        return LeaveResponsePayload.builder()
                .leaveRequestId(request.getLeaveRequestId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(days)
                .reason(request.getReason())
                .status(request.getStatus())
                .appliedOn(request.getAppliedOn())
                .approvedByUserId(approvedById)
                .approvedByUsername(approvedByUsername)
                .build();
    }

    private LeaveBalanceResponsePayload mapToBalanceResponse(LeaveBalance balance) {
        String employeeName = "";
        Long employeeId = null;
        if (balance.getEmployee() != null) {
            employeeId = balance.getEmployee().getEmployeeId();
            employeeName = balance.getEmployee().getFirstName() + " " + balance.getEmployee().getLastName();
        }

        return LeaveBalanceResponsePayload.builder()
                .leaveBalanceId(balance.getLeaveBalanceId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .leaveType(balance.getLeaveType())
                .totalLeaves(balance.getTotalLeaves())
                .usedLeaves(balance.getUsedLeaves())
                .remainingLeaves(balance.getRemainingLeaves())
                .build();
    }
}
