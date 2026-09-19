package com.example.PayrollProcessingSystem.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.example.PayrollProcessingSystem.entity.Attendance.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordPayload {

    private Long attendanceId;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    private AttendanceStatus status;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private BigDecimal workingHours;

    private BigDecimal overtimeHours;

    private String remarks;
}
