package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Department;
import com.example.PayrollProcessingSystem.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        return departmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Department> getDepartmentByName(@RequestParam String name) {
        return departmentRepository.findByDepartmentName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        if (departmentRepository.findByDepartmentName(department.getDepartmentName()).isPresent()) {
            return ResponseEntity.badRequest().body("Department name already exists");
        }
        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department updated) {
        return departmentRepository.findById(id).map(dept -> {
            dept.setDepartmentName(updated.getDepartmentName());
            dept.setDescription(updated.getDescription());
            return ResponseEntity.ok(departmentRepository.save(dept));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}