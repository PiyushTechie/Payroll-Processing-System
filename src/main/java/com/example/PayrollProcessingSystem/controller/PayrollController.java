package com.example.PayrollProcessingSystem.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.PayrollProcessingSystem.entity.Employee;
import com.example.PayrollProcessingSystem.entity.PaymentTransaction;
import com.example.PayrollProcessingSystem.entity.PayrollComponent;
import com.example.PayrollProcessingSystem.entity.PayrollItem;
import com.example.PayrollProcessingSystem.entity.PayrollRecord;
import com.example.PayrollProcessingSystem.entity.PayrollRun;
import com.example.PayrollProcessingSystem.entity.Payslip;
import com.example.PayrollProcessingSystem.entity.SalaryStructure;
import com.example.PayrollProcessingSystem.entity.SalaryStructureDetail;
import com.example.PayrollProcessingSystem.entity.User;
import com.example.PayrollProcessingSystem.exception.BadRequestException;
import com.example.PayrollProcessingSystem.exception.ResourceNotFoundException;
import com.example.PayrollProcessingSystem.repository.EmployeeRepository;
import com.example.PayrollProcessingSystem.repository.PaymentTransactionRepository;
import com.example.PayrollProcessingSystem.repository.PayrollRecordRepository;
import com.example.PayrollProcessingSystem.repository.PayrollRunRepository;
import com.example.PayrollProcessingSystem.repository.PayslipRepository;
import com.example.PayrollProcessingSystem.repository.SalaryStructureRepository;
import com.example.PayrollProcessingSystem.repository.UserRepository;
import com.example.PayrollProcessingSystem.response.PaymentTransactionPayload;
import com.example.PayrollProcessingSystem.response.PayrollRunSummaryPayload;
import com.example.PayrollProcessingSystem.response.PayslipPayload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payroll")
@Tag(name = "Payroll", description = "Endpoints for managing the end-to-end payroll flow with demo payment processing")
public class PayrollController {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PayslipRepository payslipRepository;
    private final UserRepository userRepository;

    public PayrollController(
            PayrollRunRepository payrollRunRepository,
            PayrollRecordRepository payrollRecordRepository,
            EmployeeRepository employeeRepository,
            SalaryStructureRepository salaryStructureRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PayslipRepository payslipRepository,
            UserRepository userRepository) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.employeeRepository = employeeRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.payslipRepository = payslipRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate")
    @Operation(summary = "Step 1: Generate Payroll (HR)", description = "Calculates salaries and creates a draft payroll run with itemized records.")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<PayrollRunSummaryPayload> generatePayroll(@RequestParam int year, @RequestParam int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12.");
        }

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = LocalDate.of(year, month, periodStart.lengthOfMonth());

        // 1. Create a Draft Payroll Run
        PayrollRun run = PayrollRun.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .runDate(LocalDate.now())
                .status(PayrollRun.PayrollRunStatus.GENERATED)
                .totalEmployees(0)
                .totalGrossSalary(BigDecimal.ZERO)
                .totalNetSalary(BigDecimal.ZERO)
                .build();

        List<Employee> activeEmployees = employeeRepository.findAll();
        int totalDays = periodStart.lengthOfMonth();

        // 2. Calculate salaries for all active employees
        for (Employee emp : activeEmployees) {
            if (emp.getStatus() != Employee.EmployeeStatus.ACTIVE) {
                continue;
            }

            Optional<SalaryStructure> activeStructureOpt = salaryStructureRepository.findActiveStructure(emp.getEmployeeId(), periodStart);

            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;
            List<PayrollItem> items = new ArrayList<>();

            if (activeStructureOpt.isPresent() && !activeStructureOpt.get().getDetails().isEmpty()) {
                SalaryStructure structure = activeStructureOpt.get();
                for (SalaryStructureDetail detail : structure.getDetails()) {
                    PayrollComponent comp = detail.getComponent();
                    BigDecimal compAmount = detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO;

                    PayrollItem.ComponentName compName = mapComponentName(comp.getName());
                    items.add(PayrollItem.builder()
                            .componentName(compName)
                            .componentCategory(comp.getCategory())
                            .amount(compAmount)
                            .build());

                    if (comp.getCategory() == PayrollComponent.ComponentCategory.EARNING) {
                        gross = gross.add(compAmount);
                    } else {
                        deductions = deductions.add(compAmount);
                    }
                }
            } else {
                // Sensible demo breakdown when custom salary structure is not configured
                BigDecimal basic = new BigDecimal("50000.00");
                BigDecimal hra = new BigDecimal("20000.00");
                BigDecimal special = new BigDecimal("10000.00");
                BigDecimal pf = new BigDecimal("6000.00");
                BigDecimal tax = new BigDecimal("2500.00");

                items.add(PayrollItem.builder().componentName(PayrollItem.ComponentName.BASIC).componentCategory(PayrollComponent.ComponentCategory.EARNING).amount(basic).build());
                items.add(PayrollItem.builder().componentName(PayrollItem.ComponentName.HRA).componentCategory(PayrollComponent.ComponentCategory.EARNING).amount(hra).build());
                items.add(PayrollItem.builder().componentName(PayrollItem.ComponentName.SPECIAL_ALLOWANCE).componentCategory(PayrollComponent.ComponentCategory.EARNING).amount(special).build());
                items.add(PayrollItem.builder().componentName(PayrollItem.ComponentName.PF).componentCategory(PayrollComponent.ComponentCategory.DEDUCTION).amount(pf).build());
                items.add(PayrollItem.builder().componentName(PayrollItem.ComponentName.PROFESSIONAL_TAX).componentCategory(PayrollComponent.ComponentCategory.TAX).amount(tax).build());

                gross = basic.add(hra).add(special);
                deductions = pf.add(tax);
            }

            BigDecimal net = gross.subtract(deductions);
            if (net.compareTo(BigDecimal.ZERO) < 0) {
                net = BigDecimal.ZERO;
            }

            PayrollRecord record = PayrollRecord.builder()
                    .employee(emp)
                    .totalDays(totalDays)
                    .payableDays(totalDays)
                    .lopDays(0)
                    .grossSalary(gross)
                    .totalDeductions(deductions)
                    .netSalary(net)
                    .paymentStatus(PayrollRecord.PaymentStatus.PENDING)
                    .build();

            for (PayrollItem item : items) {
                record.addPayrollItem(item);
            }

            run.addPayrollRecord(record);
            run.setTotalEmployees(run.getTotalEmployees() + 1);
            run.setTotalGrossSalary(run.getTotalGrossSalary().add(gross));
            run.setTotalNetSalary(run.getTotalNetSalary().add(net));
        }

        run = payrollRunRepository.save(run);
        return ResponseEntity.ok(mapToSummaryPayload(run));
    }

    @PostMapping("/{runId}/approve")
    @Operation(summary = "Step 2: Approve Payroll (Finance)", description = "Finance reviews and approves the draft payroll run.")
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> approvePayroll(@PathVariable Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll Run not found with ID: " + runId));

        if (run.getStatus() != PayrollRun.PayrollRunStatus.GENERATED) {
            throw new BadRequestException("Payroll must be in GENERATED state to approve. Current status: " + run.getStatus());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userRepository.findByUsername(authentication.getName()).ifPresent(run::setApprovedBy);
        }

        run.setStatus(PayrollRun.PayrollRunStatus.FINANCE_APPROVED);
        payrollRunRepository.save(run);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payroll Run " + runId + " approved by Finance. Ready for payout.");
        response.put("payrollRunId", runId);
        response.put("status", run.getStatus().name());
        response.put("totalEmployees", run.getTotalEmployees());
        response.put("totalNetSalary", run.getTotalNetSalary());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{runId}/payout")
    @Operation(summary = "Step 3: Execute Demo Payouts (Finance)", description = "Simulates direct deposit payouts to employee bank accounts and creates audit transactions and payslips.")
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> executePayouts(@PathVariable Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll Run not found with ID: " + runId));

        if (run.getStatus() != PayrollRun.PayrollRunStatus.FINANCE_APPROVED) {
            throw new BadRequestException("Payroll must be FINANCE_APPROVED to execute payouts. Current status: " + run.getStatus());
        }

        String batchId = "DEMO-BATCH-" + runId + "-" + System.currentTimeMillis();
        int successCount = 0;
        BigDecimal totalDisbursed = BigDecimal.ZERO;
        List<Map<String, Object>> sampleTransactions = new ArrayList<>();

        for (PayrollRecord record : run.getPayrollRecords()) {
            Employee emp = record.getEmployee();

            // Simulate direct deposit transfer
            String bankRef = "DEMO-UTR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .payrollRecord(record)
                    .employee(emp)
                    .batchId(batchId)
                    .paymentMethod("DIRECT_DEPOSIT")
                    .amount(record.getNetSalary())
                    .status(PaymentTransaction.TransactionStatus.SUCCESS)
                    .transactionDate(LocalDateTime.now())
                    .bankReferenceNumber(bankRef)
                    .retryCount(0)
                    .build();

            paymentTransactionRepository.save(transaction);
            record.addPaymentTransaction(transaction);
            record.setPaymentStatus(PayrollRecord.PaymentStatus.PAID);

            // Generate or update Payslip
            Optional<Payslip> existingPayslip = payslipRepository.findByPayrollRecordPayrollRecordId(record.getPayrollRecordId());
            Payslip payslip = existingPayslip.orElseGet(() -> Payslip.builder()
                    .payrollRecord(record)
                    .employee(emp)
                    .generatedDate(LocalDate.now())
                    .pdfUrl("/api/payroll/payslips/" + record.getPayrollRecordId())
                    .status(Payslip.PayslipStatus.GENERATED)
                    .build());
            payslip.setStatus(Payslip.PayslipStatus.GENERATED);
            payslipRepository.save(payslip);
            record.setPayslip(payslip);

            successCount++;
            totalDisbursed = totalDisbursed.add(record.getNetSalary());

            if (sampleTransactions.size() < 5) {
                sampleTransactions.add(Map.of(
                        "employeeCode", emp.getEmployeeCode(),
                        "employeeName", emp.getFirstName() + " " + emp.getLastName(),
                        "bankAccount", maskAccount(emp.getBankAccountNumber()),
                        "ifsc", emp.getIfscCode() != null ? emp.getIfscCode() : "SBIN0001234",
                        "amount", record.getNetSalary(),
                        "bankReferenceNumber", bankRef
                ));
            }
        }

        run.setStatus(PayrollRun.PayrollRunStatus.PAID);
        payrollRunRepository.save(run);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Demo direct deposit payout executed successfully.");
        response.put("payrollRunId", runId);
        response.put("batchId", batchId);
        response.put("status", run.getStatus().name());
        response.put("successfulTransfers", successCount);
        response.put("totalAmountDisbursed", totalDisbursed);
        response.put("sampleTransactions", sampleTransactions);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get Payroll Run Details", description = "Fetches complete details and employee records for a given payroll run.")
    @PreAuthorize("hasRole('HR') or hasRole('FINANCE') or hasRole('ADMIN')")
    public ResponseEntity<PayrollRunSummaryPayload> getPayrollRun(@PathVariable Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll Run not found with ID: " + runId));
        return ResponseEntity.ok(mapToSummaryPayload(run));
    }

    @GetMapping("/runs")
    @Operation(summary = "List All Payroll Runs", description = "Retrieves all payroll runs ordered by period date.")
    @PreAuthorize("hasRole('HR') or hasRole('FINANCE') or hasRole('ADMIN')")
    public ResponseEntity<List<PayrollRunSummaryPayload>> getAllPayrollRuns() {
        List<PayrollRun> runs = payrollRunRepository.findAll();
        List<PayrollRunSummaryPayload> payloadList = runs.stream()
                .map(this::mapToSummaryPayload)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payloadList);
    }

    @GetMapping("/{runId}/transactions")
    @Operation(summary = "Get Payment Transactions for Run", description = "Lists simulated direct deposit transactions and bank reference numbers for a payroll run.")
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentTransactionPayload>> getRunTransactions(@PathVariable Long runId) {
        if (!payrollRunRepository.existsById(runId)) {
            throw new ResourceNotFoundException("Payroll Run not found with ID: " + runId);
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByPayrollRecordPayrollRunPayrollRunId(runId);
        List<PaymentTransactionPayload> payloads = transactions.stream()
                .map(t -> PaymentTransactionPayload.builder()
                        .transactionId(t.getTransactionId())
                        .payrollRecordId(t.getPayrollRecord().getPayrollRecordId())
                        .employeeId(t.getEmployee().getEmployeeId())
                        .employeeCode(t.getEmployee().getEmployeeCode())
                        .employeeName(t.getEmployee().getFirstName() + " " + t.getEmployee().getLastName())
                        .batchId(t.getBatchId())
                        .paymentMethod(t.getPaymentMethod())
                        .amount(t.getAmount())
                        .status(t.getStatus())
                        .transactionDate(t.getTransactionDate())
                        .bankReferenceNumber(t.getBankReferenceNumber())
                        .retryCount(t.getRetryCount())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(payloads);
    }

    @GetMapping("/payslips/{payrollRecordId}")
    @Operation(summary = "Get Payslip for Payroll Record", description = "Retrieves full breakdown of earnings, deductions, and payment status for an employee payslip.")
    @PreAuthorize("hasAnyRole('HR', 'FINANCE', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<PayslipPayload> getPayslip(@PathVariable Long payrollRecordId) {
        PayrollRecord record = payrollRecordRepository.findById(payrollRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll Record not found with ID: " + payrollRecordId));

        Employee emp = record.getEmployee();
        PayrollRun run = record.getPayrollRun();

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByPayrollRecordPayrollRecordId(payrollRecordId);
        String bankRef = transactions.isEmpty() ? "N/A" : transactions.get(0).getBankReferenceNumber();

        Optional<Payslip> payslipOpt = payslipRepository.findByPayrollRecordPayrollRecordId(payrollRecordId);

        List<PayslipPayload.PayrollItemPayload> itemPayloads = record.getPayrollItems().stream()
                .map(item -> PayslipPayload.PayrollItemPayload.builder()
                        .componentName(item.getComponentName().name())
                        .category(item.getComponentCategory().name())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());

        PayslipPayload payload = PayslipPayload.builder()
                .payslipId(payslipOpt.map(Payslip::getPayslipId).orElse(null))
                .payrollRecordId(record.getPayrollRecordId())
                .employeeId(emp.getEmployeeId())
                .employeeCode(emp.getEmployeeCode())
                .employeeName(emp.getFirstName() + " " + emp.getLastName())
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getDepartmentName() : "N/A")
                .designation(emp.getDesignation())
                .bankAccountNumber(maskAccount(emp.getBankAccountNumber()))
                .ifscCode(emp.getIfscCode())
                .periodStart(run.getPeriodStart())
                .periodEnd(run.getPeriodEnd())
                .payableDays(record.getPayableDays())
                .lopDays(record.getLopDays())
                .grossSalary(record.getGrossSalary())
                .totalDeductions(record.getTotalDeductions())
                .netSalary(record.getNetSalary())
                .generatedDate(payslipOpt.map(Payslip::getGeneratedDate).orElse(LocalDate.now()))
                .pdfUrl(payslipOpt.map(Payslip::getPdfUrl).orElse("/api/payroll/payslips/" + record.getPayrollRecordId()))
                .status(payslipOpt.map(Payslip::getStatus).orElse(Payslip.PayslipStatus.PENDING))
                .paymentStatus(record.getPaymentStatus().name())
                .bankReferenceNumber(bankRef)
                .items(itemPayloads)
                .build();

        return ResponseEntity.ok(payload);
    }

    private PayrollRunSummaryPayload mapToSummaryPayload(PayrollRun run) {
        List<PayrollRunSummaryPayload.PayrollRecordSummaryPayload> recordPayloads = new ArrayList<>();
        if (run.getPayrollRecords() != null) {
            for (PayrollRecord rec : run.getPayrollRecords()) {
                Employee emp = rec.getEmployee();
                String utr = null;
                if (rec.getPaymentTransactions() != null && !rec.getPaymentTransactions().isEmpty()) {
                    utr = rec.getPaymentTransactions().get(0).getBankReferenceNumber();
                }

                recordPayloads.add(PayrollRunSummaryPayload.PayrollRecordSummaryPayload.builder()
                        .payrollRecordId(rec.getPayrollRecordId())
                        .employeeId(emp.getEmployeeId())
                        .employeeCode(emp.getEmployeeCode())
                        .employeeName(emp.getFirstName() + " " + emp.getLastName())
                        .designation(emp.getDesignation())
                        .payableDays(rec.getPayableDays())
                        .grossSalary(rec.getGrossSalary())
                        .totalDeductions(rec.getTotalDeductions())
                        .netSalary(rec.getNetSalary())
                        .paymentStatus(rec.getPaymentStatus().name())
                        .bankAccountNumber(maskAccount(emp.getBankAccountNumber()))
                        .ifscCode(emp.getIfscCode())
                        .bankReferenceNumber(utr)
                        .build());
            }
        }

        return PayrollRunSummaryPayload.builder()
                .payrollRunId(run.getPayrollRunId())
                .periodStart(run.getPeriodStart())
                .periodEnd(run.getPeriodEnd())
                .runDate(run.getRunDate())
                .status(run.getStatus())
                .totalEmployees(run.getTotalEmployees())
                .totalGrossSalary(run.getTotalGrossSalary())
                .totalNetSalary(run.getTotalNetSalary())
                .approvedByUsername(run.getApprovedBy() != null ? run.getApprovedBy().getUsername() : null)
                .records(recordPayloads)
                .build();
    }

    private PayrollItem.ComponentName mapComponentName(String name) {
        if (name == null) return PayrollItem.ComponentName.BASIC;
        String upper = name.toUpperCase().trim();
        if (upper.contains("BASIC")) return PayrollItem.ComponentName.BASIC;
        if (upper.contains("HRA") || upper.contains("RENT")) return PayrollItem.ComponentName.HRA;
        if (upper.contains("DA") || upper.contains("DEARNESS")) return PayrollItem.ComponentName.DA;
        if (upper.contains("SPECIAL")) return PayrollItem.ComponentName.SPECIAL_ALLOWANCE;
        if (upper.contains("BONUS")) return PayrollItem.ComponentName.BONUS;
        if (upper.contains("OVERTIME")) return PayrollItem.ComponentName.OVERTIME;
        if (upper.contains("PF") || upper.contains("PROVIDENT")) return PayrollItem.ComponentName.PF;
        if (upper.contains("PROFESSIONAL")) return PayrollItem.ComponentName.PROFESSIONAL_TAX;
        if (upper.contains("INCOME") || upper.contains("TAX")) return PayrollItem.ComponentName.INCOME_TAX;
        return PayrollItem.ComponentName.BASIC;
    }

    private String maskAccount(String account) {
        if (account == null || account.length() < 4) {
            return "XXXX-DEMO";
        }
        return "XXXX-XXXX-" + account.substring(account.length() - 4);
    }
}
