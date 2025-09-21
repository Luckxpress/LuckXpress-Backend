package com.luckxpress.service.wallet;

import com.luckxpress.common.annotation.RequiresAudit;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.data.entity.Wallet;
import com.luckxpress.data.repository.WalletRepository;
import com.luckxpress.service.ledger.LedgerService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Advanced wallet operations service
 * CRITICAL: Handles complex wallet operations like locks and adjustments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletOperationsService {
    
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final RedissonClient redissonClient;
    
    private static final String WALLET_LOCK_PREFIX = "wallet:lock:";
    private static final long LOCK_WAIT_TIME = 5;
    private static final long LOCK_LEASE_TIME = 10;
    
    /**
     * Lock funds for active gameplay
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit(eventType = "FUNDS_LOCK")
    @Timed(value = "wallet.lock_funds", description = "Time taken to lock funds")
    public WalletTransaction lockFunds(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            String gameReference) {
        
        log.info("Locking {} funds for user: {}, amount: {}, game: {}",
            currency, userId, amount, gameReference);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lock amount must be positive");
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
            
            // Check if sufficient balance available
            if (currency == CurrencyType.GOLD) {
                if (!wallet.hasSufficientGoldBalance(amount)) {
                    throw new RuntimeException("Insufficient Gold balance to lock");
                }
                
                // Move from available to locked
                BigDecimal newBalance = MoneyUtil.subtract(wallet.getGoldBalance(), amount);
                BigDecimal newLocked = MoneyUtil.add(wallet.getGoldLocked(), amount);
                
                wallet.setGoldBalance(newBalance);
                wallet.setGoldLocked(newLocked);
            } else {
                if (!wallet.hasSufficientSweepsBalance(amount)) {
                    throw new RuntimeException("Insufficient Sweeps balance to lock");
                }
                
                // Move from available to locked
                BigDecimal newBalance = MoneyUtil.subtract(wallet.getSweepsBalance(), amount);
                BigDecimal newLocked = MoneyUtil.add(wallet.getSweepsLocked(), amount);
                
                wallet.setSweepsBalance(newBalance);
                wallet.setSweepsLocked(newLocked);
            }
            
            wallet = walletRepository.save(wallet);
            
            log.info("Funds locked successfully - User: {}, Currency: {}, Amount: {}, Game: {}",
                userId, currency, amount, gameReference);
            
            return WalletTransaction.builder()
                .transactionId("LOCK-" + System.currentTimeMillis())
                .userId(userId)
                .currency(currency)
                .amount(amount.negate()) // Negative because it's removing from available balance
                .reference(gameReference)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to lock funds for user: {}", userId, e);
            throw new RuntimeException("Failed to lock funds", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Unlock funds after gameplay completion
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit(eventType = "FUNDS_UNLOCK")
    @Timed(value = "wallet.unlock_funds", description = "Time taken to unlock funds")
    public WalletTransaction unlockFunds(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            String gameReference) {
        
        log.info("Unlocking {} funds for user: {}, amount: {}, game: {}",
            currency, userId, amount, gameReference);
        
        // Validate amount
        amount = MoneyUtil.normalize(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
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
            
            // Move from locked back to available
            if (currency == CurrencyType.GOLD) {
                BigDecimal currentLocked = wallet.getGoldLocked();
                if (currentLocked.compareTo(amount) < 0) {
                    throw new RuntimeException("Cannot unlock more Gold than is locked");
                }
                
                BigDecimal newBalance = MoneyUtil.add(wallet.getGoldBalance(), amount);
                BigDecimal newLocked = MoneyUtil.subtract(wallet.getGoldLocked(), amount);
                
                wallet.setGoldBalance(newBalance);
                wallet.setGoldLocked(newLocked);
            } else {
                BigDecimal currentLocked = wallet.getSweepsLocked();
                if (currentLocked.compareTo(amount) < 0) {
                    throw new RuntimeException("Cannot unlock more Sweeps than is locked");
                }
                
                BigDecimal newBalance = MoneyUtil.add(wallet.getSweepsBalance(), amount);
                BigDecimal newLocked = MoneyUtil.subtract(wallet.getSweepsLocked(), amount);
                
                wallet.setSweepsBalance(newBalance);
                wallet.setSweepsLocked(newLocked);
            }
            
            wallet = walletRepository.save(wallet);
            
            log.info("Funds unlocked successfully - User: {}, Currency: {}, Amount: {}, Game: {}",
                userId, currency, amount, gameReference);
            
            return WalletTransaction.builder()
                .transactionId("UNLOCK-" + System.currentTimeMillis())
                .userId(userId)
                .currency(currency)
                .amount(amount) // Positive because it's adding back to available balance
                .reference(gameReference)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to unlock funds for user: {}", userId, e);
            throw new RuntimeException("Failed to unlock funds", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Process game win - unlock bet amount and add winnings
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @RequiresAudit(eventType = "GAME_WIN")
    @Timed(value = "wallet.process_win", description = "Time taken to process win")
    public WalletTransaction processWin(
            String userId,
            CurrencyType currency,
            BigDecimal betAmount,
            BigDecimal winAmount,
            String gameReference) {
        
        log.info("Processing {} win for user: {}, bet: {}, win: {}, game: {}",
            currency, userId, betAmount, winAmount, gameReference);
        
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
            
            BigDecimal oldBalance;
            BigDecimal newBalance;
            
            if (currency == CurrencyType.GOLD) {
                oldBalance = wallet.getGoldBalance();
                // Unlock bet amount and add win amount
                newBalance = MoneyUtil.add(oldBalance, winAmount);
                
                wallet.setGoldBalance(newBalance);
                wallet.setTotalGoldWon(
                    MoneyUtil.add(wallet.getTotalGoldWon(), winAmount)
                );
                
                // Unlock the bet amount
                BigDecimal newLocked = MoneyUtil.subtract(wallet.getGoldLocked(), betAmount);
                wallet.setGoldLocked(newLocked);
            } else {
                oldBalance = wallet.getSweepsBalance();
                // Unlock bet amount and add win amount
                newBalance = MoneyUtil.add(oldBalance, winAmount);
                
                wallet.setSweepsBalance(newBalance);
                wallet.setTotalSweepsWon(
                    MoneyUtil.add(wallet.getTotalSweepsWon(), winAmount)
                );
                
                // Unlock the bet amount
                BigDecimal newLocked = MoneyUtil.subtract(wallet.getSweepsLocked(), betAmount);
                wallet.setSweepsLocked(newLocked);
            }
            
            wallet = walletRepository.save(wallet);
            
            // Record win in ledger
            String transactionId = ledgerService.recordWin(
                userId,
                currency,
                winAmount,
                oldBalance,
                newBalance,
                gameReference,
                String.format("Game win - Bet: %s, Win: %s", betAmount, winAmount)
            );
            
            log.info("Game win processed - User: {}, Currency: {}, Win Amount: {}, TxId: {}",
                userId, currency, winAmount, transactionId);
            
            return WalletTransaction.builder()
                .transactionId(transactionId)
                .userId(userId)
                .currency(currency)
                .amount(winAmount)
                .balanceBefore(oldBalance)
                .balanceAfter(newBalance)
                .reference(gameReference)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to process win for user: {}", userId, e);
            throw new RuntimeException("Failed to process win", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
