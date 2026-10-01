package com.example.PayrollProcessingSystem.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.PayrollProcessingSystem.entity.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployee_EmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployee_EmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    boolean existsByEmployee_EmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
}
