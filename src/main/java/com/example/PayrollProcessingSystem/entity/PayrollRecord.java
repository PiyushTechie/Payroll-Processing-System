package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.PayrollProcessingSystem.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an individual employee's payroll calculation record within a
 * specific
 * payroll run.
 * Contains all earnings, deductions, and net pay details for one employee.
 */

@Entity
@Table(name = "payroll_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_record_run_employee", columnNames = { "payroll_run_id", "employee_id" })
}, indexes = {
        @Index(name = "idx_payroll_record_employee", columnList = "employee_id"),
        @Index(name = "idx_payroll_record_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollRecordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    @JsonBackReference("run-records")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference("employee-payroll-records")
    private Employee employee;

    @NotNull
    @PositiveOrZero
    @Column(name = "gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @NotNull
    @PositiveOrZero
    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    @Builder.Default
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("payroll-record-items")
    private List<PayrollItem> payrollItems = new ArrayList<>();

    @OneToOne(mappedBy = "payrollRecord", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference("payroll-record-payslip")
    private Payslip payslip;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRecord", fetch = FetchType.LAZY)
    @JsonManagedReference("payroll-record-payments")
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    // Helper methods for bi-directional consistency
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

    public void addPaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransactions.add(paymentTransaction);
        paymentTransaction.setPayrollRecord(this);
    }
}