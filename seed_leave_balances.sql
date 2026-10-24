-- Seed initial leave balances for all 100 employees
-- Gives each employee CASUAL (12 days), SICK (12 days), and ANNUAL (18 days)
INSERT INTO leave_balance (employee_id, leave_type, total_leaves, used_leaves)
SELECT e.employee_id, 'CASUAL', 12, 0 FROM employee e
ON CONFLICT (employee_id, leave_type) DO NOTHING;

INSERT INTO leave_balance (employee_id, leave_type, total_leaves, used_leaves)
SELECT e.employee_id, 'SICK', 12, 0 FROM employee e
ON CONFLICT (employee_id, leave_type) DO NOTHING;

INSERT INTO leave_balance (employee_id, leave_type, total_leaves, used_leaves)
SELECT e.employee_id, 'ANNUAL', 18, 0 FROM employee e
ON CONFLICT (employee_id, leave_type) DO NOTHING;
