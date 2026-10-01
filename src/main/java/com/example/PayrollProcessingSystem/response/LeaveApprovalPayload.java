package com.example.PayrollProcessingSystem.response;

import com.example.PayrollProcessingSystem.entity.LeaveRequest.LeaveRequestStatus;

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
public class LeaveApprovalPayload {

    @NotNull(message = "Leave request ID is required")
    private Long leaveRequestId;

    @NotNull(message = "New status is required (APPROVED or REJECTED)")
    private LeaveRequestStatus newStatus;

    private String remarks;
}
