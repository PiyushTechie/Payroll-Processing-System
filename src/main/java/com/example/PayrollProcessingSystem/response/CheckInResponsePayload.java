package com.example.PayrollProcessingSystem.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dedicated clean response for check-in requests.
 * Excludes checkout, working hours, and overtime hours as they only apply post check-out.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponsePayload {

    private Long attendanceId;
    private Long employeeId;
    private String employeeName;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private LocalTime checkIn;
    private String remarks;
    private String message;
}
