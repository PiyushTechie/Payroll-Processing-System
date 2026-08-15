package com.example.PayrollProcessingSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "Attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_Attendance_employee_date",
                        columnNames = {"employee_id", "attendance_date"}
                )
        },
        indexes = {
                @Index(name = "idx_attendance_employee", columnList = "employee_id"),
                @Index(name = "idx_attendance_date", columnList = "attendance_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID attendanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String remarks;
}