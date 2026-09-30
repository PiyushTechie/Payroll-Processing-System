package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.LeaveRequest;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.LeaveRequestRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveRequestController(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    // GET all leave requests
    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {

        return ResponseEntity.ok(
                leaveRequestRepository.findAll()
        );
    }

    // GET leave request by ID
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequestById(
            @PathVariable Long id) {

        return leaveRequestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE leave request
    @PostMapping
    public ResponseEntity<LeaveRequest> createLeaveRequest(
            @Valid @RequestBody LeaveRequest leaveRequest) {

        // Employee is required
        if (leaveRequest.getEmployee() == null ||
                leaveRequest.getEmployee().getEmployeeId() == null) {

            return ResponseEntity.badRequest().build();
        }

        // Validate dates
        if (leaveRequest.getStartDate() == null ||
                leaveRequest.getEndDate() == null ||
                leaveRequest.getEndDate()
                        .isBefore(leaveRequest.getStartDate())) {

            return ResponseEntity.badRequest().build();
        }

        // Find employee
        Long employeeId =
                leaveRequest.getEmployee().getEmployeeId();

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElse(null);

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        leaveRequest.setEmployee(employee);

        // New requests should start as PENDING
        leaveRequest.setStatus(
                LeaveRequest.LeaveRequestStatus.PENDING
        );

        LeaveRequest savedLeaveRequest =
                leaveRequestRepository.save(leaveRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedLeaveRequest);
    }

    // UPDATE leave request
    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequest> updateLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequest requestDetails) {

        return leaveRequestRepository.findById(id)
                .map(leaveRequest -> {

                    // Update employee
                    if (requestDetails.getEmployee() != null &&
                            requestDetails.getEmployee().getEmployeeId() != null) {

                        Long employeeId =
                                requestDetails
                                        .getEmployee()
                                        .getEmployeeId();

                        Employee employee =
                                employeeRepository.findById(employeeId)
                                        .orElse(null);

                        if (employee != null) {
                            leaveRequest.setEmployee(employee);
                        }
                    }

                    // Validate dates
                    if (requestDetails.getStartDate() != null &&
                            requestDetails.getEndDate() != null &&
                            requestDetails.getEndDate()
                                    .isBefore(requestDetails.getStartDate())) {

                        return ResponseEntity.badRequest()
                                .<LeaveRequest>build();
                    }

                    leaveRequest.setLeaveType(
                            requestDetails.getLeaveType());

                    leaveRequest.setStartDate(
                            requestDetails.getStartDate());

                    leaveRequest.setEndDate(
                            requestDetails.getEndDate());

                    leaveRequest.setReason(
                            requestDetails.getReason());

                    leaveRequest.setStatus(
                            requestDetails.getStatus());

                    // Update approvedBy if provided
                    if (requestDetails.getApprovedBy() != null &&
                            requestDetails.getApprovedBy().getUserId() != null) {

                        Long userId =
                                requestDetails
                                        .getApprovedBy()
                                        .getUserId();

                        User user =
                                userRepository.findById(userId)
                                        .orElse(null);

                        if (user != null) {
                            leaveRequest.setApprovedBy(user);
                        }
                    }

                    LeaveRequest updatedLeaveRequest =
                            leaveRequestRepository.save(leaveRequest);

                    return ResponseEntity.ok(updatedLeaveRequest);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE leave request
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveRequest(
            @PathVariable Long id) {

        if (!leaveRequestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        leaveRequestRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}