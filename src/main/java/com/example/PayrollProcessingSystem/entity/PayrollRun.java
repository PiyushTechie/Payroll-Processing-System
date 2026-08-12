package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.PayrollProcessingSystem.enums.PayrollRunStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a batch payroll processing event for a specific time period.
 * Groups together individual payroll records for multiple employees.
 */
@Entity
@Table(name = "payroll_runs", indexes = {
    @Index(name = "idx_payroll_run_status", columnList = "status"),
    @Index(name = "idx_payroll_run_period", columnList = "periodStart, periodEnd")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID payrollRunId;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(nullable = false)
    private LocalDate periodEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollRunStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDate runDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Builder.Default
    @PositiveOrZero
    @Column(nullable = false)
    private Integer totalEmployees = 0;

    @Builder.Default
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGrossSalary = BigDecimal.ZERO;

    @Builder.Default
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalNetSalary = BigDecimal.ZERO;

    @Builder.Default
    @OneToMany(
            mappedBy = "payrollRun",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<PayrollRecord> payrollRecords = new ArrayList<>();

    // Helper Methods
    public void addPayrollRecord(PayrollRecord payrollRecord) {
        payrollRecords.add(payrollRecord);
        payrollRecord.setPayrollRun(this);
    }

    public void removePayrollRecord(PayrollRecord payrollRecord) {
        payrollRecords.remove(payrollRecord);
        payrollRecord.setPayrollRun(null);
    }
}