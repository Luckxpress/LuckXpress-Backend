package com.luckxpress.data.repository;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.LedgerEntry;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.data.entity.User;
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
 * Ledger Repository
 * CRITICAL: Provides data access for immutable financial ledger entries
 */
@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, String> {
    
    /**
     * Find ledger entry by reference number
     */
    Optional<LedgerEntry> findByReferenceNumber(String referenceNumber);
    
    /**
     * Find ledger entries by user
     */
    List<LedgerEntry> findByUser(User user);
    
    /**
     * Find ledger entries by user ID
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.user.id = :userId ORDER BY l.postedAt DESC")
    Page<LedgerEntry> findByUserId(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Find ledger entries by account
     */
    List<LedgerEntry> findByAccount(Account account);
    
    /**
     * Find ledger entries by account ID
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.account.id = :accountId ORDER BY l.postedAt DESC")
    Page<LedgerEntry> findByAccountId(@Param("accountId") String accountId, Pageable pageable);
    
    /**
     * Find ledger entries by transaction
     */
    List<LedgerEntry> findByTransaction(Transaction transaction);
    
    /**
     * Find ledger entries by transaction ID
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.transaction.id = :transactionId")
    List<LedgerEntry> findByTransactionId(@Param("transactionId") String transactionId);
    
    /**
     * Find ledger entries by entry type
     */
    List<LedgerEntry> findByEntryType(LedgerEntry.EntryType entryType);
    
    /**
     * Find debit entries
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.entryType = 'DEBIT'")
    Page<LedgerEntry> findDebitEntries(Pageable pageable);
    
    /**
     * Find credit entries
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.entryType = 'CREDIT'")
    Page<LedgerEntry> findCreditEntries(Pageable pageable);
    
    /**
     * Find ledger entries by transaction type
     */
    List<LedgerEntry> findByTransactionType(TransactionType transactionType);
    
    /**
     * Find ledger entries by currency type
     */
    List<LedgerEntry> findByCurrencyType(CurrencyType currencyType);
    
    /**
     * Find ledger entries by date range
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.postedAt BETWEEN :startDate AND :endDate ORDER BY l.postedAt DESC")
    Page<LedgerEntry> findEntriesBetween(@Param("startDate") Instant startDate, 
                                        @Param("endDate") Instant endDate, 
                                        Pageable pageable);
    
    /**
     * Find ledger entries by amount range
     */
    @Query("SELECT l FROM LedgerEntry l WHERE " +
           "(l.entryType = 'DEBIT' AND l.debitAmount BETWEEN :minAmount AND :maxAmount) OR " +
           "(l.entryType = 'CREDIT' AND l.creditAmount BETWEEN :minAmount AND :maxAmount)")
    List<LedgerEntry> findEntriesByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                              @Param("maxAmount") BigDecimal maxAmount);
    
    /**
     * Find reconciled entries
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.reconciledAt IS NOT NULL")
    List<LedgerEntry> findReconciledEntries();
    
    /**
     * Find unreconciled entries
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.reconciledAt IS NULL")
    List<LedgerEntry> findUnreconciledEntries();
    
    /**
     * Find entries by reconciliation batch
     */
    List<LedgerEntry> findByReconciliationBatch(String reconciliationBatch);
    
    /**
     * Find reversal entries
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.isReversal = true")
    List<LedgerEntry> findReversalEntries();
    
    /**
     * Find entries by reversal entry ID
     */
    List<LedgerEntry> findByReversalEntryId(String reversalEntryId);
    
    /**
     * Find entries posted by user
     */
    List<LedgerEntry> findByPostedBy(String postedBy);
    
    /**
     * Find entries by external reference
     */
    List<LedgerEntry> findByExternalReference(String externalReference);
    
    /**
     * Get account balance at specific point in time
     */
    @Query("SELECT l.balanceAfter FROM LedgerEntry l " +
           "WHERE l.account.id = :accountId AND l.postedAt <= :pointInTime " +
           "ORDER BY l.postedAt DESC, l.createdAt DESC")
    List<BigDecimal> getAccountBalanceAtTime(@Param("accountId") String accountId, 
                                           @Param("pointInTime") Instant pointInTime);
    
    /**
     * Get latest ledger entry for account
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.account.id = :accountId " +
           "ORDER BY l.postedAt DESC, l.createdAt DESC")
    Optional<LedgerEntry> getLatestEntryForAccount(@Param("accountId") String accountId);
    
    /**
     * Calculate total debits for account
     */
    @Query("SELECT COALESCE(SUM(l.debitAmount), 0) FROM LedgerEntry l " +
           "WHERE l.account.id = :accountId AND l.entryType = 'DEBIT'")
    BigDecimal getTotalDebitsForAccount(@Param("accountId") String accountId);
    
    /**
     * Calculate total credits for account
     */
    @Query("SELECT COALESCE(SUM(l.creditAmount), 0) FROM LedgerEntry l " +
           "WHERE l.account.id = :accountId AND l.entryType = 'CREDIT'")
    BigDecimal getTotalCreditsForAccount(@Param("accountId") String accountId);
    
    /**
     * Calculate net balance for account from ledger
     */
    @Query("SELECT COALESCE(SUM(l.creditAmount), 0) - COALESCE(SUM(l.debitAmount), 0) " +
           "FROM LedgerEntry l WHERE l.account.id = :accountId")
    BigDecimal getCalculatedBalanceForAccount(@Param("accountId") String accountId);
    
    /**
     * Find entries for daily reconciliation
     */
    @Query("SELECT l FROM LedgerEntry l WHERE DATE(l.postedAt) = :date AND l.reconciledAt IS NULL")
    List<LedgerEntry> findEntriesForDailyReconciliation(@Param("date") java.time.LocalDate date);
    
    /**
     * Get daily transaction volume by currency
     */
    @Query("SELECT l.currencyType, " +
           "SUM(CASE WHEN l.entryType = 'DEBIT' THEN l.debitAmount ELSE 0 END) as totalDebits, " +
           "SUM(CASE WHEN l.entryType = 'CREDIT' THEN l.creditAmount ELSE 0 END) as totalCredits " +
           "FROM LedgerEntry l WHERE DATE(l.postedAt) = :date " +
           "GROUP BY l.currencyType")
    List<Object[]> getDailyVolumeByCurrency(@Param("date") java.time.LocalDate date);
    
    /**
     * Get ledger summary by transaction type
     */
    @Query("SELECT l.transactionType, l.entryType, COUNT(l), SUM(l.debitAmount), SUM(l.creditAmount) " +
           "FROM LedgerEntry l WHERE l.postedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY l.transactionType, l.entryType")
    List<Object[]> getLedgerSummaryByTransactionType(@Param("startDate") Instant startDate, 
                                                    @Param("endDate") Instant endDate);
    
    /**
     * Find entries posted today
     */
    @Query("SELECT l FROM LedgerEntry l WHERE DATE(l.postedAt) = CURRENT_DATE")
    List<LedgerEntry> findEntriesPostedToday();
    
    /**
     * Find large entries (above threshold)
     */
    @Query("SELECT l FROM LedgerEntry l WHERE " +
           "(l.entryType = 'DEBIT' AND l.debitAmount > :threshold) OR " +
           "(l.entryType = 'CREDIT' AND l.creditAmount > :threshold)")
    List<LedgerEntry> findLargeEntries(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find user's ledger entries by currency and date range
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.user.id = :userId " +
           "AND l.currencyType = :currencyType " +
           "AND l.postedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY l.postedAt DESC")
    List<LedgerEntry> findUserEntriesByCurrencyAndDateRange(@Param("userId") String userId,
                                                           @Param("currencyType") CurrencyType currencyType,
                                                           @Param("startDate") Instant startDate,
                                                           @Param("endDate") Instant endDate);
    
    /**
     * Verify ledger integrity for account
     */
    @Query("SELECT COUNT(l) FROM LedgerEntry l WHERE l.account.id = :accountId " +
           "AND l.balanceAfter != (SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.creditAmount ELSE -le.debitAmount END), 0) " +
           "FROM LedgerEntry le WHERE le.account.id = :accountId AND le.postedAt <= l.postedAt)")
    Long findBalanceIntegrityIssues(@Param("accountId") String accountId);
    
    /**
     * Mark entries as reconciled
     */
    @Query("UPDATE LedgerEntry l SET l.reconciledAt = :reconciledAt, l.reconciliationBatch = :batchId " +
           "WHERE l.id IN :entryIds")
    void markEntriesAsReconciled(@Param("entryIds") List<String> entryIds,
                                @Param("reconciledAt") Instant reconciledAt,
                                @Param("batchId") String batchId);
    
    /**
     * Get entries requiring reconciliation
     */
    @Query("SELECT l FROM LedgerEntry l WHERE l.reconciledAt IS NULL " +
           "AND l.postedAt < :cutoffTime ORDER BY l.postedAt ASC")
    List<LedgerEntry> getEntriesRequiringReconciliation(@Param("cutoffTime") Instant cutoffTime);
}
