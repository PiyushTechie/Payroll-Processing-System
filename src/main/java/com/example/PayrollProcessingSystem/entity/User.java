package com.example.PayrollProcessingSystem.entity;

import com.example.PayrollProcessingSystem.enums.Role;
import com.example.PayrollProcessingSystem.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        @JoinColumn(name = "employee_id", nullable = false)
        @JsonBackReference("employee-user")
        private Employee employee;

        @NotBlank
        @Column(name = "username", nullable = false, length = 50)
        private String username;

        @NotBlank
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Column(name = "password_hash", nullable = false, length = 255)
        private String passwordHash;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false, length = 20)
        private Role role;

        @Builder.Default
        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        private UserStatus status = UserStatus.ACTIVE;
}