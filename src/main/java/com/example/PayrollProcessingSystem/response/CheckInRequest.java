package com.example.PayrollProcessingSystem.response;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private String remarks;
}
