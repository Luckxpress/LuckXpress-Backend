package com.luckxpress.service.wallet;

import com.luckxpress.common.annotation.RequiresAudit;
import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.exception.InsufficientBalanceException;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.entity.Wallet;
import com.luckxpress.data.repository.UserRepository;
import com.luckxpress.data.repository.WalletRepository;
import com.luckxpress.service.ledger.LedgerService;
import io.micrometer.core.annotation.Timed;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Withdrawal processing service
 * CRITICAL: Only Sweeps can be withdrawn and only after KYC verification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {
    
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final RedissonClient redissonClient;
    
    private static final String WITHDRAWAL_LOCK_PREFIX = "withdrawal:lock:";
    private static final long LOCK_WAIT_TIME = 10;
    private static final long LOCK_LEASE_TIME = 30;
    
    /**
     * Process Sweeps withdrawal
     * CRITICAL: Only Sweeps are withdrawable, only after KYC
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit(eventType = "SWEEPS_WITHDRAWAL")
    @Timed(value = "withdrawal.process", description = "Time taken to process withdrawal")
    public WithdrawalResult processSweepsWithdrawal(
            String userId,
            BigDecimal amount,
            String paymentMethod,
            String bankAccountId) {
        
        log.info("Processing Sweeps withdrawal for user: {}, amount: {}", userId, amount);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        validateWithdrawalAmount(amount);
        
        // Only Sweeps can be withdrawn
        CurrencyType currency = CurrencyType.SWEEPS;
        currency.validateWithdrawable();
        
        // Acquire distributed lock
        String lockKey = WITHDRAWAL_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire withdrawal lock for user: " + userId);
            }
            
            // Get user and validate eligibility
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
            
            validateWithdrawalEligibility(user, amount);
            
            // Get wallet and check balance
            Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for user: " + userId));
            
            if (!wallet.hasSufficientSweepsBalance(amount)) {
                throw new InsufficientBalanceException(userId, currency, amount, wallet.getSweepsBalance());
            }
            
            // Calculate new balance
            BigDecimal currentBalance = wallet.getSweepsBalance();
            BigDecimal newBalance = MoneyUtil.subtract(currentBalance, amount);
            
            // Update wallet
            wallet.setSweepsBalance(newBalance);
            wallet = walletRepository.save(wallet);
            
            // Record in ledger
            String withdrawalReference = generateWithdrawalReference();
            String transactionId = ledgerService.recordWithdrawal(
                userId,
                currency,
                amount,
                currentBalance,
                newBalance,
                withdrawalReference,
                String.format("Sweeps withdrawal via %s", paymentMethod)
            );
            
            // Track compliance metrics
            trackWithdrawalCompliance(userId, amount);
            
            log.info("Sweeps withdrawal successful - User: {}, Amount: {}, New Balance: {}, Ref: {}",
                userId, amount, newBalance, withdrawalReference);
            
            return WithdrawalResult.builder()
                .withdrawalId(withdrawalReference)
                .transactionId(transactionId)
                .userId(userId)
                .currency(currency)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .paymentMethod(paymentMethod)
                .status(WithdrawalStatus.PENDING)
                .requestedAt(Instant.now())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to process Sweeps withdrawal for user: {}", userId, e);
            Sentry.captureException(e, scope -> {
                scope.setTag("operation", "sweeps_withdrawal");
                scope.setTag("user_id", userId);
                scope.setLevel(SentryLevel.ERROR);
            });
            throw new RuntimeException("Failed to process withdrawal", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Validate withdrawal amount against compliance limits
     */
    private void validateWithdrawalAmount(BigDecimal amount) {
        if (amount.compareTo(ComplianceConstants.MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                "Minimum withdrawal amount is " + ComplianceConstants.MIN_WITHDRAWAL_AMOUNT
            );
        }
        
        if (amount.compareTo(ComplianceConstants.MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "Maximum withdrawal amount is " + ComplianceConstants.MAX_WITHDRAWAL_AMOUNT
            );
        }
    }
    
    /**
     * Validate user eligibility for withdrawal
     */
    private void validateWithdrawalEligibility(User user, BigDecimal amount) {
        // Must be KYC verified
        if (!user.canWithdraw()) {
            throw new IllegalStateException("User is not eligible for withdrawal. KYC verification required.");
        }
        
        // Check if account is locked
        if (user.getIsLocked()) {
            throw new IllegalStateException("Account is locked and cannot withdraw");
        }
        
        // Check self-exclusion
        if (user.getIsSelfExcluded() && 
            user.getSelfExclusionUntil() != null && 
            user.getSelfExclusionUntil().isAfter(Instant.now())) {
            throw new IllegalStateException("Account is self-excluded until: " + user.getSelfExclusionUntil());
        }
        
        // Validate state for Sweeps operations
        user.validateStateForSweeps();
        
        // Check W2G reporting threshold
        if (amount.compareTo(ComplianceConstants.W2G_REPORTING_THRESHOLD) >= 0) {
            log.info("W2G reporting required for withdrawal - User: {}, Amount: {}", user.getId(), amount);
            // This would trigger W2G form generation in a real implementation
        }
    }
    
    /**
     * Generate unique withdrawal reference
     */
    private String generateWithdrawalReference() {
        return "WD-" + System.currentTimeMillis() + "-" + 
               String.format("%04d", (int)(Math.random() * 10000));
    }
    
    /**
     * Track withdrawal for compliance monitoring
     */
    private void trackWithdrawalCompliance(String userId, BigDecimal amount) {
        Sentry.addBreadcrumb(
            String.format("Sweeps withdrawal: User %s, Amount %s", userId, amount),
            "compliance"
        );
        
        // Additional compliance tracking would go here
        // e.g., AML monitoring, suspicious activity detection
    }
}
