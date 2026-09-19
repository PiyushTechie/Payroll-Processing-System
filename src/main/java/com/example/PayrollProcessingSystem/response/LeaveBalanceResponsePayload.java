package com.example.PayrollProcessingSystem.response;

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
public class LeaveBalanceResponsePayload {

    private Long leaveBalanceId;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private Integer totalLeaves;
    private Integer usedLeaves;
    private Integer remainingLeaves;
}
