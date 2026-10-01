package com.example.PayrollProcessingSystem.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.PayrollProcessingSystem.entity.Attendance;
import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;
import com.example.PayrollProcessingSystem.entity.Department;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.Employee.EmployeeStatus;
import com.example.PayrollProcessingSystem.entity.Employee.EmploymentType;
import com.example.PayrollProcessingSystem.entity.LeaveBalance;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveType;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.entity.User.Role;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import com.example.PayrollProcessingSystem.repository.AttendanceRepository;
import com.example.PayrollProcessingSystem.repository.DepartmentRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.LeaveBalanceRepository;
import com.example.PayrollProcessingSystem.repository.LeaveRequestRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import com.example.PayrollProcessingSystem.response.LeaveApplicationPayload;
import com.example.PayrollProcessingSystem.response.LeaveApprovalPayload;
import com.example.PayrollProcessingSystem.response.LeaveBalancePayload;
import com.example.PayrollProcessingSystem.response.LeaveBalanceResponsePayload;
import com.example.PayrollProcessingSystem.response.LeaveResponsePayload;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "hr_alice", roles = {"HR", "ADMIN"})
class LeaveControllerTest {

    @Autowired
    private LeaveController leaveController;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Employee testEmployee;
    private User hrUser;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(Department.builder()
                .departmentName("Human Resources")
                .description("HR Dept")
                .build());

        testEmployee = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-201")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .phone("9876543210")
                .designation("HR Executive")
                .department(dept)
                .employmentType(EmploymentType.FULL_TIME)
                .dateOfJoining(LocalDate.of(2025, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build());

        hrUser = userRepository.save(User.builder()
                .employee(testEmployee)
                .username("hr_alice")
                .passwordHash("hashed_password")
                .role(Role.HR)
                .build());
    }

    @Test
    void testAllocateLeaveBalance() {
        LeaveBalancePayload payload = LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(12)
                .build();

        ResponseEntity<LeaveBalanceResponsePayload> response = leaveController.allocateLeaveBalance(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(12, response.getBody().getTotalLeaves());
        assertEquals(0, response.getBody().getUsedLeaves());
        assertEquals(12, response.getBody().getRemainingLeaves());
    }

    @Test
    void testApplyLeave_SufficientBalance_Success() {
        // Allocate 10 days of CASUAL leave
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(10)
                .build());

        // Apply for 3 days
        LeaveApplicationPayload applyPayload = LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
                .reason("Personal work")
                .build();

        ResponseEntity<LeaveResponsePayload> response = leaveController.applyLeave(applyPayload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LeaveRequestStatus.PENDING, response.getBody().getStatus());
        assertEquals(3, response.getBody().getNumberOfDays());
    }

    @Test
    void testApplyLeave_InsufficientBalance_ThrowsBadRequest() {
        // Allocate only 2 days of SICK leave
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.SICK)
                .totalLeaves(2)
                .build());

        // Attempt to apply for 5 days
        LeaveApplicationPayload applyPayload = LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 5))
                .reason("Medical treatment")
                .build();

        assertThrows(BadRequestException.class, () -> leaveController.applyLeave(applyPayload));
    }

    @Test
    void testApplyLeave_NoBalanceConfigured_ThrowsBadRequest() {
        LeaveApplicationPayload applyPayload = LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 2))
                .reason("Vacation")
                .build();

        assertThrows(BadRequestException.class, () -> leaveController.applyLeave(applyPayload));
    }

    @Test
    void testApplyLeave_OverlappingDates_ThrowsBadRequest() {
        // Allocate leaves
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(15)
                .build());

        // First application: Oct 5 to Oct 10
        leaveController.applyLeave(LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.of(2026, 10, 5))
                .endDate(LocalDate.of(2026, 10, 10))
                .reason("Trip")
                .build());

        // Second application overlapping: Oct 8 to Oct 12
        LeaveApplicationPayload overlappingPayload = LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.of(2026, 10, 8))
                .endDate(LocalDate.of(2026, 10, 12))
                .reason("Another trip")
                .build();

        assertThrows(BadRequestException.class, () -> leaveController.applyLeave(overlappingPayload));
    }

    @Test
    void testApplyLeave_Unpaid_AllowedWithoutBalance() {
        LeaveApplicationPayload unpaidPayload = LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.UNPAID)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 4))
                .reason("Unpaid leave")
                .build();

        ResponseEntity<LeaveResponsePayload> response = leaveController.applyLeave(unpaidPayload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LeaveType.UNPAID, response.getBody().getLeaveType());
        assertEquals(4, response.getBody().getNumberOfDays());
    }

    @Test
    void testReviewLeave_Approve_DeductsBalanceAndSyncsAttendance() {
        // Allocate 10 days of CASUAL leave
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(10)
                .build());

        // Apply for 3 days: Nov 10 to Nov 12
        ResponseEntity<LeaveResponsePayload> applyResponse = leaveController.applyLeave(LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 12))
                .reason("Family event")
                .build());

        Long requestId = applyResponse.getBody().getLeaveRequestId();

        // Approve leave
        CustomUserDetails userDetails = new CustomUserDetails(hrUser);
        LeaveApprovalPayload approvalPayload = LeaveApprovalPayload.builder()
                .leaveRequestId(requestId)
                .newStatus(LeaveRequestStatus.APPROVED)
                .remarks("Approved by HR")
                .build();

        ResponseEntity<LeaveResponsePayload> reviewResponse = leaveController.reviewLeave(approvalPayload, userDetails);

        assertEquals(HttpStatus.OK, reviewResponse.getStatusCode());
        assertEquals(LeaveRequestStatus.APPROVED, reviewResponse.getBody().getStatus());

        // Verify balance deducted: used = 3, remaining = 7
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_EmployeeIdAndLeaveType(testEmployee.getEmployeeId(), LeaveType.CASUAL)
                .orElseThrow();
        assertEquals(3, balance.getUsedLeaves());
        assertEquals(7, balance.getRemainingLeaves());

        // Verify Attendance records created with status ON_LEAVE
        List<Attendance> attendances = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDateBetween(
                testEmployee.getEmployeeId(), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));
        assertEquals(3, attendances.size());
        for (Attendance att : attendances) {
            assertEquals(AttendanceStatus.ON_LEAVE, att.getStatus());
        }
    }

    @Test
    void testReviewLeave_Reject_DoesNotDeductBalance() {
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.SICK)
                .totalLeaves(10)
                .build());

        ResponseEntity<LeaveResponsePayload> applyResponse = leaveController.applyLeave(LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.of(2026, 11, 20))
                .endDate(LocalDate.of(2026, 11, 21))
                .reason("Fever")
                .build());

        Long requestId = applyResponse.getBody().getLeaveRequestId();

        CustomUserDetails userDetails = new CustomUserDetails(hrUser);
        LeaveApprovalPayload rejectPayload = LeaveApprovalPayload.builder()
                .leaveRequestId(requestId)
                .newStatus(LeaveRequestStatus.REJECTED)
                .remarks("Invalid certificate")
                .build();

        ResponseEntity<LeaveResponsePayload> reviewResponse = leaveController.reviewLeave(rejectPayload, userDetails);

        assertEquals(HttpStatus.OK, reviewResponse.getStatusCode());
        assertEquals(LeaveRequestStatus.REJECTED, reviewResponse.getBody().getStatus());

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_EmployeeIdAndLeaveType(testEmployee.getEmployeeId(), LeaveType.SICK)
                .orElseThrow();
        assertEquals(0, balance.getUsedLeaves());
        assertEquals(10, balance.getRemainingLeaves());
    }

    @Test
    void testCancelLeave_RestoresBalance() {
        // Allocate balance
        leaveController.allocateLeaveBalance(LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.ANNUAL)
                .totalLeaves(15)
                .build());

        // Apply and approve 4 days: Dec 1 to Dec 4
        ResponseEntity<LeaveResponsePayload> applyResponse = leaveController.applyLeave(LeaveApplicationPayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 4))
                .reason("Vacation")
                .build());

        Long requestId = applyResponse.getBody().getLeaveRequestId();
        leaveController.reviewLeave(LeaveApprovalPayload.builder()
                .leaveRequestId(requestId)
                .newStatus(LeaveRequestStatus.APPROVED)
                .build(), new CustomUserDetails(hrUser));

        // Verify balance deducted
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployee_EmployeeIdAndLeaveType(testEmployee.getEmployeeId(), LeaveType.ANNUAL)
                .orElseThrow();
        assertEquals(4, balance.getUsedLeaves());

        // Cancel the approved leave
        ResponseEntity<LeaveResponsePayload> cancelResponse = leaveController.cancelLeave(requestId);

        assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
        assertEquals(LeaveRequestStatus.CANCELLED, cancelResponse.getBody().getStatus());

        // Verify balance restored to 0 used
        balance = leaveBalanceRepository
                .findByEmployee_EmployeeIdAndLeaveType(testEmployee.getEmployeeId(), LeaveType.ANNUAL)
                .orElseThrow();
        assertEquals(0, balance.getUsedLeaves());
        assertEquals(15, balance.getRemainingLeaves());
    }

    @Test
    @WithMockUser(username = "emp_bob", roles = {"EMPLOYEE"})
    void testAllocateLeaveBalance_EmployeeRole_AccessDenied() {
        LeaveBalancePayload payload = LeaveBalancePayload.builder()
                .employeeId(testEmployee.getEmployeeId())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(12)
                .build();

        assertThrows(AccessDeniedException.class, () -> leaveController.allocateLeaveBalance(payload));
    }
}
