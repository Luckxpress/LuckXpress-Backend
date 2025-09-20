package com.luckxpress.web.exception;

import com.luckxpress.common.exception.*;
import com.luckxpress.core.mdc.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * CRITICAL: Centralized exception handling with proper error responses and logging
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid input data")
            .requestId(RequestContext.getRequestId())
            .validationErrors(errors)
            .build();
        
        log.warn("Validation error: {}", errors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle constraint violation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Invalid data constraints")
            .requestId(RequestContext.getRequestId())
            .validationErrors(errors)
            .build();
        
        log.warn("Constraint violation: {}", errors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle compliance exceptions
     */
    @ExceptionHandler(ComplianceException.class)
    public ResponseEntity<ErrorResponse> handleComplianceException(ComplianceException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .error("Compliance Violation")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .complianceType(ex.getComplianceType().name())
            .userId(ex.getUserId())
            .build();
        
        log.warn("Compliance violation: type={}, message={}, userId={}", 
                ex.getComplianceType(), ex.getMessage(), ex.getUserId());
        
        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }
    
    /**
     * Handle insufficient balance exceptions
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .error("Insufficient Balance")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .userId(ex.getUserId())
            .currencyType(ex.getCurrencyType().name())
            .requestedAmount(ex.getRequestedAmount())
            .availableBalance(ex.getAvailableBalance())
            .build();
        
        log.warn("Insufficient balance: userId={}, currency={}, requested={}, available={}", 
                ex.getUserId(), ex.getCurrencyType(), ex.getRequestedAmount(), ex.getAvailableBalance());
        
        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }
    
    /**
     * Handle dual approval required exceptions
     */
    @ExceptionHandler(DualApprovalRequiredException.class)
    public ResponseEntity<ErrorResponse> handleDualApprovalRequired(DualApprovalRequiredException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.ACCEPTED.value())
            .error("Dual Approval Required")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .operationType(ex.getOperationType())
            .amount(ex.getAmount())
            .initiatorId(ex.getInitiatorId())
            .build();
        
        log.info("Dual approval required: operation={}, amount={}, initiator={}", 
                ex.getOperationType(), ex.getAmount(), ex.getInitiatorId());
        
        return ResponseEntity.accepted().body(errorResponse);
    }
    
    /**
     * Handle idempotency exceptions
     */
    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyException(IdempotencyException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Duplicate Request")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .idempotencyKey(ex.getIdempotencyKey())
            .existingTransactionId(ex.getExistingTransactionId())
            .build();
        
        log.warn("Idempotency violation: key={}, existingTransaction={}", 
                ex.getIdempotencyKey(), ex.getExistingTransactionId());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Authentication Failed")
            .message("Invalid credentials or authentication required")
            .requestId(RequestContext.getRequestId())
            .build();
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("Insufficient permissions to access this resource")
            .requestId(RequestContext.getRequestId())
            .userId(RequestContext.getUserId())
            .build();
        
        log.warn("Access denied: userId={}, message={}", RequestContext.getUserId(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Argument")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .build();
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Invalid State")
            .message(ex.getMessage())
            .requestId(RequestContext.getRequestId())
            .build();
        
        log.warn("Invalid state: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .requestId(RequestContext.getRequestId())
            .path(request.getDescription(false))
            .build();
        
        log.error("Unexpected error: ", ex);
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
    
    /**
     * Error Response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String requestId;
        private String path;
        private String userId;
        private String complianceType;
        private String currencyType;
        private java.math.BigDecimal requestedAmount;
        private java.math.BigDecimal availableBalance;
        private String operationType;
        private java.math.BigDecimal amount;
        private String initiatorId;
        private String idempotencyKey;
        private String existingTransactionId;
        private Map<String, String> validationErrors;
    }
}
