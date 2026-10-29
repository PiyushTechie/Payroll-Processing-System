package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.AuditLog;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.repository.AuditLogRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private void recordAuditLog(String action, Long employeeId, String oldState, String newState) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;
            if (auth != null && auth.getName() != null) {
                currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
            }
            AuditLog auditLog = AuditLog.builder()
                    .entityName("Employee")
                    .entityId(employeeId)
                    .oldState(oldState)
                    .newState(newState)
                    .changedBy(currentUser)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception ignored) {
            // Prevent audit log errors from blocking core business flow
        }
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Employee> getEmployeeByEmail(@RequestParam String email) {
        return employeeRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        Employee saved = employeeRepository.save(employee);
        recordAuditLog("CREATE", saved.getEmployeeId(), null,
                String.format("{\"employeeCode\":\"%s\",\"email\":\"%s\",\"designation\":\"%s\",\"status\":\"%s\"}",
                        saved.getEmployeeCode(), saved.getEmail(), saved.getDesignation(), saved.getStatus()));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee updated) {
        return employeeRepository.findById(id).map(emp -> {
            String oldState = String.format("{\"designation\":\"%s\",\"phone\":\"%s\",\"status\":\"%s\"}",
                    emp.getDesignation(), emp.getPhone(), emp.getStatus());

            emp.setFirstName(updated.getFirstName());
            emp.setLastName(updated.getLastName());
            emp.setEmail(updated.getEmail());
            emp.setPhone(updated.getPhone());
            emp.setDesignation(updated.getDesignation());
            emp.setEmploymentType(updated.getEmploymentType());
            emp.setStatus(updated.getStatus());
            Employee saved = employeeRepository.save(emp);

            String newState = String.format("{\"designation\":\"%s\",\"phone\":\"%s\",\"status\":\"%s\"}",
                    saved.getDesignation(), saved.getPhone(), saved.getStatus());

            recordAuditLog("UPDATE", saved.getEmployeeId(), oldState, newState);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        recordAuditLog("DELETE", id, String.format("{\"deletedEmployeeId\":%d}", id), null);
        employeeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}