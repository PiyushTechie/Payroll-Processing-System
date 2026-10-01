package com.example.PayrollProcessingSystem.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;
import com.example.PayrollProcessingSystem.entity.Department;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.Employee.EmployeeStatus;
import com.example.PayrollProcessingSystem.entity.Employee.EmploymentType;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import com.example.PayrollProcessingSystem.exception.ResourceNotFoundException;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.DepartmentRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.response.AttendanceRecordPayload;
import com.example.PayrollProcessingSystem.response.AttendanceSummaryPayload;
import com.example.PayrollProcessingSystem.response.CheckInRequest;
import com.example.PayrollProcessingSystem.response.CheckOutRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttendanceControllerTest {

    @Autowired
    private AttendanceController attendanceController;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(Department.builder()
                .departmentName("Engineering")
                .description("Software Development")
                .build());

        testEmployee = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-101")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .designation("Software Engineer")
                .department(dept)
                .employmentType(EmploymentType.FULL_TIME)
                .dateOfJoining(LocalDate.of(2025, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build());
    }

    @Test
    void testCheckIn_Success() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .checkInTime(LocalTime.of(9, 0))
                .remarks("On time")
                .build();

        ResponseEntity<AttendanceRecordPayload> response = attendanceController.checkIn(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(AttendanceStatus.PRESENT, response.getBody().getStatus());
        assertEquals(LocalTime.of(9, 0), response.getBody().getCheckIn());
    }

    @Test
    void testCheckIn_Duplicate_ThrowsBadRequest() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .checkInTime(LocalTime.of(9, 0))
                .build();

        attendanceController.checkIn(request);

        // Second check-in on the same day should fail
        assertThrows(BadRequestException.class, () -> attendanceController.checkIn(request));
    }

    @Test
    void testCheckIn_EmployeeNotFound_ThrowsResourceNotFound() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(99999L)
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .checkInTime(LocalTime.of(9, 0))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> attendanceController.checkIn(request));
    }

    @Test
    void testCheckOut_StandardAndOvertimeCalculation() {
        // Check in at 09:00
        attendanceController.checkIn(CheckInRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .checkInTime(LocalTime.of(9, 0))
                .build());

        // Check out at 19:00 (10 hours worked -> 8.00 standard working hours, 2.00 overtime hours)
        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .checkOutTime(LocalTime.of(19, 0))
                .build();

        ResponseEntity<AttendanceRecordPayload> response = attendanceController.checkOut(checkOutRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("8.00"), response.getBody().getWorkingHours());
        assertEquals(new BigDecimal("2.00"), response.getBody().getOvertimeHours());
        assertEquals(AttendanceStatus.PRESENT, response.getBody().getStatus());
    }

    @Test
    void testCheckOut_HalfDayCalculation() {
        // Check in at 09:00
        attendanceController.checkIn(CheckInRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 2))
                .checkInTime(LocalTime.of(9, 0))
                .build());

        // Check out at 13:30 (4.5 hours -> HALF_DAY, 4.50 working hours, 0.00 overtime)
        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 2))
                .checkOutTime(LocalTime.of(13, 30))
                .build();

        ResponseEntity<AttendanceRecordPayload> response = attendanceController.checkOut(checkOutRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("4.50"), response.getBody().getWorkingHours());
        assertEquals(new BigDecimal("0.00"), response.getBody().getOvertimeHours());
        assertEquals(AttendanceStatus.HALF_DAY, response.getBody().getStatus());
    }

    @Test
    void testCheckOut_WithoutCheckIn_ThrowsBadRequest() {
        CheckOutRequest request = CheckOutRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 10))
                .checkOutTime(LocalTime.of(18, 0))
                .build();

        assertThrows(BadRequestException.class, () -> attendanceController.checkOut(request));
    }

    @Test
    void testCheckOut_BeforeCheckIn_ThrowsBadRequest() {
        attendanceController.checkIn(CheckInRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 3))
                .checkInTime(LocalTime.of(14, 0))
                .build());

        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 3))
                .checkOutTime(LocalTime.of(10, 0))
                .build();

        assertThrows(BadRequestException.class, () -> attendanceController.checkOut(checkOutRequest));
    }

    @Test
    @WithMockUser(username = "hr_user", roles = {"HR"})
    void testManualRecordAttendance_HR() {
        AttendanceRecordPayload payload = AttendanceRecordPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 5))
                .status(AttendanceStatus.PRESENT)
                .checkIn(LocalTime.of(9, 0))
                .checkOut(LocalTime.of(17, 0))
                .remarks("HR manual entry")
                .build();

        ResponseEntity<AttendanceRecordPayload> response = attendanceController.recordAttendance(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("8.00"), response.getBody().getWorkingHours());
        assertEquals(new BigDecimal("0.00"), response.getBody().getOvertimeHours());
    }

    @Test
    @WithMockUser(username = "emp_user", roles = {"EMPLOYEE"})
    void testManualRecordAttendance_Unauthorized() {
        AttendanceRecordPayload payload = AttendanceRecordPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .attendanceDate(LocalDate.of(2026, 9, 5))
                .status(AttendanceStatus.PRESENT)
                .checkIn(LocalTime.of(9, 0))
                .checkOut(LocalTime.of(17, 0))
                .build();

        assertThrows(AccessDeniedException.class, () -> attendanceController.recordAttendance(payload));
    }

    @Test
    void testGetMonthlyAttendanceSummary() {
        // Day 1: 8 hours present
        attendanceRepository.save(Attendance.builder()
                .employee(testEmployee)
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .status(AttendanceStatus.PRESENT)
                .workingHours(new BigDecimal("8.00"))
                .overtimeHours(new BigDecimal("1.50"))
                .build());

        // Day 2: half day
        attendanceRepository.save(Attendance.builder()
                .employee(testEmployee)
                .attendanceDate(LocalDate.of(2026, 9, 2))
                .status(AttendanceStatus.HALF_DAY)
                .workingHours(new BigDecimal("4.00"))
                .overtimeHours(BigDecimal.ZERO)
                .build());

        // Day 3: on leave
        attendanceRepository.save(Attendance.builder()
                .employee(testEmployee)
                .attendanceDate(LocalDate.of(2026, 9, 3))
                .status(AttendanceStatus.ON_LEAVE)
                .workingHours(BigDecimal.ZERO)
                .overtimeHours(BigDecimal.ZERO)
                .build());

        ResponseEntity<AttendanceSummaryPayload> response =
                attendanceController.getMonthlyAttendanceSummary(testEmployee.getEmployeeId(), 2026, 9);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AttendanceSummaryPayload summary = response.getBody();
        assertNotNull(summary);
        assertEquals(1, summary.getPresentDays());
        assertEquals(1, summary.getHalfDays());
        assertEquals(1, summary.getOnLeaveDays());
        assertEquals(new BigDecimal("12.00"), summary.getTotalWorkingHours());
        assertEquals(new BigDecimal("1.50"), summary.getTotalOvertimeHours());
    }
}
