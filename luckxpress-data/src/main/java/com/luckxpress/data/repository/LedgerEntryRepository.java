package com.luckxpress.data.repository;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.data.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Ledger entry repository for financial audit trail
 * CRITICAL: Immutable financial records
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    
    /**
     * Find entries by user ID
     */
    Page<LedgerEntry> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * Find entries by user and currency
     */
    Page<LedgerEntry> findByUserIdAndCurrencyOrderByCreatedAtDesc(
        String userId, 
        CurrencyType currency, 
        Pageable pageable
    );
    
    /**
     * Find entries by transaction type
     */
    List<LedgerEntry> findByTransactionTypeAndCreatedAtBetween(
        TransactionType transactionType,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Find entries by reference ID
     */
    Optional<LedgerEntry> findByReferenceId(String referenceId);
    
    /**
     * Get user's total deposits
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.userId = :userId AND l.transactionType = 'DEPOSIT' AND l.currency = :currency")
    BigDecimal getTotalDeposits(@Param("userId") String userId, @Param("currency") CurrencyType currency);
    
    /**
     * Get user's total withdrawals
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.userId = :userId AND l.transactionType = 'WITHDRAWAL' AND l.currency = :currency")
    BigDecimal getTotalWithdrawals(@Param("userId") String userId, @Param("currency") CurrencyType currency);
    
    /**
     * Get user's total wagered amount
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.userId = :userId AND l.transactionType = 'BET' AND l.currency = :currency")
    BigDecimal getTotalWagered(@Param("userId") String userId, @Param("currency") CurrencyType currency);
    
    /**
     * Get user's total winnings
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.userId = :userId AND l.transactionType = 'WIN' AND l.currency = :currency")
    BigDecimal getTotalWinnings(@Param("userId") String userId, @Param("currency") CurrencyType currency);
    
    /**
     * Find high-value transactions for compliance monitoring
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.amount >= :threshold AND l.createdAt >= :since ORDER BY l.amount DESC")
    List<LedgerEntry> findHighValueTransactions(
        @Param("threshold") BigDecimal threshold,
        @Param("since") Instant since
    );
    
    /**
     * Find suspicious transaction patterns
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.userId = :userId AND l.transactionType = :type AND l.createdAt >= :since AND l.amount >= :minAmount")
    List<LedgerEntry> findTransactionPattern(
        @Param("userId") String userId,
        @Param("type") TransactionType type,
        @Param("since") Instant since,
        @Param("minAmount") BigDecimal minAmount
    );
    
    /**
     * Get daily transaction volume
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.transactionType = :type AND l.currency = :currency AND l.createdAt >= :startOfDay AND l.createdAt < :endOfDay")
    BigDecimal getDailyVolume(
        @Param("type") TransactionType type,
        @Param("currency") CurrencyType currency,
        @Param("startOfDay") Instant startOfDay,
        @Param("endOfDay") Instant endOfDay
    );
    
    /**
     * Check for duplicate transactions (fraud prevention)
     */
    @Query("SELECT COUNT(l) > 0 FROM LedgerEntry l WHERE l.referenceId = :referenceId AND l.userId = :userId AND l.amount = :amount AND l.createdAt >= :since")
    boolean hasDuplicateTransaction(
        @Param("referenceId") String referenceId,
        @Param("userId") String userId,
        @Param("amount") BigDecimal amount,
        @Param("since") Instant since
    );
}
