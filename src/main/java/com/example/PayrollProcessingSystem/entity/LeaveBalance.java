package com.example.PayrollProcessingSystem.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.example.PayrollProcessingSystem.enums.LeaveType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an employee's leave balance showing total and used leave days.
 * Tracks available leave balances for different leave types per employee.
 */

@Entity
@Table(name = "leave_balance", uniqueConstraints = {
                @UniqueConstraint(name = "uk_leave_balance_emp_type", columnNames = { "employee_id", "leave_type" })
}, indexes = {
                @Index(name = "idx_leave_balance_employee", columnList = "employee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long leaveBalanceId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "employee_id", nullable = false)
        @JsonBackReference("employee-leave-balances")
        private Employee employee;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "leave_type", nullable = false, length = 30)
        private LeaveType leaveType;

        @NotNull
        @Min(0)
        @Column(name = "total_leaves", nullable = false)
        private Integer totalLeaves;

        @Builder.Default
        @NotNull
        @Min(0)
        @Column(name = "used_leaves", nullable = false)
        private Integer usedLeaves = 0;

        @Transient
        public Integer getRemainingLeaves() {
                if (totalLeaves == null)
                        return 0;
                return totalLeaves - (usedLeaves != null ? usedLeaves : 0);
        }
}