package com.example.PayrollProcessingSystem.response;

import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveType;

import jakarta.validation.constraints.Min;
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
public class LeaveBalancePayload {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Total leaves is required")
    @Min(value = 0, message = "Total leaves cannot be negative")
    private Integer totalLeaves;
}
