package com.luckxpress.service;

import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.common.exception.ComplianceException;
import com.luckxpress.common.exception.InsufficientBalanceException;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.ComplianceAudit;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.data.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Withdrawal Service
 * CRITICAL: Handles withdrawal processing with strict compliance validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalService {
    
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final KycService kycService;
    private final ComplianceService complianceService;
    private final PaymentIdempotencyService idempotencyService;
    private final AuditService auditService;
    
    /**
     * Process withdrawal request
     */
    @Transactional
    public Transaction processWithdrawal(WithdrawalRequest request) {
        log.info("Processing withdrawal request: userId={}, amount={}, currency={}", 
                request.getUserId(), request.getAmount(), request.getCurrencyType());
        
        // Validate idempotency
        if (idempotencyService.isDuplicateRequest(request.getIdempotencyKey())) {
            throw new IllegalArgumentException("Duplicate withdrawal request");
        }
        
        // Get user and validate
        User user = userService.findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        
        // Get account
        Account account = accountService.findByUserIdAndCurrency(request.getUserId(), request.getCurrencyType())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Comprehensive withdrawal validation
        validateWithdrawalEligibility(user, account, request);
        
        // Create withdrawal transaction
        Transaction withdrawal = transactionService.processWithdrawal(
            request.getUserId(),
            request.getCurrencyType(),
            request.getAmount(),
            request.getPaymentMethod(),
            request.getIdempotencyKey()
        );
        
        // Log withdrawal attempt
        auditService.logWithdrawalAttempted(user, withdrawal, request.getPaymentMethod());
        
        log.info("Withdrawal processed: transactionId={}, userId={}, amount={}", 
                withdrawal.getId(), request.getUserId(), request.getAmount());
        
        return withdrawal;
    }
    
    /**
     * Validate withdrawal eligibility with comprehensive compliance checks
     */
    private void validateWithdrawalEligibility(User user, Account account, WithdrawalRequest request) {
        log.debug("Validating withdrawal eligibility: userId={}, amount={}", user.getId(), request.getAmount());
        
        // 1. User Status Validation
        validateUserStatus(user);
        
        // 2. Currency Validation
        validateCurrencyWithdrawable(request.getCurrencyType());
        
        // 3. KYC Validation
        validateKycRequirements(user, request.getAmount());
        
        // 4. State Compliance Validation
        validateStateCompliance(user);
        
        // 5. Account Status Validation
        validateAccountStatus(account);
        
        // 6. Balance Validation
        validateSufficientBalance(account, request.getAmount());
        
        // 7. Amount Limits Validation
        validateWithdrawalLimits(user, request.getAmount());
        
        // 8. Frequency Validation
        validateWithdrawalFrequency(user);
        
        // 9. Suspicious Activity Check
        validateSuspiciousActivity(user, request);
        
        log.debug("Withdrawal eligibility validation passed: userId={}", user.getId());
    }
    
    /**
     * Validate user status for withdrawals
     */
    private void validateUserStatus(User user) {
        if (!user.isActive()) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.WITHDRAWAL_WITHOUT_KYC,
                ComplianceAudit.Severity.HIGH,
                "Withdrawal attempt by inactive user",
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.USER_STATUS_INVALID,
                "User account is not active",
                user.getId()
            );
        }
        
        if (user.isSelfExcluded()) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.SELF_EXCLUDED_USER_ACCESS,
                ComplianceAudit.Severity.HIGH,
                "Withdrawal attempt by self-excluded user",
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.SELF_EXCLUSION_VIOLATION,
                "User is self-excluded until: " + user.getSelfExclusionUntil(),
                user.getId()
            );
        }
        
        if (user.isAccountLocked()) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.ACCOUNT_LOCKED,
                "User account is locked until: " + user.getAccountLockedUntil(),
                user.getId()
            );
        }
    }
    
    /**
     * Validate currency is withdrawable
     */
    private void validateCurrencyWithdrawable(CurrencyType currencyType) {
        // Only Sweeps Coins are withdrawable per compliance
        if (!currencyType.isWithdrawable()) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.CURRENCY_NOT_WITHDRAWABLE,
                "Currency type " + currencyType + " is not withdrawable",
                null
            );
        }
    }
    
    /**
     * Validate KYC requirements for withdrawal
     */
    private void validateKycRequirements(User user, BigDecimal amount) {
        if (!user.isKycVerified()) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.WITHDRAWAL_WITHOUT_KYC,
                ComplianceAudit.Severity.HIGH,
                "Withdrawal attempt without KYC verification",
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.KYC_REQUIRED,
                "KYC verification required for withdrawals",
                user.getId()
            );
        }
        
        // Check if enhanced KYC is required for large amounts
        if (amount.compareTo(ComplianceConstants.ENHANCED_KYC_THRESHOLD) >= 0) {
            if (!kycService.hasValidEnhancedKyc(user)) {
                auditService.createComplianceAudit(
                    ComplianceAudit.EventType.KYC_VERIFICATION_FAILED,
                    ComplianceAudit.Severity.HIGH,
                    "Enhanced KYC required for large withdrawal: " + amount,
                    user
                );
                throw new ComplianceException(
                    ComplianceException.ComplianceType.ENHANCED_KYC_REQUIRED,
                    "Enhanced KYC verification required for withdrawals over " + ComplianceConstants.ENHANCED_KYC_THRESHOLD,
                    user.getId()
                );
            }
        }
    }
    
    /**
     * Validate state compliance for withdrawals
     */
    private void validateStateCompliance(User user) {
        try {
            complianceService.validateStateRestrictions(user.getStateCode(), user.getId());
        } catch (ComplianceException e) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.STATE_RESTRICTION_VIOLATION,
                ComplianceAudit.Severity.HIGH,
                "Withdrawal attempt from restricted state: " + user.getStateCode(),
                user
            );
            throw e;
        }
    }
    
    /**
     * Validate account status
     */
    private void validateAccountStatus(Account account) {
        if (!account.canWithdraw()) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.ACCOUNT_RESTRICTED,
                "Account is not eligible for withdrawals: " + account.getStatus(),
                account.getUser().getId()
            );
        }
        
        if (account.isFrozen()) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.ACCOUNT_FROZEN,
                "Account is frozen until: " + account.getFrozenUntil(),
                account.getUser().getId()
            );
        }
    }
    
    /**
     * Validate sufficient balance
     */
    private void validateSufficientBalance(Account account, BigDecimal amount) {
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        
        if (account.getAvailableBalance().compareTo(normalizedAmount) < 0) {
            throw new InsufficientBalanceException(
                account.getUser().getId(),
                account.getCurrencyType(),
                normalizedAmount,
                account.getAvailableBalance()
            );
        }
    }
    
    /**
     * Validate withdrawal amount limits
     */
    private void validateWithdrawalLimits(User user, BigDecimal amount) {
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        
        // Minimum withdrawal amount
        if (normalizedAmount.compareTo(ComplianceConstants.MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                "Withdrawal amount below minimum: " + ComplianceConstants.MIN_WITHDRAWAL_AMOUNT
            );
        }
        
        // Maximum withdrawal amount
        if (normalizedAmount.compareTo(ComplianceConstants.MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "Withdrawal amount exceeds maximum: " + ComplianceConstants.MAX_WITHDRAWAL_AMOUNT
            );
        }
        
        // Daily withdrawal limit
        BigDecimal dailyTotal = getDailyWithdrawalTotal(user.getId());
        BigDecimal newDailyTotal = MoneyUtil.add(dailyTotal, normalizedAmount);
        
        if (newDailyTotal.compareTo(ComplianceConstants.DAILY_WITHDRAWAL_LIMIT) > 0) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.DAILY_LIMIT_EXCEEDED,
                "Daily withdrawal limit exceeded. Current: " + dailyTotal + ", Limit: " + ComplianceConstants.DAILY_WITHDRAWAL_LIMIT,
                user.getId()
            );
        }
        
        // Monthly withdrawal limit
        BigDecimal monthlyTotal = getMonthlyWithdrawalTotal(user.getId());
        BigDecimal newMonthlyTotal = MoneyUtil.add(monthlyTotal, normalizedAmount);
        
        if (newMonthlyTotal.compareTo(ComplianceConstants.MONTHLY_WITHDRAWAL_LIMIT) > 0) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.MONTHLY_LIMIT_EXCEEDED,
                "Monthly withdrawal limit exceeded. Current: " + monthlyTotal + ", Limit: " + ComplianceConstants.MONTHLY_WITHDRAWAL_LIMIT,
                user.getId()
            );
        }
    }
    
    /**
     * Validate withdrawal frequency
     */
    private void validateWithdrawalFrequency(User user) {
        // Check for too many withdrawals in short period
        int recentWithdrawals = getRecentWithdrawalCount(user.getId(), 24); // Last 24 hours
        
        if (recentWithdrawals >= ComplianceConstants.MAX_DAILY_WITHDRAWALS) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.EXCESSIVE_WITHDRAWAL_AMOUNT,
                ComplianceAudit.Severity.MEDIUM,
                "Excessive withdrawal frequency: " + recentWithdrawals + " in 24 hours",
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.FREQUENCY_LIMIT_EXCEEDED,
                "Too many withdrawal attempts. Maximum " + ComplianceConstants.MAX_DAILY_WITHDRAWALS + " per day",
                user.getId()
            );
        }
    }
    
    /**
     * Validate for suspicious activity
     */
    private void validateSuspiciousActivity(User user, WithdrawalRequest request) {
        // Check for rapid sequence of transactions
        List<Transaction> recentTransactions = getRecentTransactions(user.getId(), 1); // Last hour
        
        if (recentTransactions.size() >= 5) {
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.RAPID_TRANSACTION_SEQUENCE,
                ComplianceAudit.Severity.HIGH,
                "Rapid transaction sequence detected: " + recentTransactions.size() + " in 1 hour",
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.SUSPICIOUS_ACTIVITY,
                "Suspicious transaction pattern detected",
                user.getId()
            );
        }
        
        // Check for new user with large withdrawal
        if (user.getCreatedAt().isAfter(Instant.now().minusSeconds(7 * 24 * 60 * 60)) && // 7 days
            request.getAmount().compareTo(new BigDecimal("500.0000")) >= 0) {
            
            auditService.createComplianceAudit(
                ComplianceAudit.EventType.SUSPICIOUS_TRANSACTION_PATTERN,
                ComplianceAudit.Severity.HIGH,
                "New user large withdrawal: " + request.getAmount(),
                user
            );
            throw new ComplianceException(
                ComplianceException.ComplianceType.SUSPICIOUS_ACTIVITY,
                "Large withdrawal by new user requires manual review",
                user.getId()
            );
        }
    }
    
    /**
     * Get daily withdrawal total for user
     */
    private BigDecimal getDailyWithdrawalTotal(String userId) {
        // This would query TransactionRepository for today's withdrawals
        return BigDecimal.ZERO; // Placeholder
    }
    
    /**
     * Get monthly withdrawal total for user
     */
    private BigDecimal getMonthlyWithdrawalTotal(String userId) {
        // This would query TransactionRepository for current month's withdrawals
        return BigDecimal.ZERO; // Placeholder
    }
    
    /**
     * Get recent withdrawal count
     */
    private int getRecentWithdrawalCount(String userId, int hours) {
        // This would query TransactionRepository for recent withdrawals
        return 0; // Placeholder
    }
    
    /**
     * Get recent transactions
     */
    private List<Transaction> getRecentTransactions(String userId, int hours) {
        // This would query TransactionRepository for recent transactions
        return List.of(); // Placeholder
    }
    
    /**
     * Check withdrawal eligibility without processing
     */
    public WithdrawalEligibility checkWithdrawalEligibility(String userId, CurrencyType currencyType, BigDecimal amount) {
        log.debug("Checking withdrawal eligibility: userId={}, currency={}, amount={}", userId, currencyType, amount);
        
        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            
            WithdrawalRequest request = new WithdrawalRequest();
            request.setUserId(userId);
            request.setCurrencyType(currencyType);
            request.setAmount(amount);
            
            validateWithdrawalEligibility(user, account, request);
            
            return new WithdrawalEligibility(true, null, calculateWithdrawalLimits(user));
            
        } catch (Exception e) {
            log.debug("Withdrawal eligibility check failed: userId={}, reason={}", userId, e.getMessage());
            return new WithdrawalEligibility(false, e.getMessage(), null);
        }
    }
    
    /**
     * Calculate withdrawal limits for user
     */
    private WithdrawalLimits calculateWithdrawalLimits(User user) {
        BigDecimal dailyUsed = getDailyWithdrawalTotal(user.getId());
        BigDecimal monthlyUsed = getMonthlyWithdrawalTotal(user.getId());
        
        BigDecimal dailyRemaining = MoneyUtil.subtract(ComplianceConstants.DAILY_WITHDRAWAL_LIMIT, dailyUsed);
        BigDecimal monthlyRemaining = MoneyUtil.subtract(ComplianceConstants.MONTHLY_WITHDRAWAL_LIMIT, monthlyUsed);
        
        return new WithdrawalLimits(
            ComplianceConstants.MIN_WITHDRAWAL_AMOUNT,
            ComplianceConstants.MAX_WITHDRAWAL_AMOUNT,
            ComplianceConstants.DAILY_WITHDRAWAL_LIMIT,
            dailyUsed,
            dailyRemaining,
            ComplianceConstants.MONTHLY_WITHDRAWAL_LIMIT,
            monthlyUsed,
            monthlyRemaining
        );
    }
    
    /**
     * Withdrawal Request DTO
     */
    public static class WithdrawalRequest {
        private String userId;
        private CurrencyType currencyType;
        private BigDecimal amount;
        private String paymentMethod;
        private String idempotencyKey;
        private String ipAddress;
        private String userAgent;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public CurrencyType getCurrencyType() { return currencyType; }
        public void setCurrencyType(CurrencyType currencyType) { this.currencyType = currencyType; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }
    
    /**
     * Withdrawal Eligibility DTO
     */
    public static class WithdrawalEligibility {
        private final boolean eligible;
        private final String reason;
        private final WithdrawalLimits limits;
        
        public WithdrawalEligibility(boolean eligible, String reason, WithdrawalLimits limits) {
            this.eligible = eligible;
            this.reason = reason;
            this.limits = limits;
        }
        
        public boolean isEligible() { return eligible; }
        public String getReason() { return reason; }
        public WithdrawalLimits getLimits() { return limits; }
    }
    
    /**
     * Withdrawal Limits DTO
     */
    public static class WithdrawalLimits {
        private final BigDecimal minAmount;
        private final BigDecimal maxAmount;
        private final BigDecimal dailyLimit;
        private final BigDecimal dailyUsed;
        private final BigDecimal dailyRemaining;
        private final BigDecimal monthlyLimit;
        private final BigDecimal monthlyUsed;
        private final BigDecimal monthlyRemaining;
        
        public WithdrawalLimits(BigDecimal minAmount, BigDecimal maxAmount, 
                              BigDecimal dailyLimit, BigDecimal dailyUsed, BigDecimal dailyRemaining,
                              BigDecimal monthlyLimit, BigDecimal monthlyUsed, BigDecimal monthlyRemaining) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.dailyLimit = dailyLimit;
            this.dailyUsed = dailyUsed;
            this.dailyRemaining = dailyRemaining;
            this.monthlyLimit = monthlyLimit;
            this.monthlyUsed = monthlyUsed;
            this.monthlyRemaining = monthlyRemaining;
        }
        
        // Getters
        public BigDecimal getMinAmount() { return minAmount; }
        public BigDecimal getMaxAmount() { return maxAmount; }
        public BigDecimal getDailyLimit() { return dailyLimit; }
        public BigDecimal getDailyUsed() { return dailyUsed; }
        public BigDecimal getDailyRemaining() { return dailyRemaining; }
        public BigDecimal getMonthlyLimit() { return monthlyLimit; }
        public BigDecimal getMonthlyUsed() { return monthlyUsed; }
        public BigDecimal getMonthlyRemaining() { return monthlyRemaining; }
    }
}
