package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.PayrollProcessingSystem.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
 * Represents the payroll details for a specific employee in a specific payroll
 * run.
 * Contains aggregated totals for gross salary, deductions, and net salary.
 */
@Entity
@Table(name = "payroll_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_record_run_employee", columnNames = { "payroll_run_id", "employee_id" })
}, indexes = {
        @Index(name = "idx_payroll_record_employee", columnList = "employee_id"),
        @Index(name = "idx_payroll_record_status", columnList = "paymentStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID payrollRecordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    @JsonBackReference
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("payroll-record-items")
    private List<PayrollItem> payrollItems = new ArrayList<>();

    @OneToOne(mappedBy = "payrollRecord", fetch = FetchType.LAZY)
    @JsonManagedReference("payroll-record-payslip")
    private Payslip payslip;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRecord", fetch = FetchType.LAZY)
    @JsonManagedReference("payroll-record-payments")
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    public void addPayrollItem(PayrollItem payrollItem) {
        payrollItems.add(payrollItem);
        payrollItem.setPayrollRecord(this);
    }

    public void removePayrollItem(PayrollItem payrollItem) {
        payrollItems.remove(payrollItem);
        payrollItem.setPayrollRecord(null);
    }

    public void setPayslip(Payslip payslip) {
        this.payslip = payslip;

        if (payslip != null) {
            payslip.setPayrollRecord(this);
        }
    }

    public void addPaymentTransaction(
            PaymentTransaction paymentTransaction) {

        paymentTransactions.add(paymentTransaction);
        paymentTransaction.setPayrollRecord(this);
    }
}