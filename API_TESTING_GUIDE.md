# Payroll Processing System - Comprehensive API Testing & Edge Cases Guide

This document provides an end-to-end testing specification for the **Payroll Processing System REST APIs**. It covers all endpoints, authorization matrix, sample payloads, expected status codes, validation rules, business logic flows, and exhaustive edge cases (boundary values, state transitions, security violations, and error handling).

---

## 1. Authentication & Security Architecture Overview

### 1.1 Base Configuration
- **Base URL**: `http://localhost:8080`
- **Session Management**: Spring Session backed by Redis (`redis-session-store`).
- **Session Cookie**: `JSESSIONID` (HTTP-Only, lifespan 30 minutes, max 1 concurrent session per user).
- **CSRF Protection**: Spring Security `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
  - On login or any request, an `XSRF-TOKEN` cookie is issued.
  - All modifying HTTP methods (`POST`, `PUT`, `DELETE`) **must** include the header:
    ```http
    X-XSRF-TOKEN: <value_from_XSRF-TOKEN_cookie>
    ```

### 1.2 Test Credentials & Role-Based Access Control (RBAC)

All accounts share the default password: **`password123`**

| Role | Username | Password | Intended Permissions |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `password123` | Full access across all modules, configuration, and user management. |
| **HR** | `hr_user` | `password123` | Generate payroll draft, attendance management, allocate/review leaves, view employees & runs. |
| **FINANCE** | `finance_user` | `password123` | Approve payroll run, trigger demo payouts, view financial audit transactions & payslips. |
| **EMPLOYEE** | `emp0004` to `emp0100` | `password123` | Check-in/check-out, apply for leaves, view own attendance summary, view personal payslips. |

---

## 2. Global Error Handling Specification

All error responses from `@RestControllerAdvice` conform to the following schema:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive business error message",
  "timestamp": "2026-10-23T20:00:00",
  "fieldErrors": {
    "fieldName": "Validation constraint message"
  }
}
```

| HTTP Status | Trigger Condition |
| :--- | :--- |
| **200 OK / 201 Created** | Request processed successfully. |
| **400 Bad Request** | Business rule violation, illegal state transition, or bean validation failure (`@Valid`). |
| **401 Unauthorized** | Missing/invalid session cookie or bad login credentials. |
| **403 Forbidden** | User lacks required role for the method (`@PreAuthorize`). |
| **404 Not Found** | Referenced entity ID (employee, run, leave, user, department) does not exist. |
| **422 Unprocessable Content** | Domain-specific payroll calculation or processing failure. |
| **500 Internal Server Error** | Unexpected unhandled runtime exception. |

---

## 3. End-to-End API Test Scenarios & Edge Cases

---

### Module 1: Authentication & Session (`AuthController` / `SecurityConfig`)

#### 1.1 Login (`POST /login`)
- **Content-Type**: `application/x-www-form-urlencoded`
- **Body**: `username={username}&password={password}`
- **Happy Path Test**:
  - `admin` / `password123` ➔ Returns `200 OK` with `{"message": "Login successful"}`, sets `JSESSIONID` and `XSRF-TOKEN` cookies.
  - `hr_user` / `password123` ➔ Returns `200 OK`.
  - `finance_user` / `password123` ➔ Returns `200 OK`.
- **Edge Cases & Security Tests**:
  - **EC-AUTH-01 (Wrong Password)**: `admin` with `wrongpass` ➔ `401 Unauthorized` with `{"message": "username or password is incorrect"}`.
  - **EC-AUTH-02 (Non-existent User)**: `ghost_user` with `password123` ➔ `401 Unauthorized`.
  - **EC-AUTH-03 (Empty Credentials)**: Empty `username` or `password` ➔ `401 Unauthorized`.
  - **EC-AUTH-04 (Account Status Inactive/Locked)**: User with `status = LOCKED` ➔ `401 Unauthorized` (account locked check in `CustomUserDetails.isAccountNonLocked()`).
  - **EC-AUTH-05 (Single Active Session Enforcement)**:
    - Login as `admin` on Client A.
    - Login as `admin` on Client B.
    - Using Client A's session on subsequent calls should be invalidated (`maxSessionsPreventsLogin(false)`).
  - **EC-AUTH-06 (SQL Injection / Special Characters in Username)**: `' OR '1'='1` ➔ `401 Unauthorized` (handled safely via JPA parametrized queries).

#### 1.2 Logout (`POST /api/logout`)
- **Headers**: Session cookie + `X-XSRF-TOKEN`
- **Happy Path Test**: Returns `200 OK` with `{"message": "Logout successful"}`, clears `JSESSIONID`.
- **Edge Cases**:
  - **EC-AUTH-07 (Subsequent Request after Logout)**: Making any authenticated call (e.g. `GET /api/employees`) with the invalidated cookie ➔ `401 Unauthorized`.
  - **EC-AUTH-08 (Missing CSRF on Logout)**: POST without `X-XSRF-TOKEN` header ➔ `403 Forbidden` (CSRF token missing).

---

### Module 2: Payroll Engine (`PayrollController`)

#### 2.1 Step 1: Generate Payroll Run (`POST /api/payroll/generate?year={year}&month={month}`)
- **Authorization**: `ROLE_HR`, `ROLE_ADMIN`
- **Parameters**: `year` (e.g., `2026`), `month` (1-12)
- **Happy Path Test**:
  - As `hr_user`: `POST /api/payroll/generate?year=2026&month=10`
  - Returns `200 OK` with `PayrollRunSummaryPayload` containing `status: "GENERATED"`, list of 100 employee records, calculated `totalGrossSalary`, and `totalNetSalary`.
- **Edge Cases & Boundary Tests**:
  - **EC-PAY-01 (Invalid Month Range)**: `month=0` or `month=13` or `month=-1` ➔ `400 Bad Request` ("Month must be between 1 and 12.").
  - **EC-PAY-02 (Leap Year February)**: `year=2024&month=2` (29 days) vs `year=2025&month=2` (28 days) ➔ Verifies `periodStart=2024-02-01`, `periodEnd=2024-02-29`, and `totalDays=29`.
  - **EC-PAY-03 (Role Authorization Violation)**: Run as `finance_user` or `emp0004` ➔ `403 Forbidden`.
  - **EC-PAY-04 (Inactive Employees Exclusion)**: If employee has `status = INACTIVE` or `TERMINATED`, verify they are excluded from the payroll run calculations.
  - **EC-PAY-05 (Fallback Salary Structure)**: For employees without custom `salary_structure`, verify default fallback is applied (Basic: 50,000, HRA: 20,000, Special: 10,000, PF: 6,000, Tax: 2,500 ➔ Gross: 80,000, Net: 71,500).
  - **EC-PAY-06 (Net Salary Non-Negative Floor)**: If deductions exceed gross earnings, verify net salary is floored to `0.00` and never negative.

#### 2.2 Step 2: Approve Payroll Run (`POST /api/payroll/{runId}/approve`)
- **Authorization**: `ROLE_FINANCE`, `ROLE_ADMIN`
- **Happy Path Test**:
  - As `finance_user`: `POST /api/payroll/1/approve`
  - Returns `200 OK` with `status: "FINANCE_APPROVED"`, `approvedBy` set to `finance_user`.
- **Edge Cases & State Machine Tests**:
  - **EC-PAY-07 (Role Authorization Violation)**: Call as `hr_user` ➔ `403 Forbidden` (HR cannot approve their own generated payroll).
  - **EC-PAY-08 (Invalid Run ID)**: `runId=999999` ➔ `404 Not Found` ("Payroll Run not found with ID: 999999").
  - **EC-PAY-09 (Illegal State Transition - Already Approved)**: Re-calling approve on an already `FINANCE_APPROVED` run ➔ `400 Bad Request` ("Payroll must be in GENERATED state to approve. Current status: FINANCE_APPROVED").
  - **EC-PAY-10 (Illegal State Transition - Already Paid)**: Calling approve on a `PAID` run ➔ `400 Bad Request`.

#### 2.3 Step 3: Execute Payouts (`POST /api/payroll/{runId}/payout`)
- **Authorization**: `ROLE_FINANCE`, `ROLE_ADMIN`
- **Happy Path Test**:
  - As `finance_user`: `POST /api/payroll/1/payout`
  - Returns `200 OK` with:
    - `batchId`: e.g. `DEMO-BATCH-1-172970...`
    - `status`: `PAID`
    - `successfulTransfers`: 100
    - `totalAmountDisbursed`: Matching sum of net salaries
    - Creates `PaymentTransaction` records with unique `DEMO-UTR-...` bank reference codes.
    - Generates `Payslip` records for all employees.
- **Edge Cases & Concurrency Tests**:
  - **EC-PAY-11 (Execute Payout Before Approval)**: Calling `/payout` on a `GENERATED` draft run ➔ `400 Bad Request` ("Payroll must be FINANCE_APPROVED to execute payouts. Current status: GENERATED").
  - **EC-PAY-12 (Double Payout / Idempotency Check)**: Calling `/payout` a second time on the same run ➔ `400 Bad Request` ("Payroll must be FINANCE_APPROVED... Current status: PAID").
  - **EC-PAY-13 (Masked Bank Account Audit)**: Verify in the sample transactions response that bank account numbers are masked (`XXXX-XXXX-1234`), never exposed in plaintext.

#### 2.4 Payroll Queries & Payslips
- **`GET /api/payroll/runs`** (HR / Finance / Admin): Returns list of all runs.
- **`GET /api/payroll/{runId}`** (HR / Finance / Admin): Returns full run details and records.
- **`GET /api/payroll/{runId}/transactions`** (Finance / Admin): Returns all payment audit transactions with UTR numbers.
- **`GET /api/payroll/payslips/{recordId}`** (HR / Finance / Admin / Employee):
  - **EC-PAY-14 (Accessing Non-Existent Record)**: `recordId=999999` ➔ `404 Not Found`.
  - **EC-PAY-15 (Item Breakdown Integrity)**: Verify earnings (`BASIC`, `HRA`, `SPECIAL_ALLOWANCE`) and deductions (`PF`, `PROFESSIONAL_TAX`) match the calculated gross and net values.

---

### Module 3: Attendance Tracking (`AttendanceController`)

#### 3.1 Employee Check-In (`POST /api/attendance/check-in`)
- **Body**:
  ```json
  {
    "employeeId": 1,
    "attendanceDate": "2026-10-24",
    "checkInTime": "09:00:00",
    "remarks": "On-time arrival"
  }
  ```
#### 3.1 Employee Check-In (`POST /api/attendance/check-in`)
- **Body**:
  ```json
  {
    "employeeId": 1,
    "attendanceDate": "2026-10-23",
    "checkInTime": "09:00:00",
    "remarks": "On-time arrival"
  }
  ```
- **Happy Path Response**: Returns `201 Created` with dedicated clean `CheckInResponsePayload` (excludes `checkOut`, `workingHours`, `overtimeHours`):
  ```json
  {
    "attendanceId": 1,
    "employeeId": 1,
    "employeeName": "Aditi Sharma",
    "attendanceDate": "2026-10-23",
    "status": "PRESENT",
    "checkIn": "09:00:00",
    "remarks": "On-time arrival",
    "message": "Check-in recorded successfully"
  }
  ```
- **Edge Cases**:
  - **EC-ATT-01 (Block Previous / Future Date)**: 
    - `attendanceDate < today` (e.g. `2026-10-20`) ➔ `400 Bad Request` ("Check-in is not allowed for past dates.").
    - `attendanceDate > today` (e.g. `2026-11-01`) ➔ `400 Bad Request` ("Check-in is not allowed for future dates.").
  - **EC-ATT-02 (Duplicate Check-In on Same Date)**: Submitting check-in for employee 1 on the same date twice ➔ `400 Bad Request` ("Employee already checked in for date: ...").
  - **EC-ATT-03 (Invalid Employee ID Input)**:
    - `"employeeId": ""` or `"employeeId": "abc"` or `"employeeId": "!@#$"` ➔ `400 Bad Request` ("Invalid value ... for field 'employeeId'. Expected a valid Long.").
    - `"employeeId": -5` or `0` ➔ `400 Bad Request` ("Employee ID must be a positive number").
    - `"employeeId": 99999` (non-existent) ➔ `404 Not Found` ("Employee not found with ID: 99999").
  - **EC-ATT-04 (Omitted Date/Time Defaults)**: Omitting `attendanceDate` defaults to `LocalDate.now()`; omitting `checkInTime` defaults to `LocalTime.now()`.

#### 3.2 Employee Check-Out & Hour Calculation (`POST /api/attendance/check-out`)
- **Body**:
  ```json
  {
    "employeeId": 1,
    "attendanceDate": "2026-10-23",
    "checkOutTime": "17:30:00"
  }
  ```
- **Happy Path Test**: Returns `200 OK` with calculated `workingHours` and `overtimeHours`.
- **Business Logic & Edge Cases**:
  - **EC-ATT-05 (Block Previous / Future Date)**: Attempting check-out on a past or future date ➔ `400 Bad Request`.
  - **EC-ATT-06 (Prevent Multiple Check-Outs)**: Calling check-out when the employee has already checked out for that date ➔ `400 Bad Request` ("Employee has already checked out for date: ...").
  - **EC-ATT-07 (Check-Out Without Check-In)**: Checking out on a date with no check-in record ➔ `400 Bad Request` ("No check-in record found for date: ...").
  - **EC-ATT-08 (Check-Out Before Check-In Time)**: `checkInTime: "10:00:00"` and `checkOutTime: "09:00:00"` ➔ `400 Bad Request` ("Check-out time cannot be before check-in time").
  - **EC-ATT-09 (Half-Day Calculation)**: Work duration < 5.0 hours (e.g. 09:00 to 13:00 = 4.0 hrs) ➔ `status` becomes `HALF_DAY`, `workingHours = 4.00`, `overtimeHours = 0.00`.
  - **EC-ATT-10 (Standard Full-Day)**: Work duration = 8.0 hours (09:00 to 17:00) ➔ `status = PRESENT`, `workingHours = 8.00`, `overtimeHours = 0.00`.
  - **EC-ATT-11 (Overtime Calculation)**: Work duration = 10.5 hours (09:00 to 19:30) ➔ `workingHours = 8.00`, `overtimeHours = 2.50`.

#### 3.3 HR Manual Attendance Record (`POST /api/attendance/record`)
- **Authorization**: `ROLE_HR`, `ROLE_ADMIN`
- **Use Case**: Allows HR to manually record or adjust attendance for any date (including past dates, holidays, weekends, or unrecorded absences).
- **Body Example**:
  ```json
  {
    "employeeId": 1,
    "attendanceDate": "2026-10-15",
    "status": "HOLIDAY",
    "remarks": "Diwali Public Holiday"
  }
  ```
- **Edge Cases**:
  - **EC-ATT-12 (RBAC Enforcement)**: Call as `finance_user` or `emp0004` ➔ `403 Forbidden`.
  - **EC-ATT-13 (Manual Status Override)**: Setting `ABSENT`, `HOLIDAY`, `WEEKEND`, or `ON_LEAVE`.

#### 3.4 Monthly Attendance Summary & Status Definitions (`GET /api/attendance/summary/{employeeId}?year={year}&month={month}`)
- **Attendance Status Reference & Testing Guide**:
  1. **`PRESENT`**:
     - *Meaning*: Employee worked full day ($\ge 5.0$ hours).
     - *How to test*: Check-in at 09:00 and check-out at 17:00 (8.0 hours), or HR manual entry with `status: "PRESENT"`.
  2. **`HALF_DAY`**:
     - *Meaning*: Employee worked less than standard minimum (< 5.0 hours).
     - *How to test*: Check-in at 09:00 and check-out at 12:30 (3.5 hours). System automatically flags `HALF_DAY`.
  3. **`ON_LEAVE`**:
     - *Meaning*: Employee is on approved paid or unpaid leave.
     - *How to test*: Employee submits leave application (`POST /api/leaves/apply`), HR approves it (`PUT /api/leaves/review` with `newStatus: "APPROVED"`). Attendance records for all days in the leave range are automatically created with `status: "ON_LEAVE"`.
  4. **`ABSENT`**:
     - *Meaning*: Employee failed to check in without approved leave.
     - *How to test*: HR manually records attendance via `POST /api/attendance/record` with `status: "ABSENT"`, `workingHours: 0`, `overtimeHours: 0`.
  5. **`HOLIDAY`**:
     - *Meaning*: Recognized company or national public holiday.
     - *How to test*: HR submits `POST /api/attendance/record` with `status: "HOLIDAY"`, `remarks: "Public Holiday"`.
  6. **`WEEKEND`**:
     - *Meaning*: Scheduled weekend rest days (Saturday / Sunday).
     - *How to test*: HR submits `POST /api/attendance/record` with `status: "WEEKEND"`.
- **Summary Payload Output**:
  - `employeeId` & `employeeName`: Full name of the employee.
  - `presentDays`, `halfDays`, `absentDays`, `onLeaveDays`, `holidayDays`, `weekendDays`: Total counts of each status during that calendar month.
  - `totalWorkingHours`, `totalOvertimeHours`: Accurate sum of hours worked.
  - `records`: Complete itemized array of each individual date's punches.

---

### Module 4: Leaves & Leave Balances (`LeaveController`)

#### 4.1 Apply for Leave (`POST /api/leaves/apply`)
- **Body**:
  ```json
  {
    "employeeId": 1,
    "leaveType": "CASUAL",
    "startDate": "2026-11-02",
    "endDate": "2026-11-04",
    "reason": "Personal family event"
  }
  ```
- **Happy Path Test**: Returns `201 Created` with `status: "PENDING"`, `numberOfDays: 3`.
- **Complex Edge Cases & Policy Violations**:
  - **EC-LEV-01 (Block Past Dates)**: `startDate < today` (e.g. `startDate: "2025-11-02"` when today is `2026-10-23`) ➔ `400 Bad Request` ("Cannot apply for leave on past dates. Leave start date must be today or in the future.").
  - **EC-LEV-02 (Inverted Dates)**: `startDate: "2026-11-05"`, `endDate: "2026-11-02"` ➔ `400 Bad Request` ("Leave start date cannot be after end date.").
  - **EC-LEV-03 (Single-Day Leave)**: `startDate == endDate` ➔ Handled properly with `numberOfDays: 1`.
  - **EC-LEV-04 (Overlapping Active Leave Request)**: Applying for `2026-11-03` to `2026-11-06` while a request for `2026-11-02` to `2026-11-04` is `PENDING` or `APPROVED` ➔ `400 Bad Request` ("Employee already has an active (PENDING or APPROVED) leave in the requested date range.").
  - **EC-LEV-05 (Insufficient Leave Balance)**:
    - Employee has 2 days CASUAL remaining.
    - Employee applies for 3 days CASUAL.
    - ➔ `400 Bad Request` ("Insufficient leave balance for CASUAL. Available: 2 days, Requested: 3 days.").
  - **EC-LEV-06 (Unpaid Leave Exemption)**: Applying for 10 days `UNPAID` leave ➔ Bypasses balance check, succeeds with `201 Created`.
  - **EC-LEV-07 (Missing Reason)**: `reason: ""` or `null` ➔ `400 Bad Request` ("Reason is required").

#### 4.2 Review Leave Request (`PUT /api/leaves/review`)
- **Authorization**: `ROLE_HR`, `ROLE_ADMIN`
- **Body**:
  ```json
  {
    "leaveRequestId": 1,
    "newStatus": "APPROVED",
    "remarks": "Approved by HR"
  }
  ```
- **Happy Path Test**:
  - Returns `200 OK` with `status: "APPROVED"`.
  - Automatically deducts `usedLeaves` in `LeaveBalance`.
  - Automatically synchronizes attendance: creates/updates `Attendance` records for each date in range with `status = ON_LEAVE`.
- **Edge Cases**:
  - **EC-LEV-07 (Review Already Processed Leave)**: Trying to approve or reject a leave that is already `APPROVED`, `REJECTED`, or `CANCELLED` ➔ `400 Bad Request` ("Leave request cannot be reviewed. Current status is already: ...").
  - **EC-LEV-08 (Invalid Review Status)**: `newStatus: "PENDING"` or `"CANCELLED"` ➔ `400 Bad Request` ("Review status must be either APPROVED or REJECTED.").
  - **EC-LEV-09 (Balance Exhaustion at Approval Time)**: If balance was deducted by another approval in the interim ➔ `400 Bad Request` ("Cannot approve leave: insufficient balance remaining...").

#### 4.3 Cancel Leave (`POST /api/leaves/{id}/cancel`)
- **Happy Path Test**:
  - Cancel a `PENDING` leave ➔ `status = CANCELLED`.
  - Cancel an `APPROVED` leave ➔ `status = CANCELLED`, restores deducted leave balance (`usedLeaves -= days`), deletes or clears attendance records for those dates.
- **Edge Cases**:
  - **EC-LEV-10 (Cancelling Rejected or Cancelled Leave)**: ➔ `400 Bad Request` ("Leave request cannot be cancelled from current status...").
  - **EC-LEV-11 (Non-Existent Request ID)**: ➔ `404 Not Found`.

#### 4.4 Leave Balance Allocation (`POST /api/leaves/balance/allocate`)
- **Authorization**: `ROLE_HR`, `ROLE_ADMIN`
- **Body**:
  ```json
  {
    "employeeId": 1,
    "leaveType": "CASUAL",
    "totalLeaves": 18
  }
  ```
- **Edge Cases**:
  - **EC-LEV-12 (RBAC Violation)**: Calling as `finance_user` ➔ `403 Forbidden`.
  - **EC-LEV-13 (Upsert Logic)**: If balance record exists, updates `totalLeaves`; if not, creates record with `usedLeaves = 0`.

---

### Module 5: Employee Directory (`EmployeeController`)

#### Endpoints:
- `GET /api/employees` (List all 100 employees)
- `GET /api/employees/{id}`
- `GET /api/employees/search?email={email}`
- `POST /api/employees`
- `PUT /api/employees/{id}`
- `DELETE /api/employees/{id}`

#### Edge Cases & Integrity Tests:
- **EC-EMP-01 (Duplicate Email on Create)**: `POST /api/employees` with an existing email (e.g., `aditi.sharma1@example.com`) ➔ `400 Bad Request` ("Email already registered").
- **EC-EMP-02 (Search by Non-Existent Email)**: `search?email=notfound@example.com` ➔ `404 Not Found`.
- **EC-EMP-03 (Get by Non-Existent ID)**: `GET /api/employees/9999` ➔ `404 Not Found`.
- **EC-EMP-04 (Update Non-Existent Employee)**: `PUT /api/employees/9999` ➔ `404 Not Found`.
- **EC-EMP-05 (Delete Employee with Referential Integrity)**:
  - Deleting an employee who has linked records in `users`, `salary_structure`, `attendance`, or `payroll_records` without cascade handling ➔ Verifies database constraint protection.

---

### Module 6: Department Management (`DepartmentController`)

#### Endpoints:
- `GET /api/departments`
- `GET /api/departments/{id}`
- `GET /api/departments/search?name={name}`
- `POST /api/departments`
- `PUT /api/departments/{id}`
- `DELETE /api/departments/{id}`

#### Edge Cases:
- **EC-DEP-01 (Duplicate Department Name)**: `POST /api/departments` with `departmentName: "Engineering"` ➔ `400 Bad Request` ("Department name already exists").
- **EC-DEP-02 (Search Non-Existent Department)**: `search?name=SpaceExploration` ➔ `404 Not Found`.
- **EC-DEP-03 (Delete Active Department)**: Deleting a department currently assigned to employees ➔ Ensures foreign key constraint safety.

---

### Module 7: User Administration (`UserController`)

#### Endpoints:
- `GET /api/users`
- `GET /api/users/{id}`
- `GET /api/users/search?username={username}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PUT /api/users/{id}/lock` (Admin only)
- `PUT /api/users/{id}/unlock` (Admin only)
- `DELETE /api/users/{id}` (Admin only)

#### Sample Create User Payload (`POST /api/users`):
```json
{
  "username": "demo_hr_user",
  "password": "password123",
  "role": "HR",
  "status": "ACTIVE",
  "employee": {
    "employeeId": 10
  }
}
```
> [!NOTE]
> **Automatic Password Hashing**: The client supplies a plain-text `password`. `UserController` automatically hashes it with `BCryptPasswordEncoder` before saving to `password_hash` in PostgreSQL. Neither `password` nor `passwordHash` are ever returned in JSON responses.

#### Edge Cases:
- **EC-USR-01 (Duplicate Username)**: `POST /api/users` with `username: "admin"` ➔ `400 Bad Request` ("Username already taken").
- **EC-USR-02 (Employee Already Has User Account)**: Creating a user for `employeeId: 1` when employee 1 is already linked to user `admin` ➔ `400 Bad Request` ("This employee already has a user account").
- **EC-USR-03 (Auto-Hashing & Write-Only Security)**: Plain text `password` is hashed using BCrypt. When querying `GET /api/users` or `GET /api/users/1`, neither `password` nor `passwordHash` is ever leaked in JSON responses (`@JsonProperty(access = Access.WRITE_ONLY)`).
- **EC-USR-04 (Account Lock Enforcement - Admin Only)**: Attempting to lock an account via `PUT /api/users/{id}` with `status = LOCKED` or `PUT /api/users/{id}/lock` as `hr_user`, `finance_user`, or `emp0004` ➔ Returns **`403 Forbidden`** ("Only ADMIN users are authorized to lock user accounts.").
- **EC-USR-05 (Account Lock Prevents Login)**: Once locked by `admin`, any subsequent login attempt with that username ➔ Denied with **`401 Unauthorized`**. Unlocking via `PUT /api/users/{id}/unlock` restores access immediately.

---

## 4. Test Execution Matrix Checklist

Use this checklist during manual or automated Postman / Newman execution:

| Test ID | Module | Method & Endpoint | Role | Expected Status | Checked |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | Auth | `POST /login` (Valid Admin) | None | `200 OK` | [ ] |
| **TC-02** | Auth | `POST /login` (Bad Password) | None | `401 Unauthorized` | [ ] |
| **TC-03** | Auth | `POST /login` (Locked User) | None | `401 Unauthorized` | [ ] |
| **TC-04** | Auth | `POST /api/logout` | Authenticated | `200 OK` | [ ] |
| **TC-05** | Security | Any mutating call without `X-XSRF-TOKEN` | Authenticated | `403 Forbidden` | [ ] |
| **TC-06** | Payroll | `POST /api/payroll/generate?year=2026&month=10` | HR / Admin | `200 OK` (GENERATED) | [ ] |
| **TC-07** | Payroll | `POST /api/payroll/generate?year=2026&month=13` | HR / Admin | `400 Bad Request` | [ ] |
| **TC-08** | Payroll | `POST /api/payroll/generate` | Finance / Emp | `403 Forbidden` | [ ] |
| **TC-09** | Payroll | `POST /api/payroll/{id}/approve` | Finance / Admin | `200 OK` (FINANCE_APPROVED) | [ ] |
| **TC-10** | Payroll | `POST /api/payroll/{id}/approve` | HR | `403 Forbidden` | [ ] |
| **TC-11** | Payroll | `POST /api/payroll/{id}/approve` (Re-approve) | Finance / Admin | `400 Bad Request` | [ ] |
| **TC-12** | Payroll | `POST /api/payroll/{id}/payout` | Finance / Admin | `200 OK` (PAID) | [ ] |
| **TC-13** | Payroll | `POST /api/payroll/{id}/payout` (Before Approve) | Finance / Admin | `400 Bad Request` | [ ] |
| **TC-14** | Payroll | `GET /api/payroll/{id}/transactions` | Finance / Admin | `200 OK` | [ ] |
| **TC-15** | Payroll | `GET /api/payroll/payslips/{recId}` | Any Role | `200 OK` (Masked Account) | [ ] |
| **TC-16** | Attendance | `POST /api/attendance/check-in` | Any Role | `201 Created` | [ ] |
| **TC-17** | Attendance | `POST /api/attendance/check-in` (Duplicate Date) | Any Role | `400 Bad Request` | [ ] |
| **TC-18** | Attendance | `POST /api/attendance/check-out` (< 5 hrs) | Any Role | `200 OK` (HALF_DAY) | [ ] |
| **TC-19** | Attendance | `POST /api/attendance/check-out` (> 8 hrs) | Any Role | `200 OK` (Overtime calc) | [ ] |
| **TC-20** | Attendance | `POST /api/attendance/record` | HR / Admin | `200 OK` | [ ] |
| **TC-21** | Attendance | `POST /api/attendance/record` | Employee | `403 Forbidden` | [ ] |
| **TC-22** | Leaves | `POST /api/leaves/apply` (Valid) | Any Role | `201 Created` (PENDING) | [ ] |
| **TC-23** | Leaves | `POST /api/leaves/apply` (Inverted Dates) | Any Role | `400 Bad Request` | [ ] |
| **TC-24** | Leaves | `POST /api/leaves/apply` (Overlapping Dates) | Any Role | `400 Bad Request` | [ ] |
| **TC-25** | Leaves | `POST /api/leaves/apply` (Insufficient Bal) | Any Role | `400 Bad Request` | [ ] |
| **TC-26** | Leaves | `PUT /api/leaves/review` (Approve) | HR / Admin | `200 OK` (Deduct & Sync) | [ ] |
| **TC-27** | Leaves | `PUT /api/leaves/review` | Employee | `403 Forbidden` | [ ] |
| **TC-28** | Leaves | `POST /api/leaves/{id}/cancel` (Approved) | Any Role | `200 OK` (Revert Bal & Att) | [ ] |
| **TC-29** | Employees | `POST /api/employees` (Duplicate Email) | Any Role | `400 Bad Request` | [ ] |
| **TC-30** | Departments | `POST /api/departments` (Duplicate Name) | Any Role | `400 Bad Request` | [ ] |
| **TC-31** | Users | `GET /api/users` (Password hash leak test) | Authenticated | `200 OK` (Hash omitted) | [ ] |
