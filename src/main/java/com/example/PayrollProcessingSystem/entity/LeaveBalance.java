package com.example.PayrollProcessingSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "leave_balance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leave_balance_employee",
                        columnNames = "employee_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID leaveBalanceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer totalLeaves;

    @Column(nullable = false)
    private Integer usedLeaves;

    @Column(nullable = false)
    private Integer remainingLeaves;
}