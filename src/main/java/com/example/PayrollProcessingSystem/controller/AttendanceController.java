package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.response.CheckInRequest;
import com.example.PayrollProcessingSystem.response.CheckOutRequest;
import com.example.PayrollProcessingSystem.response.CheckInResponsePayload;
import com.example.PayrollProcessingSystem.response.AttendanceRecordPayload;
import com.example.PayrollProcessingSystem.exception.ResourceNotFoundException;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import java.time.LocalDate;

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

    // POST /api/attendance/check-in
    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponsePayload> checkIn(@Valid @RequestBody CheckInRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate today = LocalDate.now();
        LocalDate attendanceDate = request.getAttendanceDate() != null ? request.getAttendanceDate() : today;

        if (attendanceDate.isBefore(today)) {
            throw new BadRequestException("Check-in is not allowed for past dates.");
        }
        if (attendanceDate.isAfter(today)) {
            throw new BadRequestException("Check-in is not allowed for future dates.");
        }

        attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), attendanceDate)
                .ifPresent(a -> { throw new BadRequestException("Employee already checked in for date: " + attendanceDate); });

        java.time.LocalTime checkInTime = request.getCheckInTime() != null 
                ? request.getCheckInTime() 
                : java.time.LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setCheckIn(checkInTime);
        attendance.setStatus(com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.PRESENT);
        attendance.setRemarks(request.getRemarks());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        CheckInResponsePayload responsePayload = CheckInResponsePayload.builder()
                .attendanceId(savedAttendance.getAttendanceId())
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .attendanceDate(savedAttendance.getAttendanceDate())
                .status(savedAttendance.getStatus())
                .checkIn(savedAttendance.getCheckIn())
                .remarks(savedAttendance.getRemarks())
                .message("Check-in recorded successfully")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responsePayload);
    }

    // POST /api/attendance/check-out
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceRecordPayload> checkOut(@Valid @RequestBody CheckOutRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate today = LocalDate.now();
        LocalDate attendanceDate = request.getAttendanceDate() != null ? request.getAttendanceDate() : today;

        if (attendanceDate.isBefore(today)) {
            throw new BadRequestException("Check-out is not allowed for past dates.");
        }
        if (attendanceDate.isAfter(today)) {
            throw new BadRequestException("Check-out is not allowed for future dates.");
        }

        Attendance attendance = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), attendanceDate)
                .orElseThrow(() -> new BadRequestException("No check-in record found for date: " + attendanceDate));

        // Prevent multiple checkouts for the same day
        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("Employee has already checked out for date: " + attendanceDate);
        }

        java.time.LocalTime checkOutTime = request.getCheckOutTime() != null 
                ? request.getCheckOutTime() 
                : java.time.LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        if (attendance.getCheckIn() != null && checkOutTime.isBefore(attendance.getCheckIn())) {
            throw new BadRequestException("Check-out time cannot be before check-in time");
        }

        attendance.setCheckOut(checkOutTime);

        // Calculate hours
        if (attendance.getCheckIn() != null) {
            long minutesWorked = java.time.Duration.between(attendance.getCheckIn(), checkOutTime).toMinutes();
            double hoursWorked = minutesWorked / 60.0;
            
            if (hoursWorked < 5.0) {
                attendance.setStatus(com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.HALF_DAY);
                attendance.setWorkingHours(java.math.BigDecimal.valueOf(hoursWorked).setScale(2, java.math.RoundingMode.HALF_UP));
                attendance.setOvertimeHours(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
            } else {
                attendance.setStatus(com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.PRESENT);
                double regularHours = Math.min(hoursWorked, 8.0);
                double overtimeHours = Math.max(0, hoursWorked - 8.0);
                attendance.setWorkingHours(java.math.BigDecimal.valueOf(regularHours).setScale(2, java.math.RoundingMode.HALF_UP));
                attendance.setOvertimeHours(java.math.BigDecimal.valueOf(overtimeHours).setScale(2, java.math.RoundingMode.HALF_UP));
            }
        }

        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return ResponseEntity.ok(mapToRecordPayload(updatedAttendance));
    }

    // POST /api/attendance/record (HR manual entry)
    @PostMapping("/record")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<AttendanceRecordPayload> recordAttendance(@Valid @RequestBody AttendanceRecordPayload payload) {
        Employee employee = employeeRepository.findById(payload.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Attendance attendance = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(employee.getEmployeeId(), payload.getAttendanceDate())
                .orElse(new Attendance());

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(payload.getAttendanceDate());
        attendance.setStatus(payload.getStatus());
        attendance.setCheckIn(payload.getCheckIn());
        attendance.setCheckOut(payload.getCheckOut());
        attendance.setRemarks(payload.getRemarks());

        if (payload.getWorkingHours() != null) {
            attendance.setWorkingHours(payload.getWorkingHours());
        } else {
            // Default calculation if times are provided
            if (payload.getCheckIn() != null && payload.getCheckOut() != null) {
                long minutes = java.time.Duration.between(payload.getCheckIn(), payload.getCheckOut()).toMinutes();
                double hours = minutes / 60.0;
                double reg = Math.min(hours, 8.0);
                double over = Math.max(0, hours - 8.0);
                attendance.setWorkingHours(java.math.BigDecimal.valueOf(reg).setScale(2, java.math.RoundingMode.HALF_UP));
                attendance.setOvertimeHours(java.math.BigDecimal.valueOf(over).setScale(2, java.math.RoundingMode.HALF_UP));
            } else {
                attendance.setWorkingHours(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
                attendance.setOvertimeHours(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
            }
        }

        if (payload.getOvertimeHours() != null) {
            attendance.setOvertimeHours(payload.getOvertimeHours());
        }

        Attendance saved = attendanceRepository.save(attendance);
        return ResponseEntity.ok(mapToRecordPayload(saved));
    }

    // GET /api/attendance/summary/{employeeId}
    @GetMapping("/summary/{employeeId}")
    public ResponseEntity<com.example.PayrollProcessingSystem.response.AttendanceSummaryPayload> getMonthlyAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
            
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Attendance> records = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDateBetween(employee.getEmployeeId(), startDate, endDate);

        int presentDays = 0;
        int halfDays = 0;
        int onLeaveDays = 0;
        int absentDays = 0;
        int holidayDays = 0;
        int weekendDays = 0;
        java.math.BigDecimal totalWork = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalOver = java.math.BigDecimal.ZERO;

        for (Attendance a : records) {
            if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.PRESENT) presentDays++;
            else if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.HALF_DAY) halfDays++;
            else if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.ON_LEAVE) onLeaveDays++;
            else if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.ABSENT) absentDays++;
            else if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.HOLIDAY) holidayDays++;
            else if (a.getStatus() == com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus.WEEKEND) weekendDays++;

            if (a.getWorkingHours() != null) totalWork = totalWork.add(a.getWorkingHours());
            if (a.getOvertimeHours() != null) totalOver = totalOver.add(a.getOvertimeHours());
        }

        List<com.example.PayrollProcessingSystem.response.AttendanceRecordPayload> recordPayloads = records.stream()
                .map(this::mapToRecordPayload)
                .collect(java.util.stream.Collectors.toList());

        com.example.PayrollProcessingSystem.response.AttendanceSummaryPayload summary = com.example.PayrollProcessingSystem.response.AttendanceSummaryPayload.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .month(month)
                .year(year)
                .presentDays(presentDays)
                .halfDays(halfDays)
                .absentDays(absentDays)
                .onLeaveDays(onLeaveDays)
                .holidayDays(holidayDays)
                .weekendDays(weekendDays)
                .totalWorkingHours(totalWork.setScale(2, java.math.RoundingMode.HALF_UP))
                .totalOvertimeHours(totalOver.setScale(2, java.math.RoundingMode.HALF_UP))
                .records(recordPayloads)
                .build();

        return ResponseEntity.ok(summary);
    }

    // Mapping method
    private AttendanceRecordPayload mapToRecordPayload(Attendance attendance) {
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
