package com.example.PayrollProcessingSystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
