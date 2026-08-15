package com.example.PayrollProcessingSystem.entity;

import com.example.PayrollProcessingSystem.enums.Role;
import com.example.PayrollProcessingSystem.enums.UserStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a system user who can log in and perform actions.
 * Each user is linked to an employee record and assigned a role
 * that determines their access level in the system.
 */

@Entity
@Table(name = "users", uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_employee", columnNames = "employee_id")
}, indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_status", columnList = "status")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long userId;

        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "employee_id", nullable = false, unique = true)
        private Employee employee;

        @NotBlank
        @Column(nullable = false, unique = true, length = 50)
        private String username;

        @NotBlank
        @Column(nullable = false)
        private String passwordHash;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Role role;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private UserStatus status;
}