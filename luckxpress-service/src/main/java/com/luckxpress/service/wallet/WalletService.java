package com.luckxpress.service.wallet;

import com.luckxpress.common.annotation.IdempotentOperation;
import com.luckxpress.common.annotation.RequiresAudit;
import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.exception.InsufficientBalanceException;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.data.entity.Wallet;
import com.luckxpress.data.repository.WalletRepository;
import com.luckxpress.service.ledger.LedgerService;
import com.luckxpress.service.compliance.ComplianceService;
import com.luckxpress.service.compliance.ComplianceResult;
import io.micrometer.core.annotation.Timed;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Wallet service for managing user balances
 * CRITICAL: All operations must be atomic and use distributed locking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final RedissonClient redissonClient;
    private final ComplianceService complianceService;
    
    private static final String WALLET_LOCK_PREFIX = "wallet:lock:";
    private static final long LOCK_WAIT_TIME = 5;
    private static final long LOCK_LEASE_TIME = 10;
    
    /**
     * Credit Gold coins (from purchase)
     * CRITICAL: Gold is NEVER withdrawable
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @IdempotentOperation(ttl = 24, unit = TimeUnit.HOURS)
    @RequiresAudit
    @Timed(value = "wallet.credit.gold", description = "Time taken to credit gold")
    public WalletTransaction creditGold(
            String userId,
            BigDecimal amount,
            String paymentReference,
            String idempotencyKey) {
        
        log.info("Processing Gold credit for user: {}, amount: {}, idempotencyKey: {}",
            userId, amount, idempotencyKey);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        if (amount.compareTo(ComplianceConstants.MIN_DEPOSIT_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                "Minimum deposit amount is " + ComplianceConstants.MIN_DEPOSIT_AMOUNT
            );
        }
        
        // Acquire distributed lock
        String lockKey = WALLET_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire wallet lock for user: " + userId);
            }
            
            // Get or create wallet
            Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
            
            // Calculate new balance
            BigDecimal oldBalance = wallet.getGoldBalance();
            BigDecimal newBalance = MoneyUtil.add(oldBalance, amount);
            
            // Update wallet
            wallet.setGoldBalance(newBalance);
            wallet = walletRepository.save(wallet);
            
            // Record in ledger
            String transactionId = ledgerService.recordDeposit(
                userId,
                CurrencyType.GOLD,
                amount,
                oldBalance,
                newBalance,
                paymentReference,
                "Gold coin purchase"
            );
            
            // Track metrics
            trackTransaction(CurrencyType.GOLD, amount, "deposit");
            
            log.info("Gold credit successful - User: {}, Amount: {}, New Balance: {}, TxId: {}",
                userId, amount, newBalance, transactionId);
            
            return WalletTransaction.builder()
                .transactionId(transactionId)
                .userId(userId)
                .currency(CurrencyType.GOLD)
                .amount(amount)
                .balanceBefore(oldBalance)
                .balanceAfter(newBalance)
                .reference(paymentReference)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to credit Gold for user: {}", userId, e);
            Sentry.captureException(e, scope -> {
                scope.setTag("operation", "credit_gold");
                scope.setTag("user_id", userId);
                scope.setLevel(SentryLevel.ERROR);
            });
            throw new RuntimeException("Failed to process Gold credit", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Credit Sweeps coins (promotional only)
     * CRITICAL: Sweeps can be withdrawn after KYC
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit
    @Timed(value = "wallet.credit.sweeps", description = "Time taken to credit sweeps")
    public WalletTransaction creditSweeps(
            String userId,
            BigDecimal amount,
            String promotionReference,
            String reason) {
        
        log.info("Processing Sweeps credit for user: {}, amount: {}, reason: {}",
            userId, amount, reason);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        
        // Acquire distributed lock
        String lockKey = WALLET_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire wallet lock for user: " + userId);
            }
            
            // Get or create wallet
            Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
            
            // Calculate new balance
            BigDecimal oldBalance = wallet.getSweepsBalance();
            BigDecimal newBalance = MoneyUtil.add(oldBalance, amount);
            
            // Update wallet
            wallet.setSweepsBalance(newBalance);
            wallet = walletRepository.save(wallet);
            
            // Record in ledger
            String transactionId = ledgerService.recordBonus(
                userId,
                CurrencyType.SWEEPS,
                amount,
                oldBalance,
                newBalance,
                promotionReference,
                reason
            );
            
            // Clear cache
            evictWalletCache(userId);
            
            // Track metrics
            trackTransaction(CurrencyType.SWEEPS, amount, "bonus");
            
            log.info("Sweeps credit successful - User: {}, Amount: {}, New Balance: {}, TxId: {}",
                userId, amount, newBalance, transactionId);
            
            return WalletTransaction.builder()
                .transactionId(transactionId)
                .userId(userId)
                .currency(CurrencyType.SWEEPS)
                .amount(amount)
                .balanceBefore(oldBalance)
                .balanceAfter(newBalance)
                .reference(promotionReference)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to credit Sweeps for user: {}", userId, e);
            Sentry.captureException(e, scope -> {
                scope.setTag("operation", "credit_sweeps");
                scope.setTag("user_id", userId);
                scope.setLevel(SentryLevel.ERROR);
            });
            throw new RuntimeException("Failed to process Sweeps credit", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Debit currency for gameplay
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit
    @Timed(value = "wallet.debit", description = "Time taken to debit wallet")
    public WalletTransaction debit(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            String gameReference,
            String description) {
        
        log.info("Processing {} debit for user: {}, amount: {}",
            currency, userId, amount);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        
        // Acquire distributed lock
        String lockKey = WALLET_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire wallet lock for user: " + userId);
            }
            
            // Get wallet
            Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for user: " + userId));
            
            // Check balance based on currency
            BigDecimal currentBalance;
            if (currency == CurrencyType.GOLD) {
                currentBalance = wallet.getGoldBalance();
                if (!wallet.hasSufficientGoldBalance(amount)) {
                    throw new InsufficientBalanceException(userId, currency, amount, currentBalance);
                }
            } else {
                currentBalance = wallet.getSweepsBalance();
                if (!wallet.hasSufficientSweepsBalance(amount)) {
                    throw new InsufficientBalanceException(userId, currency, amount, currentBalance);
                }
            }
            
            // Calculate new balance
            BigDecimal newBalance = MoneyUtil.subtract(currentBalance, amount);
            
            // Update wallet
            if (currency == CurrencyType.GOLD) {
                wallet.setGoldBalance(newBalance);
                wallet.setTotalGoldWagered(
                    MoneyUtil.add(wallet.getTotalGoldWagered(), amount)
                );
            } else {
                wallet.setSweepsBalance(newBalance);
                wallet.setTotalSweepsWagered(
                    MoneyUtil.add(wallet.getTotalSweepsWagered(), amount)
                );
            }
            wallet = walletRepository.save(wallet);
            
            // Record in ledger
            String transactionId = ledgerService.recordBet(
                userId,
                currency,
                amount,
                currentBalance,
                newBalance,
                gameReference,
                description
            );
            
            // Clear cache
            evictWalletCache(userId);
            
            log.info("{} debit successful - User: {}, Amount: {}, New Balance: {}, TxId: {}",
                currency, userId, amount, newBalance, transactionId);
            
            return WalletTransaction.builder()
                .transactionId(transactionId)
                .userId(userId)
                .currency(currency)
                .amount(amount.negate())
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .reference(gameReference)
                .build();
                
        } catch (InsufficientBalanceException e) {
            log.warn("Insufficient balance for user: {}, currency: {}, amount: {}",
                userId, currency, amount);
            throw e;
        } catch (Exception e) {
            log.error("Failed to debit {} for user: {}", currency, userId, e);
            Sentry.captureException(e, scope -> {
                scope.setTag("operation", "debit");
                scope.setTag("user_id", userId);
                scope.setTag("currency", currency.getCode());
                scope.setLevel(SentryLevel.ERROR);
            });
            throw new RuntimeException("Failed to process debit", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Get wallet balances (cached)
     */
    @Cacheable(value = "wallets", key = "#userId")
    @Transactional(readOnly = true)
    public WalletBalance getBalance(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> createWallet(userId));
        
        return WalletBalance.builder()
            .userId(userId)
            .goldBalance(wallet.getGoldBalance())
            .sweepsBalance(wallet.getSweepsBalance())
            .goldLocked(wallet.getGoldLocked())
            .sweepsLocked(wallet.getSweepsLocked())
            .availableGold(wallet.getAvailableGoldBalance())
            .availableSweeps(wallet.getAvailableSweepsBalance())
            .build();
    }
    
    @CacheEvict(value = "wallets", key = "#userId")
    public void evictWalletCache(String userId) {
        log.debug("Evicting wallet cache for user: {}", userId);
    }
    
    private Wallet createWallet(String userId) {
        Wallet wallet = Wallet.builder()
            .userId(userId)
            .goldBalance(BigDecimal.ZERO)
            .sweepsBalance(BigDecimal.ZERO)
            .goldLocked(BigDecimal.ZERO)
            .sweepsLocked(BigDecimal.ZERO)
            .totalGoldWagered(BigDecimal.ZERO)
            .totalSweepsWagered(BigDecimal.ZERO)
            .totalGoldWon(BigDecimal.ZERO)
            .totalSweepsWon(BigDecimal.ZERO)
            .build();
        
        return walletRepository.save(wallet);
    }
    
    private void trackTransaction(CurrencyType currency, BigDecimal amount, String type) {
        // Track in Sentry for monitoring
        Sentry.addBreadcrumb(
            String.format("Wallet transaction: %s %s %s", 
                type, amount, currency.getCode()),
            "transaction"
        );
    }
}
