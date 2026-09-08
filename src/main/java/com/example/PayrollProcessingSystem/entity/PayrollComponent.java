package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public enum ComponentCategory {
        EARNING,
        DEDUCTION,
        TAX
    }

    public enum ComponentType {
        FLAT_AMOUNT,
        PERCENTAGE
    }

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
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ComponentType type = ComponentType.FLAT_AMOUNT;

    @DecimalMin(value = "0.00", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
    @Column(name = "percentage_value", precision = 5, scale = 2)
    private BigDecimal percentageValue;

    @Builder.Default
    @Column(name = "is_taxable", nullable = false)
    private Boolean isTaxable = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}