package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.LeaveBalance;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.LeaveBalanceRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveBalanceController(
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository) {

        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // GET all leave balances
    @GetMapping
    public ResponseEntity<List<LeaveBalance>> getAllLeaveBalances() {

        return ResponseEntity.ok(
                leaveBalanceRepository.findAll()
        );
    }

    // GET leave balance by ID
    @GetMapping("/{id}")
    public ResponseEntity<LeaveBalance> getLeaveBalanceById(
            @PathVariable Long id) {

        return leaveBalanceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE leave balance
    @PostMapping
    public ResponseEntity<LeaveBalance> createLeaveBalance(
            @Valid @RequestBody LeaveBalance leaveBalance) {

        if (leaveBalance.getEmployee() == null ||
                leaveBalance.getEmployee().getEmployeeId() == null) {

            return ResponseEntity.badRequest().build();
        }

        Long employeeId =
                leaveBalance.getEmployee().getEmployeeId();

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElse(null);

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        // Make sure used leaves don't exceed total leaves
        if (leaveBalance.getUsedLeaves() != null &&
                leaveBalance.getTotalLeaves() != null &&
                leaveBalance.getUsedLeaves()
                        > leaveBalance.getTotalLeaves()) {

            return ResponseEntity.badRequest().build();
        }

        leaveBalance.setEmployee(employee);

        LeaveBalance savedLeaveBalance =
                leaveBalanceRepository.save(leaveBalance);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedLeaveBalance);
    }

    // UPDATE leave balance
    @PutMapping("/{id}")
    public ResponseEntity<LeaveBalance> updateLeaveBalance(
            @PathVariable Long id,
            @Valid @RequestBody LeaveBalance leaveBalanceDetails) {

        return leaveBalanceRepository.findById(id)
                .map(leaveBalance -> {

                    if (leaveBalanceDetails.getEmployee() != null &&
                            leaveBalanceDetails.getEmployee().getEmployeeId() != null) {

                        Long employeeId =
                                leaveBalanceDetails
                                        .getEmployee()
                                        .getEmployeeId();

                        Employee employee =
                                employeeRepository.findById(employeeId)
                                        .orElse(null);

                        if (employee != null) {
                            leaveBalance.setEmployee(employee);
                        }
                    }

                    if (leaveBalanceDetails.getUsedLeaves() != null &&
                            leaveBalanceDetails.getTotalLeaves() != null &&
                            leaveBalanceDetails.getUsedLeaves()
                                    > leaveBalanceDetails.getTotalLeaves()) {

                        return null;
                    }

                    leaveBalance.setLeaveType(
                            leaveBalanceDetails.getLeaveType());

                    leaveBalance.setTotalLeaves(
                            leaveBalanceDetails.getTotalLeaves());

                    leaveBalance.setUsedLeaves(
                            leaveBalanceDetails.getUsedLeaves());

                    return ResponseEntity.ok(
                            leaveBalanceRepository.save(leaveBalance)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE leave balance
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveBalance(
            @PathVariable Long id) {

        if (!leaveBalanceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        leaveBalanceRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}