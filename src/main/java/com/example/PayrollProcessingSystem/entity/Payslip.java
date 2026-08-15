package com.example.PayrollProcessingSystem.entity;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

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
        @Index(name = "idx_payslip_payroll_record", columnList = "payroll_record_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payslipId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_record_id", nullable = false, unique = true)
    @JsonBackReference("payroll-record-payslip")
    private PayrollRecord payrollRecord;

    @CreationTimestamp
    @Column(name = "generated_date", nullable = false, updatable = false)
    private LocalDate generatedDate;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;
}