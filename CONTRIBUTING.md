# Contributing Guidelines

Thank you for contributing to the Payroll Processing System.

Please follow these guidelines to keep the project consistent.

---

# Branch Strategy

Never work directly on the `main` branch.

Create a feature branch.

```bash
git checkout -b feature/<module-name>
```

Example

```bash
git checkout -b feature/payroll-processing
```

---

# Before Starting Work

Always pull the latest changes.

```bash
git pull origin main
```

---

# Commit Messages

Use meaningful commit messages.

Good examples

```text
Added PayrollRun entity

Implemented PayrollRecord service

Added PaymentStatus enum

Fixed PayrollItem relationship
```

Avoid

```text
Update

Done

Changes

Fixed stuff
```

---

# Expected Package Structure(May Change)

```
entity
repository
service
controller
dto
config
exception
enums
```

Do not create unnecessary packages.

---

# Entity Guidelines

- Use `@Getter` and `@Setter`
- Avoid `@Data` on entities
- Use `BigDecimal` for money
- Use `LocalDate` or `LocalDateTime`
- Use `FetchType.LAZY`
- Use `EnumType.STRING`
- Add validation annotations
- Add `precision` and `scale` for monetary values

Example

```java
@Column(nullable = false, precision = 12, scale = 2)
private BigDecimal amount;
```

---

# Enum Guidelines

All enums must be placed inside

```
enums
```

Do not use String values for statuses.

Example

```java
@Enumerated(EnumType.STRING)
private PaymentStatus paymentStatus;
```

---

# Naming Conventions

## Classes

```
PayrollRun
Employee
SalaryStructure
```

## Variables

```
grossSalary
employeeId
paymentStatus
```

## Tables

```
payroll_runs

salary_structures

leave_requests
```

---

# Relationships

Use bidirectional mappings only when required.

Prefer

- `@ManyToOne(fetch = FetchType.LAZY)`
- `@OneToMany(mappedBy = "...")`

---

# Code Formatting

- Use meaningful names.
- Remove unused imports.
- Keep methods small.
- Follow standard Java naming conventions.
- Do not commit commented-out code.

---

# Pull Requests

Before opening a Pull Request

- Project builds successfully
- No compilation errors
- Code follows project conventions
- Commit messages are meaningful

---

# Do Not

- Rename existing entities.
- Rename database tables.
- Change package names.
- Push directly to `main`.
- Hardcode configuration values.