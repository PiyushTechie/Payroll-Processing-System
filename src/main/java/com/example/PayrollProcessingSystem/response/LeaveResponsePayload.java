package com.example.PayrollProcessingSystem.response;

import java.time.LocalDate;

import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus;
import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveType;

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
public class LeaveResponsePayload {

    private Long leaveRequestId;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private long numberOfDays;
    private String reason;
    private LeaveRequestStatus status;
    private LocalDate appliedOn;
    private Long approvedByUserId;
    private String approvedByUsername;
}
