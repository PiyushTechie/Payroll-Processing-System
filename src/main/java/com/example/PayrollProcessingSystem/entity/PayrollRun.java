package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.PayrollProcessingSystem.enums.PayrollRunStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "payroll_run", indexes = {
        @Index(name = "idx_payroll_run_status", columnList = "status"),
        @Index(name = "idx_payroll_run_period", columnList = "period_start, period_end")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollRunId;

    @NotNull
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Builder.Default
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollRunStatus status = PayrollRunStatus.GENERATED;

    @NotNull
    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Version
    private Integer version;

    @Builder.Default
    @PositiveOrZero
    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Builder.Default
    @PositiveOrZero
    @Column(name = "total_gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGrossSalary = BigDecimal.ZERO;

    @Builder.Default
    @PositiveOrZero
    @Column(name = "total_net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalNetSalary = BigDecimal.ZERO;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("run-records")
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