package com.example.PayrollProcessingSystem.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Resource Not Found (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // 2. Handle Bad Requests (400)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        log.warn("Bad request [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 3. Handle Business Payroll Processing Errors (422 - Unprocessable Content)
    @ExceptionHandler(PayrollProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePayrollProcessingException(
            PayrollProcessingException ex, HttpServletRequest request) {

        log.warn("Payroll processing rejected [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_CONTENT);
    }


    // 4. Handle DTO Validation Errors (@Valid failure on @NotBlank, @NotNull, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for request: {}", errors);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed for one or more fields",
                LocalDateTime.now(),
                errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 5. Handle JSON parsing and malformed input (e.g. empty strings for enums, bad types)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {

        String userFriendlyMessage = "Invalid input format in request body.";
        Throwable cause = ex.getCause();

        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .findFirst().orElse("field");
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowed = java.util.Arrays.toString(targetType.getEnumConstants());
                userFriendlyMessage = String.format("Invalid value '%s' for enum field '%s'. Allowed values are: %s. (Omit the field if you don't wish to change it).",
                        ife.getValue(), field, allowed);
            } else {
                String typeName = targetType != null ? targetType.getSimpleName() : "valid format";
                userFriendlyMessage = String.format("Invalid value '%s' for field '%s'. Expected a valid %s.",
                        ife.getValue(), field, typeName);
            }
        } else if (cause instanceof com.fasterxml.jackson.databind.JsonMappingException jme) {
            String field = jme.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .findFirst().orElse("field");
            userFriendlyMessage = String.format("Invalid or malformed value for field '%s'.", field);
        } else if (ex.getMessage() != null && ex.getMessage().contains("Cannot coerce empty String")) {
            userFriendlyMessage = "Empty string \"\" is not allowed for enum or numeric fields. Please omit the field or supply a valid value.";
        }

        log.warn("Malformed HTTP request body [{} {}]: {}", request.getMethod(), request.getRequestURI(), userFriendlyMessage);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                userFriendlyMessage,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 6. Handle PathVariable / Query Param type mismatches (e.g. /summary/abc or /summary/!@#)
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid format";
        String message = String.format("Parameter '%s' received invalid value '%s'. Expected type is %s.",
                paramName, ex.getValue(), expectedType);

        log.warn("Parameter mismatch [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 7. Handle Access Denied / RBAC Authorization Failures (403 Forbidden)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank() && !ex.getMessage().equalsIgnoreCase("Access Denied"))
                ? ex.getMessage()
                : "Access Denied: You do not have the required permissions to perform this action.";

        log.warn("Access denied [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // 8. Handle Database Constraint / Unique Key Violations (400 Bad Request)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Database constraint violation: A record with this unique field (such as username, email, or employee reference) already exists.";
        if (ex.getMessage() != null && ex.getMessage().contains("uk_user_username")) {
            message = "Username already exists. Usernames must be unique across all accounts.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uk_user_employee")) {
            message = "This employee already has a linked user account.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uk_employee_email")) {
            message = "Email address is already registered.";
        }

        log.warn("Data integrity violation [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 9. Fallback for unhandled internal server exceptions (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled internal server exception [{} {}]", request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal server error occurred: " + ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}