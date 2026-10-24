-- ==============================================================================
-- Seed Attendance Records for October 2026 (All 31 Days for All Employees)
-- ==============================================================================

-- Remove any existing October 2026 attendance records to prevent unique key conflicts
DELETE FROM attendance WHERE attendance_date BETWEEN '2026-10-01' AND '2026-10-31';

-- Insert comprehensive attendance for all employees
INSERT INTO attendance (
    employee_id,
    attendance_date,
    status,
    check_in,
    check_out,
    working_hours,
    overtime_hours,
    remarks
)
SELECT
    e.employee_id,
    d.dt::date as attendance_date,
    
    -- Status determination
    CASE
        -- October 2nd: Gandhi Jayanti (National Holiday)
        WHEN d.dt::date = '2026-10-02' THEN 'HOLIDAY'
        -- Saturdays (DOW = 6) and Sundays (DOW = 0): Weekend
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN 'WEEKEND'
        -- Approved Leave for Employee 6
        WHEN e.employee_id = 6 AND d.dt::date BETWEEN '2026-10-05' AND '2026-10-09' THEN 'ON_LEAVE'
        -- Sample Leave days
        WHEN (e.employee_id % 15 = 0) AND d.dt::date IN ('2026-10-12', '2026-10-13') THEN 'ON_LEAVE'
        -- Sample Half days
        WHEN (e.employee_id % 7 = 0) AND d.dt::date = '2026-10-16' THEN 'HALF_DAY'
        -- Sample Absent days
        WHEN (e.employee_id % 11 = 0) AND d.dt::date = '2026-10-20' THEN 'ABSENT'
        -- Standard working days
        ELSE 'PRESENT'
    END AS status,
    
    -- Check In Time
    CASE
        WHEN d.dt::date = '2026-10-02' THEN NULL
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN NULL
        WHEN e.employee_id = 6 AND d.dt::date BETWEEN '2026-10-05' AND '2026-10-09' THEN NULL
        WHEN (e.employee_id % 15 = 0) AND d.dt::date IN ('2026-10-12', '2026-10-13') THEN NULL
        WHEN (e.employee_id % 11 = 0) AND d.dt::date = '2026-10-20' THEN NULL
        WHEN (e.employee_id % 7 = 0) AND d.dt::date = '2026-10-16' THEN '09:30:00'::time
        ELSE ('09:00:00'::time + ((e.employee_id % 15) * interval '1 minute'))::time
    END AS check_in,

    -- Check Out Time
    CASE
        WHEN d.dt::date = '2026-10-02' THEN NULL
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN NULL
        WHEN e.employee_id = 6 AND d.dt::date BETWEEN '2026-10-05' AND '2026-10-09' THEN NULL
        WHEN (e.employee_id % 15 = 0) AND d.dt::date IN ('2026-10-12', '2026-10-13') THEN NULL
        WHEN (e.employee_id % 11 = 0) AND d.dt::date = '2026-10-20' THEN NULL
        WHEN (e.employee_id % 7 = 0) AND d.dt::date = '2026-10-16' THEN '13:30:00'::time
        -- Overtime on Wednesdays for some employees
        WHEN (e.employee_id % 5 = 0) AND EXTRACT(DOW FROM d.dt) = 3 THEN '19:30:00'::time
        ELSE '17:30:00'::time
    END AS check_out,

    -- Working Hours
    CASE
        WHEN d.dt::date = '2026-10-02' THEN 0.00
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN 0.00
        WHEN e.employee_id = 6 AND d.dt::date BETWEEN '2026-10-05' AND '2026-10-09' THEN 0.00
        WHEN (e.employee_id % 15 = 0) AND d.dt::date IN ('2026-10-12', '2026-10-13') THEN 0.00
        WHEN (e.employee_id % 11 = 0) AND d.dt::date = '2026-10-20' THEN 0.00
        WHEN (e.employee_id % 7 = 0) AND d.dt::date = '2026-10-16' THEN 4.00
        ELSE 8.00
    END AS working_hours,

    -- Overtime Hours
    CASE
        WHEN d.dt::date = '2026-10-02' THEN 0.00
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN 0.00
        WHEN (e.employee_id % 5 = 0) AND EXTRACT(DOW FROM d.dt) = 3 AND d.dt::date != '2026-10-02' THEN 2.00
        ELSE 0.00
    END AS overtime_hours,

    -- Remarks
    CASE
        WHEN d.dt::date = '2026-10-02' THEN 'Gandhi Jayanti'
        WHEN EXTRACT(DOW FROM d.dt) IN (0, 6) THEN 'Weekend'
        WHEN e.employee_id = 6 AND d.dt::date BETWEEN '2026-10-05' AND '2026-10-09' THEN 'Approved Annual Leave'
        WHEN (e.employee_id % 15 = 0) AND d.dt::date IN ('2026-10-12', '2026-10-13') THEN 'Approved Casual Leave'
        WHEN (e.employee_id % 7 = 0) AND d.dt::date = '2026-10-16' THEN 'Personal work - half day'
        WHEN (e.employee_id % 11 = 0) AND d.dt::date = '2026-10-20' THEN 'Uninformed Absent'
        ELSE 'Regular working day'
    END AS remarks
FROM
    employee e
CROSS JOIN
    generate_series('2026-10-01'::date, '2026-10-31'::date, '1 day'::interval) d(dt)
ORDER BY
    e.employee_id, d.dt;
