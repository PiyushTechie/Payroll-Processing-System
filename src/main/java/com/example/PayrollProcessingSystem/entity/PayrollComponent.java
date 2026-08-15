package com.example.PayrollProcessingSystem.entity;

import com.example.PayrollProcessingSystem.enums.ComponentCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a standard payroll component (earning or deduction)
 * that can be assigned to employees.
 * Defines system-wide payroll components like salary, bonus, tax, etc.
 */

@Entity
@Table(name = "payroll_component", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_component_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long componentId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ComponentCategory category;

    @Builder.Default
    @Column(name = "is_taxable", nullable = false)
    private Boolean isTaxable = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}