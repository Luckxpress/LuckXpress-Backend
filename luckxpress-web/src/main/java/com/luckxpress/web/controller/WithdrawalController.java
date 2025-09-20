package com.luckxpress.web.controller;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.service.WithdrawalService;
import com.luckxpress.web.dto.TransactionResponseDto;
import com.luckxpress.web.dto.WithdrawalEligibilityDto;
import com.luckxpress.web.dto.WithdrawalRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Withdrawal Controller
 * CRITICAL: Provides withdrawal processing REST endpoints with strict compliance
 */
@Slf4j
@RestController
@RequestMapping("/api/withdrawal")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Withdrawal Management", description = "Withdrawal processing with compliance validation")
public class WithdrawalController {
    
    private final WithdrawalService withdrawalService;
    
    /**
     * Check withdrawal eligibility
     */
    @GetMapping("/eligibility")
    @Operation(
        summary = "Check withdrawal eligibility",
        description = "Checks if user is eligible for withdrawal and returns limits"
    )
    @ApiResponse(responseCode = "200", description = "Eligibility check completed")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<WithdrawalEligibilityDto> checkWithdrawalEligibility(
            @Parameter(description = "Currency type (SWEEPS only)")
            @RequestParam CurrencyType currencyType,
            @Parameter(description = "Withdrawal amount")
            @RequestParam BigDecimal amount) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        log.debug("Checking withdrawal eligibility: userId={}, currency={}, amount={}", 
                currentUserId, currencyType, amount);
        
        WithdrawalService.WithdrawalEligibility eligibility = withdrawalService.checkWithdrawalEligibility(
            currentUserId, currencyType, amount
        );
        
        WithdrawalEligibilityDto response = WithdrawalEligibilityDto.fromEligibility(eligibility);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process withdrawal request
     */
    @PostMapping
    @Operation(
        summary = "Process withdrawal",
        description = "Processes a withdrawal request with comprehensive compliance validation"
    )
    @ApiResponse(responseCode = "201", description = "Withdrawal processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid withdrawal request or compliance violation")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "409", description = "Duplicate request")
    @ApiResponse(responseCode = "422", description = "Insufficient balance or eligibility issue")
    public ResponseEntity<TransactionResponseDto> processWithdrawal(
            @Valid @RequestBody WithdrawalRequestDto withdrawalRequest,
            HttpServletRequest request) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("Processing withdrawal request: userId={}, amount={}, currency={}, paymentMethod={}", 
                currentUserId, withdrawalRequest.getAmount(), withdrawalRequest.getCurrencyType(), 
                withdrawalRequest.getPaymentMethod());
        
        // Create withdrawal service request
        WithdrawalService.WithdrawalRequest serviceRequest = new WithdrawalService.WithdrawalRequest();
        serviceRequest.setUserId(currentUserId);
        serviceRequest.setCurrencyType(withdrawalRequest.getCurrencyType());
        serviceRequest.setAmount(withdrawalRequest.getAmount());
        serviceRequest.setPaymentMethod(withdrawalRequest.getPaymentMethod());
        serviceRequest.setIdempotencyKey(withdrawalRequest.getIdempotencyKey());
        serviceRequest.setIpAddress(ipAddress);
        serviceRequest.setUserAgent(userAgent);
        
        Transaction transaction = withdrawalService.processWithdrawal(serviceRequest);
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        log.info("Withdrawal request processed: transactionId={}, status={}, userId={}", 
                transaction.getId(), transaction.getStatus(), currentUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get withdrawal limits
     */
    @GetMapping("/limits")
    @Operation(
        summary = "Get withdrawal limits",
        description = "Retrieves current withdrawal limits for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Withdrawal limits retrieved")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<WithdrawalService.WithdrawalLimits> getWithdrawalLimits() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        // Check eligibility to get limits
        WithdrawalService.WithdrawalEligibility eligibility = withdrawalService.checkWithdrawalEligibility(
            currentUserId, CurrencyType.SWEEPS, BigDecimal.ONE
        );
        
        if (eligibility.getLimits() == null) {
            return ResponseEntity.ok(new WithdrawalService.WithdrawalLimits(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            ));
        }
        
        return ResponseEntity.ok(eligibility.getLimits());
    }
    
    /**
     * Get withdrawal requirements
     */
    @GetMapping("/requirements")
    @Operation(
        summary = "Get withdrawal requirements",
        description = "Retrieves withdrawal requirements and compliance information"
    )
    @ApiResponse(responseCode = "200", description = "Withdrawal requirements retrieved")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<WithdrawalRequirementsDto> getWithdrawalRequirements() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        // Check what's needed for withdrawal
        WithdrawalService.WithdrawalEligibility eligibility = withdrawalService.checkWithdrawalEligibility(
            currentUserId, CurrencyType.SWEEPS, BigDecimal.ONE
        );
        
        WithdrawalRequirementsDto requirements = new WithdrawalRequirementsDto();
        requirements.setEligible(eligibility.isEligible());
        requirements.setRequiresKyc(!eligibility.isEligible() && eligibility.getReason() != null && 
                                   eligibility.getReason().contains("KYC"));
        requirements.setOnlySweepsWithdrawable(true);
        requirements.setMinAmount(eligibility.getLimits() != null ? eligibility.getLimits().getMinAmount() : BigDecimal.ZERO);
        requirements.setMaxAmount(eligibility.getLimits() != null ? eligibility.getLimits().getMaxAmount() : BigDecimal.ZERO);
        requirements.setMessage(eligibility.getReason());
        
        return ResponseEntity.ok(requirements);
    }
    
    /**
     * Validate withdrawal amount
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate withdrawal amount",
        description = "Validates a withdrawal amount without processing"
    )
    @ApiResponse(responseCode = "200", description = "Validation completed")
    @ApiResponse(responseCode = "400", description = "Invalid amount")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<WithdrawalValidationDto> validateWithdrawal(
            @Parameter(description = "Currency type")
            @RequestParam CurrencyType currencyType,
            @Parameter(description = "Withdrawal amount")
            @RequestParam BigDecimal amount) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        WithdrawalService.WithdrawalEligibility eligibility = withdrawalService.checkWithdrawalEligibility(
            currentUserId, currencyType, amount
        );
        
        WithdrawalValidationDto validation = new WithdrawalValidationDto();
        validation.setValid(eligibility.isEligible());
        validation.setMessage(eligibility.getReason());
        validation.setAmount(amount);
        validation.setCurrencyType(currencyType);
        
        if (eligibility.getLimits() != null) {
            validation.setDailyRemaining(eligibility.getLimits().getDailyRemaining());
            validation.setMonthlyRemaining(eligibility.getLimits().getMonthlyRemaining());
        }
        
        return ResponseEntity.ok(validation);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Withdrawal Requirements DTO
     */
    public static class WithdrawalRequirementsDto {
        private boolean eligible;
        private boolean requiresKyc;
        private boolean onlySweepsWithdrawable;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String message;
        
        // Getters and setters
        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }
        
        public boolean isRequiresKyc() { return requiresKyc; }
        public void setRequiresKyc(boolean requiresKyc) { this.requiresKyc = requiresKyc; }
        
        public boolean isOnlySweepsWithdrawable() { return onlySweepsWithdrawable; }
        public void setOnlySweepsWithdrawable(boolean onlySweepsWithdrawable) { this.onlySweepsWithdrawable = onlySweepsWithdrawable; }
        
        public BigDecimal getMinAmount() { return minAmount; }
        public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
        
        public BigDecimal getMaxAmount() { return maxAmount; }
        public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * Withdrawal Validation DTO
     */
    public static class WithdrawalValidationDto {
        private boolean valid;
        private String message;
        private BigDecimal amount;
        private CurrencyType currencyType;
        private BigDecimal dailyRemaining;
        private BigDecimal monthlyRemaining;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public CurrencyType getCurrencyType() { return currencyType; }
        public void setCurrencyType(CurrencyType currencyType) { this.currencyType = currencyType; }
        
        public BigDecimal getDailyRemaining() { return dailyRemaining; }
        public void setDailyRemaining(BigDecimal dailyRemaining) { this.dailyRemaining = dailyRemaining; }
        
        public BigDecimal getMonthlyRemaining() { return monthlyRemaining; }
        public void setMonthlyRemaining(BigDecimal monthlyRemaining) { this.monthlyRemaining = monthlyRemaining; }
    }
}
