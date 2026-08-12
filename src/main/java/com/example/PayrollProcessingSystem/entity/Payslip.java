package com.example.PayrollProcessingSystem.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a generated payslip document providing details of an employee's pay for a specific period.
 * Typically includes a reference to a downloadable PDF.
 */
@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID payslipId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_record_id", nullable = false, unique = true)
    @JsonBackReference("payroll-record-payments")
    private PayrollRecord payrollRecord;

    @NotNull
    @Column(nullable = false)
    private LocalDate generatedDate;

    private String pdfUrl;
}