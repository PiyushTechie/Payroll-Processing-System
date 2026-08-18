package com.example.PayrollProcessingSystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.PayrollComponent;

@Repository
public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, Long> {
}
