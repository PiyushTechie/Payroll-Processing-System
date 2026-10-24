package com.example.PayrollProcessingSystem.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.PayrollProcessingSystem.entity.Payslip.PayslipStatus;

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
public class PayslipPayload {
    private Long payslipId;
    private Long payrollRecordId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String designation;
    private String bankAccountNumber;
    private String ifscCode;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer payableDays;
    private Integer lopDays;
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private LocalDate generatedDate;
    private String pdfUrl;
    private PayslipStatus status;
    private String paymentStatus;
    private String bankReferenceNumber;
    private List<PayrollItemPayload> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayrollItemPayload {
        private String componentName;
        private String category;
        private BigDecimal amount;
    }
}
