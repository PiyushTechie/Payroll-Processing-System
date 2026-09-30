package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // GET all attendance records
    @GetMapping
    public ResponseEntity<List<Attendance>> getAllAttendance() {

        return ResponseEntity.ok(attendanceRepository.findAll());
    }

    // GET attendance by ID
    @GetMapping("/{id}")
    public ResponseEntity<Attendance> getAttendanceById(
            @PathVariable Long id) {

        return attendanceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE attendance
    @PostMapping
    public ResponseEntity<Attendance> createAttendance(
            @Valid @RequestBody Attendance attendance) {

        // Make sure employee exists
        if (attendance.getEmployee() == null ||
                attendance.getEmployee().getEmployeeId() == null) {

            return ResponseEntity.badRequest().build();
        }

        Long employeeId = attendance.getEmployee().getEmployeeId();

        Employee employee = employeeRepository.findById(employeeId)
                .orElse(null);

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        attendance.setEmployee(employee);

        Attendance savedAttendance =
                attendanceRepository.save(attendance);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedAttendance);
    }

    // UPDATE attendance
    @PutMapping("/{id}")
    public ResponseEntity<Attendance> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody Attendance attendanceDetails) {

        return attendanceRepository.findById(id)
                .map(attendance -> {

                    // Update employee if provided
                    if (attendanceDetails.getEmployee() != null &&
                            attendanceDetails.getEmployee().getEmployeeId() != null) {

                        Long employeeId =
                                attendanceDetails.getEmployee().getEmployeeId();

                        Employee employee =
                                employeeRepository.findById(employeeId)
                                        .orElse(null);

                        if (employee != null) {
                            attendance.setEmployee(employee);
                        }
                    }

                    attendance.setAttendanceDate(
                            attendanceDetails.getAttendanceDate());

                    attendance.setStatus(
                            attendanceDetails.getStatus());

                    attendance.setCheckIn(
                            attendanceDetails.getCheckIn());

                    attendance.setCheckOut(
                            attendanceDetails.getCheckOut());

                    attendance.setWorkingHours(
                            attendanceDetails.getWorkingHours());

                    attendance.setOvertimeHours(
                            attendanceDetails.getOvertimeHours());

                    attendance.setRemarks(
                            attendanceDetails.getRemarks());

                    Attendance updatedAttendance =
                            attendanceRepository.save(attendance);

                    return ResponseEntity.ok(updatedAttendance);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE attendance
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable Long id) {

        if (!attendanceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        attendanceRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}