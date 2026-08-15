package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * 
 * Represents a single component within a salary structure.
 * Links a salary structure to a payroll component with a specific amount.
 * 
 */

@Entity
@Table(name = "salary_structure_detail", uniqueConstraints = {
        @UniqueConstraint(name = "uk_salary_struct_detail_comp", columnNames = { "salary_structure_id",
                "component_id" })
}, indexes = {
        @Index(name = "idx_salary_struct_detail_struct", columnList = "salary_structure_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructureDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    @JsonBackReference("salary-structure-details")
    private SalaryStructure salaryStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private PayrollComponent component;

    @NotNull
    @PositiveOrZero
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}