package com.example.PayrollProcessingSystem.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an employee's salary structure defining earnings and deductions.
 * Defines the structure of an employee's salary including components and their
 * amounts.
 */

@Entity
@Table(name = "salary_structure", indexes = {
        @Index(name = "idx_salary_structure_employee", columnList = "employee_id"),
        @Index(name = "idx_salary_structure_dates", columnList = "effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long salaryStructureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference("employee-salary-structures")
    private Employee employee;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Builder.Default
    @OneToMany(mappedBy = "salaryStructure", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("salary-structure-details")
    private List<SalaryStructureDetail> details = new ArrayList<>();

    // Helper methods for bi-directional relationship management
    public void addDetail(SalaryStructureDetail detail) {
        details.add(detail);
        detail.setSalaryStructure(this);
    }

    public void removeDetail(SalaryStructureDetail detail) {
        details.remove(detail);
        detail.setSalaryStructure(null);
    }
}