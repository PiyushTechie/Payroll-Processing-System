package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;

import com.example.PayrollProcessingSystem.enums.ComponentCategory;
import com.example.PayrollProcessingSystem.enums.ComponentName;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an individual line item (earning or deduction) within a
 * payroll record.
 * Provides granular detail for each component of an employee's pay.
 */

@Entity
@Table(name = "payroll_item", indexes = {
        @Index(name = "idx_payroll_item_record", columnList = "payroll_record_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_record_id", nullable = false)
    @JsonBackReference("payroll-record-items")
    private PayrollRecord payrollRecord;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "component_name", nullable = false, length = 50)
    private ComponentName componentName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "component_category", nullable = false, length = 20)
    private ComponentCategory componentCategory;

    @NotNull
    @PositiveOrZero
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}