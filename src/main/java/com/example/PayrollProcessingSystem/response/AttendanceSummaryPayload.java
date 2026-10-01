package com.example.PayrollProcessingSystem.response;

import java.math.BigDecimal;
import java.util.List;

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
public class AttendanceSummaryPayload {

    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Integer month;

    private long presentDays;
    private long halfDays;
    private long absentDays;
    private long onLeaveDays;
    private long holidayDays;
    private long weekendDays;

    private BigDecimal totalWorkingHours;
    private BigDecimal totalOvertimeHours;

    private List<AttendanceRecordPayload> records;
}
