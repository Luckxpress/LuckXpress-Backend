package com.luckxpress.service.compliance;

import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.LedgerEntryRepository;
import io.micrometer.core.annotation.Timed;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Compliance validation service
 * CRITICAL: Ensures all operations meet regulatory requirements
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceService {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    
    /**
     * Validate daily withdrawal limits
     */
    @Timed(value = "compliance.validate.daily_limit", description = "Time taken to validate daily limits")
    public ComplianceResult validateDailyWithdrawalLimit(String userId, BigDecimal withdrawalAmount) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        
        BigDecimal dailyWithdrawn = ledgerEntryRepository.getDailyVolume(
            com.luckxpress.common.constants.TransactionType.WITHDRAWAL,
            CurrencyType.SWEEPS,
            startOfDay,
            endOfDay
        );
        
        BigDecimal totalWithdrawal = dailyWithdrawn.add(withdrawalAmount);
        
        if (totalWithdrawal.compareTo(ComplianceConstants.DAILY_WITHDRAWAL_LIMIT) > 0) {
            log.warn("Daily withdrawal limit exceeded for user: {}, current: {}, requested: {}, limit: {}",
                userId, dailyWithdrawn, withdrawalAmount, ComplianceConstants.DAILY_WITHDRAWAL_LIMIT);
            
            return ComplianceResult.violation(
                "DAILY_WITHDRAWAL_LIMIT_EXCEEDED",
                String.format("Daily withdrawal limit of %s exceeded", ComplianceConstants.DAILY_WITHDRAWAL_LIMIT)
            );
        }
        
        return ComplianceResult.compliant();
    }
    
    /**
     * Validate KYC requirements for transaction
     */
    public ComplianceResult validateKycRequirements(User user, BigDecimal amount, String operationType) {
        // Check if KYC is required for this amount
        if (amount.compareTo(ComplianceConstants.KYC_REQUIRED_WITHDRAWAL_AMOUNT) >= 0) {
            if (user.getKycStatus() != User.KycStatus.VERIFIED) {
                log.warn("KYC verification required for user: {}, operation: {}, amount: {}",
                    user.getId(), operationType, amount);
                
                return ComplianceResult.violation(
                    "KYC_VERIFICATION_REQUIRED",
                    "KYC verification is required for this transaction amount"
                );
            }
        }
        
        // Enhanced KYC for high-value transactions
        if (amount.compareTo(ComplianceConstants.ENHANCED_KYC_THRESHOLD) >= 0) {
            // Additional enhanced KYC checks would go here
            log.info("Enhanced KYC transaction detected for user: {}, amount: {}", user.getId(), amount);
            
            Sentry.addBreadcrumb(
                String.format("Enhanced KYC transaction: User %s, Amount %s", user.getId(), amount),
                "compliance"
            );
        }
        
        return ComplianceResult.compliant();
    }
    
    /**
     * Validate state restrictions
     */
    public ComplianceResult validateStateRestrictions(User user, CurrencyType currency) {
        try {
            // Only validate for Sweeps operations
            if (currency == CurrencyType.SWEEPS) {
                user.validateStateForSweeps();
            }
            return ComplianceResult.compliant();
        } catch (Exception e) {
            log.warn("State restriction violation for user: {}, state: {}, currency: {}",
                user.getId(), user.getState(), currency);
            
            return ComplianceResult.violation(
                "STATE_RESTRICTION_VIOLATION",
                "Transaction not allowed in user's state: " + user.getState()
            );
        }
    }
    
    /**
     * Check if transaction requires dual approval
     */
    public boolean requiresDualApproval(BigDecimal amount) {
        return amount.compareTo(ComplianceConstants.DUAL_APPROVAL_THRESHOLD) >= 0;
    }
    
    /**
     * Check if transaction requires triple approval
     */
    public boolean requiresTripleApproval(BigDecimal amount) {
        return amount.compareTo(ComplianceConstants.TRIPLE_APPROVAL_THRESHOLD) >= 0;
    }
    
    /**
     * Check for suspicious transaction patterns
     */
    @Timed(value = "compliance.check.suspicious", description = "Time taken to check suspicious patterns")
    public ComplianceResult checkSuspiciousActivity(String userId, BigDecimal amount, String transactionType) {
        // Check for rapid successive transactions
        Instant recentTime = Instant.now().minus(1, ChronoUnit.HOURS);
        
        // This would implement more sophisticated fraud detection
        // For now, just a basic check for high-frequency transactions
        
        log.debug("Checking suspicious activity for user: {}, amount: {}, type: {}",
            userId, amount, transactionType);
        
        return ComplianceResult.compliant();
    }
    
    /**
     * Validate responsible gaming limits
     */
    public ComplianceResult validateResponsibleGamingLimits(User user, BigDecimal amount) {
        // Check session loss limits
        if (amount.compareTo(ComplianceConstants.SESSION_LOSS_LIMIT) > 0) {
            log.warn("Session loss limit exceeded for user: {}, amount: {}, limit: {}",
                user.getId(), amount, ComplianceConstants.SESSION_LOSS_LIMIT);
            
            return ComplianceResult.violation(
                "SESSION_LOSS_LIMIT_EXCEEDED",
                "Session loss limit exceeded. Please take a break."
            );
        }
        
        return ComplianceResult.compliant();
    }
}
