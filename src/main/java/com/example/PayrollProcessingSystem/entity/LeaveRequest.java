package com.example.PayrollProcessingSystem.entity;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.example.PayrollProcessingSystem.enums.LeaveType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.PayrollProcessingSystem.enums.LeaveRequestStatus;

/**
 * Represents an employee's leave request for time off.
 * Manages leave applications with statuses, dates, and approver tracking.
 */

@Entity
@Table(name = "leave_request", indexes = {
                @Index(name = "idx_leave_request_employee", columnList = "employee_id"),
                @Index(name = "idx_leave_request_status", columnList = "status"),
                @Index(name = "idx_leave_request_approver", columnList = "approved_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long leaveRequestId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "employee_id", nullable = false)
        @JsonBackReference("employee-leave-requests")
        private Employee employee;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "leave_type", nullable = false, length = 30)
        private LeaveType leaveType;

        @NotNull
        @Column(name = "start_date", nullable = false)
        private LocalDate startDate;

        @NotNull
        @Column(name = "end_date", nullable = false)
        private LocalDate endDate;

        @NotBlank
        @Column(nullable = false, length = 500)
        private String reason;

        @Builder.Default
        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

        @CreationTimestamp
        @Column(name = "applied_on", nullable = false, updatable = false)
        private LocalDate appliedOn;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "approved_by")
        private User approvedBy;
}