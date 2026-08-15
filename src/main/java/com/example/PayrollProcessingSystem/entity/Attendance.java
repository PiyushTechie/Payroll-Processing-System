package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.example.PayrollProcessingSystem.enums.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an employee's daily attendance record.
 * Captures attendance status, check-in/check-out times, working hours, and
 * remarks.
 */

@Entity
@Table(name = "attendance", uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_employee_date", columnNames = { "employee_id",
                                "attendance_date" })
}, indexes = {
                @Index(name = "idx_attendance_employee", columnList = "employee_id"),
                @Index(name = "idx_attendance_date", columnList = "attendance_date"),
                @Index(name = "idx_attendance_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long attendanceId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "employee_id", nullable = false)
        @JsonBackReference("employee-attendances")
        private Employee employee;

        @NotNull
        @Column(name = "attendance_date", nullable = false)
        private LocalDate attendanceDate;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private AttendanceStatus status;

        @Column(name = "check_in")
        private LocalTime checkIn;

        @Column(name = "check_out")
        private LocalTime checkOut;

        @PositiveOrZero
        @Column(name = "working_hours", precision = 4, scale = 2)
        private BigDecimal workingHours;

        @PositiveOrZero
        @Column(name = "overtime_hours", precision = 4, scale = 2)
        private BigDecimal overtimeHours;

        @Column(name = "remarks", nullable = true, length = 255)
        private String remarks;
}