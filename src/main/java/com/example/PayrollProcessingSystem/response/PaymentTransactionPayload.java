package com.example.PayrollProcessingSystem.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.PayrollProcessingSystem.entity.PaymentTransaction.TransactionStatus;

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
public class PaymentTransactionPayload {
    private Long transactionId;
    private Long payrollRecordId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String batchId;
    private String paymentMethod;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime transactionDate;
    private String bankReferenceNumber;
    private Integer retryCount;
}
