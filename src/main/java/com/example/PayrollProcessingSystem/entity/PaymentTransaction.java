package com.example.PayrollProcessingSystem.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.PayrollProcessingSystem.enums.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a payment transaction made for a specific payroll record.
 * Tracks the status and details of bank transfers or other payment methods.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_batch", columnList = "batchId"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_record_id", nullable = false)
    @JsonBackReference("payroll-record-payments")
    private PayrollRecord payrollRecord;

    @Column(nullable = false)
    private String batchId;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Setter
    private String bankReferenceNumber;

    @Setter
    @Builder.Default
    @PositiveOrZero
    @Column(nullable = false)
    private Integer retryCount = 0;
}