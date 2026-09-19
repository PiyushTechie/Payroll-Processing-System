package com.example.PayrollProcessingSystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import com.example.PayrollProcessingSystem.exception.ResourceNotFoundException;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.response.AttendanceRecordPayload;
import com.example.PayrollProcessingSystem.response.AttendanceSummaryPayload;
import com.example.PayrollProcessingSystem.response.CheckInRequest;
import com.example.PayrollProcessingSystem.response.CheckOutRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for tracking employee attendance and presence.
 * Implements business logic for check-in, check-out, working hours,
 * overtime computation, and monthly attendance aggregation.
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final double STANDARD_WORK_HOURS = 8.0;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Record employee check-in.
     */
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceRecordPayload> checkIn(@Valid @RequestBody CheckInRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate date = request.getAttendanceDate() != null ? request.getAttendanceDate() : LocalDate.now();
        LocalTime checkInTime = request.getCheckInTime() != null ? request.getCheckInTime() : LocalTime.now();

        Attendance attendance = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), date)
                .orElse(null);

        if (attendance != null && attendance.getCheckIn() != null) {
            throw new BadRequestException("Employee is already checked in for date: " + date);
        }

        if (attendance == null) {
            attendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(date)
                    .checkIn(checkInTime)
                    .status(AttendanceStatus.PRESENT)
                    .workingHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .overtimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .remarks(request.getRemarks())
                    .build();
        } else {
            attendance.setCheckIn(checkInTime);
            attendance.setStatus(AttendanceStatus.PRESENT);
            if (request.getRemarks() != null) {
                attendance.setRemarks(request.getRemarks());
            }
        }

        Attendance saved = attendanceRepository.save(attendance);
        return new ResponseEntity<>(mapToPayload(saved), HttpStatus.CREATED);
    }

    /**
     * Record employee check-out and calculate working/overtime hours.
     */
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceRecordPayload> checkOut(@Valid @RequestBody CheckOutRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate date = request.getAttendanceDate() != null ? request.getAttendanceDate() : LocalDate.now();
        LocalTime checkOutTime = request.getCheckOutTime() != null ? request.getCheckOutTime() : LocalTime.now();

        Attendance attendance = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), date)
                .orElseThrow(() -> new BadRequestException("No attendance check-in found for employee on date: " + date));

        if (attendance.getCheckIn() == null) {
            throw new BadRequestException("Cannot check out without prior check-in.");
        }

        if (checkOutTime.isBefore(attendance.getCheckIn())) {
            throw new BadRequestException("Check-out time cannot precede check-in time (" + attendance.getCheckIn() + ").");
        }

        attendance.setCheckOut(checkOutTime);
        calculateAndApplyHours(attendance);

        if (request.getRemarks() != null) {
            attendance.setRemarks(request.getRemarks());
        }

        Attendance updated = attendanceRepository.save(attendance);
        return ResponseEntity.ok(mapToPayload(updated));
    }

    /**
     * Create or update attendance record manually (HR / Admin).
     */
    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<AttendanceRecordPayload> recordAttendance(@Valid @RequestBody AttendanceRecordPayload payload) {
        Employee employee = employeeRepository.findById(payload.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + payload.getEmployeeId()));

        Attendance attendance = attendanceRepository
                .findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), payload.getAttendanceDate())
                .orElse(Attendance.builder()
                        .employee(employee)
                        .attendanceDate(payload.getAttendanceDate())
                        .build());

        attendance.setCheckIn(payload.getCheckIn());
        attendance.setCheckOut(payload.getCheckOut());

        if (payload.getStatus() != null) {
            attendance.setStatus(payload.getStatus());
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT);
        }

        if (payload.getWorkingHours() != null) {
            attendance.setWorkingHours(payload.getWorkingHours().setScale(2, RoundingMode.HALF_UP));
            attendance.setOvertimeHours(payload.getOvertimeHours() != null
                    ? payload.getOvertimeHours().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            calculateAndApplyHours(attendance);
        } else {
            attendance.setWorkingHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            attendance.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        if (payload.getRemarks() != null) {
            attendance.setRemarks(payload.getRemarks());
        }

        Attendance saved = attendanceRepository.save(attendance);
        return ResponseEntity.ok(mapToPayload(saved));
    }

    /**
     * Get attendance record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttendanceRecordPayload> getAttendanceById(@PathVariable Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));
        return ResponseEntity.ok(mapToPayload(attendance));
    }

    /**
     * Get employee attendance for a date range.
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceRecordPayload>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        if (start.isAfter(end)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        List<Attendance> records = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDateBetween(employeeId, start, end);
        List<AttendanceRecordPayload> payloads = records.stream()
                .map(this::mapToPayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    /**
     * Get monthly attendance summary for an employee (used by Module 4 Payroll Engine).
     */
    @GetMapping("/employee/{employeeId}/monthly")
    public ResponseEntity<AttendanceSummaryPayload> getMonthlyAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12.");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Attendance> records = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate);

        long presentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        long halfDayCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.HALF_DAY).count();
        long absentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        long onLeaveCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ON_LEAVE).count();
        long holidayCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.HOLIDAY).count();
        long weekendCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.WEEKEND).count();

        BigDecimal totalWorkingHours = records.stream()
                .map(r -> r.getWorkingHours() != null ? r.getWorkingHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalOvertimeHours = records.stream()
                .map(r -> r.getOvertimeHours() != null ? r.getOvertimeHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<AttendanceRecordPayload> recordPayloads = records.stream()
                .map(this::mapToPayload)
                .collect(Collectors.toList());

        AttendanceSummaryPayload summary = AttendanceSummaryPayload.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .year(year)
                .month(month)
                .presentDays(presentCount)
                .halfDays(halfDayCount)
                .absentDays(absentCount)
                .onLeaveDays(onLeaveCount)
                .holidayDays(holidayCount)
                .weekendDays(weekendCount)
                .totalWorkingHours(totalWorkingHours)
                .totalOvertimeHours(totalOvertimeHours)
                .records(recordPayloads)
                .build();

        return ResponseEntity.ok(summary);
    }

    /**
     * Get attendance register for a specific date (HR / Admin).
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<List<AttendanceRecordPayload>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Attendance> records = attendanceRepository.findByAttendanceDate(date);
        List<AttendanceRecordPayload> payloads = records.stream()
                .map(this::mapToPayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    /**
     * Business Logic: Calculate working hours and overtime hours.
     * Working hours capped at 8.00 standard hours; any excess counts as overtime.
     */
    private void calculateAndApplyHours(Attendance attendance) {
        if (attendance.getCheckIn() == null || attendance.getCheckOut() == null) {
            return;
        }

        long minutes = Duration.between(attendance.getCheckIn(), attendance.getCheckOut()).toMinutes();
        double totalHours = Math.max(0, minutes / 60.0);

        if (totalHours >= STANDARD_WORK_HOURS) {
            attendance.setWorkingHours(BigDecimal.valueOf(STANDARD_WORK_HOURS).setScale(2, RoundingMode.HALF_UP));
            attendance.setOvertimeHours(BigDecimal.valueOf(totalHours - STANDARD_WORK_HOURS).setScale(2, RoundingMode.HALF_UP));
            attendance.setStatus(AttendanceStatus.PRESENT);
        } else if (totalHours >= 4.0) {
            attendance.setWorkingHours(BigDecimal.valueOf(totalHours).setScale(2, RoundingMode.HALF_UP));
            attendance.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            attendance.setStatus(totalHours < 5.0 ? AttendanceStatus.HALF_DAY : AttendanceStatus.PRESENT);
        } else {
            attendance.setWorkingHours(BigDecimal.valueOf(totalHours).setScale(2, RoundingMode.HALF_UP));
            attendance.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }
    }

    private AttendanceRecordPayload mapToPayload(Attendance attendance) {
        return AttendanceRecordPayload.builder()
                .attendanceId(attendance.getAttendanceId())
                .employeeId(attendance.getEmployee() != null ? attendance.getEmployee().getEmployeeId() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .workingHours(attendance.getWorkingHours())
                .overtimeHours(attendance.getOvertimeHours())
                .remarks(attendance.getRemarks())
                .build();
    }
}
