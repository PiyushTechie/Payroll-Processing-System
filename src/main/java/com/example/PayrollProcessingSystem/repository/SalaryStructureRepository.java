package com.example.PayrollProcessingSystem.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.PayrollProcessingSystem.entity.SalaryStructure;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    // Query to find active salary structure for an employee on a given date
    @Query("SELECT s FROM SalaryStructure s WHERE s.employee.employeeId = :employeeId " +
            "AND s.effectiveFrom <= :date " +
            "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    Optional<SalaryStructure> findActiveStructure(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);
}