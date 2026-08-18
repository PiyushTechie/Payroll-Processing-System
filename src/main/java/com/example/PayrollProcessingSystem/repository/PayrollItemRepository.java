package com.example.PayrollProcessingSystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.PayrollItem;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {
}
