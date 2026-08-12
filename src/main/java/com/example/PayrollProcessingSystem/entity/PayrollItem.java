package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.PayrollProcessingSystem.enums.ComponentCategory;
import com.example.PayrollProcessingSystem.enums.ComponentName;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an individual component of an employee's payroll,
 * such as basic salary, allowances, or deductions.
 */
@Entity
@Table(name = "payroll_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID payrollItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_record_id", nullable = false)
    @JsonBackReference("payroll-record-items")
    private PayrollRecord payrollRecord;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentName componentName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentCategory componentCategory;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}