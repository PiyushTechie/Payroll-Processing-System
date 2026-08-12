package com.example.PayrollProcessingSystem.enums;

/**
 * It Represents the status of a payroll run.
 */

public enum PayrollRunStatus {
    GENERATED,
    HR_APPROVED,
    FINANCE_APPROVED,
    PAID,
    FAILED
}