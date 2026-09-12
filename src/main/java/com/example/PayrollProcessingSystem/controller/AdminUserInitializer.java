package com.example.PayrollProcessingSystem.controller;

import com.example.PayrollProcessingSystem.entity.Department;
import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.repository.DepartmentRepository;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class AdminUserInitializer {

    @Bean
    public CommandLineRunner initAdminUser(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            // Check if admin already exists to prevent duplicate creation on restarts
            if (userRepository.findByUsername("admin").isEmpty()) {
                
                // 1. Create a Department for the Admin
                Department adminDept = departmentRepository.findByDepartmentName("Management")
                        .orElseGet(() -> {
                            Department dept = Department.builder()
                                    .departmentName("Management")
                                    .description("Top-level management department")
                                    .build();
                            return departmentRepository.save(dept);
                        });

                // 2. Create an Employee record for the Admin
                Employee adminEmployee = employeeRepository.findByEmail("admin@example.com")
                        .orElseGet(() -> {
                            Employee emp = Employee.builder()
                                    .employeeCode("EMP-ADMIN")
                                    .firstName("System")
                                    .lastName("Admin")
                                    .email("admin@example.com")
                                    .phone("1234567890")
                                    .designation("System Administrator")
                                    .employmentType(Employee.EmploymentType.FULL_TIME)
                                    .dateOfJoining(LocalDate.now())
                                    .status(Employee.EmployeeStatus.ACTIVE)
                                    .department(adminDept)
                                    .build();
                            return employeeRepository.save(emp);
                        });

                // 3. Create the User record linked to the Employee
                User adminUser = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123")) // Default password
                        .role(User.Role.ADMIN)
                        .status(User.UserStatus.ACTIVE)
                        .employee(adminEmployee)
                        .build();

                userRepository.save(adminUser);
                System.out.println("====== SAMPLE ADMIN USER CREATED ======");
                System.out.println("Username: admin");
                System.out.println("Password: admin123");
                System.out.println("=======================================");
            }
        };
    }
}
