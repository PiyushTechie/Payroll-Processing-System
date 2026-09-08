package com.example.PayrollProcessingSystem.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a generated payslip document providing details of an employee's
 * pay for a specific period.
 * Typically includes a reference to a downloadable PDF.
 */
@Entity
@Table(name = "payslip", indexes = {
        @Index(name = "idx_payslip_payroll_record", columnList = "payroll_record_id"),
        @Index(name = "idx_payslip_employee", columnList = "employee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    public enum PayslipStatus {
        PENDING,
        GENERATED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payslipId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_record_id", nullable = false, unique = true)
    @JsonBackReference("payroll-record-payslip")
    private PayrollRecord payrollRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference("employee-payslips")
    private Employee employee;

    @NotNull
    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayslipStatus status = PayslipStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.generatedDate == null) {
            this.generatedDate = LocalDate.now();
        }
    }
}