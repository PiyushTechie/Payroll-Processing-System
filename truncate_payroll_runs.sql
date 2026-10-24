-- ==============================================================================
-- Reset & Truncate Payroll Runs
-- Clears all payroll execution history and restarts auto-increment IDs at 1.
-- Keeps Employees, Users, Departments, Attendance, and Salary structures intact.
-- ==============================================================================

TRUNCATE TABLE 
    payment_transaction, 
    payslip, 
    payroll_item, 
    payroll_record, 
    payroll_run 
RESTART IDENTITY CASCADE;
