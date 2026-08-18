package com.example.PayrollProcessingSystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.SalaryStructureDetail;

@Repository
public interface SalaryStructureDetailRepository extends JpaRepository<SalaryStructureDetail, Long> {
}
