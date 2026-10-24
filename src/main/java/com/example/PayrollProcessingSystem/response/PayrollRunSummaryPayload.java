package com.example.PayrollProcessingSystem.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.PayrollProcessingSystem.entity.PayrollRun.PayrollRunStatus;

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
public class PayrollRunSummaryPayload {
    private Long payrollRunId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate runDate;
    private PayrollRunStatus status;
    private Integer totalEmployees;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private String approvedByUsername;
    private List<PayrollRecordSummaryPayload> records;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayrollRecordSummaryPayload {
        private Long payrollRecordId;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String designation;
        private Integer payableDays;
        private BigDecimal grossSalary;
        private BigDecimal totalDeductions;
        private BigDecimal netSalary;
        private String paymentStatus;
        private String bankAccountNumber;
        private String ifscCode;
        private String bankReferenceNumber;
    }
}
