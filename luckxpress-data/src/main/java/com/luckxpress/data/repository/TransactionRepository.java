package com.luckxpress.data.repository;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.data.entity.Account;
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
 * Transaction Repository
 * CRITICAL: Provides data access for financial transactions with audit trail
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    /**
     * Find transaction by idempotency key
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Check if idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find transactions by user
     */
    List<Transaction> findByUser(User user);
    
    /**
     * Find transactions by user ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId")
    Page<Transaction> findByUserId(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Find transactions by account
     */
    List<Transaction> findByAccount(Account account);
    
    /**
     * Find transactions by account ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") String accountId, Pageable pageable);
    
    /**
     * Find transactions by transaction type
     */
    List<Transaction> findByTransactionType(TransactionType transactionType);
    
    /**
     * Find transactions by currency type
     */
    List<Transaction> findByCurrencyType(CurrencyType currencyType);
    
    /**
     * Find transactions by status
     */
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    /**
     * Find pending transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'PROCESSING')")
    List<Transaction> findPendingTransactions();
    
    /**
     * Find completed transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'COMPLETED'")
    Page<Transaction> findCompletedTransactions(Pageable pageable);
    
    /**
     * Find failed transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('FAILED', 'CANCELLED', 'REJECTED')")
    List<Transaction> findFailedTransactions();
    
    /**
     * Find transactions requiring approval
     */
    @Query("SELECT t FROM Transaction t WHERE t.requiresApproval = true AND t.status = 'PENDING_APPROVAL'")
    List<Transaction> findTransactionsRequiringApproval();
    
    /**
     * Find transactions by external reference
     */
    Optional<Transaction> findByExternalReference(String externalReference);
    
    /**
     * Find transactions by date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findTransactionsBetween(@Param("startDate") Instant startDate, 
                                            @Param("endDate") Instant endDate, 
                                            Pageable pageable);
    
    /**
     * Find transactions by amount range
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount BETWEEN :minAmount AND :maxAmount")
    List<Transaction> findTransactionsByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                                   @Param("maxAmount") BigDecimal maxAmount);
    
    /**
     * Find large transactions (above threshold)
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount > :threshold")
    List<Transaction> findLargeTransactions(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find deposits
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = 'DEPOSIT'")
    Page<Transaction> findDeposits(Pageable pageable);
    
    /**
     * Find withdrawals
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = 'WITHDRAWAL'")
    Page<Transaction> findWithdrawals(Pageable pageable);
    
    /**
     * Find user deposits
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = 'DEPOSIT'")
    List<Transaction> findUserDeposits(@Param("userId") String userId);
    
    /**
     * Find user withdrawals
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = 'WITHDRAWAL'")
    List<Transaction> findUserWithdrawals(@Param("userId") String userId);
    
    /**
     * Find transactions by IP address
     */
    List<Transaction> findByIpAddress(String ipAddress);
    
    /**
     * Find suspicious transactions (rapid sequence)
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.createdAt > :cutoffTime ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactionsByUser(@Param("userId") String userId, 
                                                  @Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find transactions by payment method
     */
    List<Transaction> findByPaymentMethod(String paymentMethod);
    
    /**
     * Find related transactions
     */
    List<Transaction> findByRelatedTransactionId(String relatedTransactionId);
    
    /**
     * Get daily transaction volume by currency
     */
    @Query("SELECT t.currencyType, SUM(t.amount) FROM Transaction t " +
           "WHERE DATE(t.createdAt) = CURRENT_DATE AND t.status = 'COMPLETED' " +
           "GROUP BY t.currencyType")
    List<Object[]> getDailyVolumeByCurrency();
    
    /**
     * Get transaction count by status
     */
    @Query("SELECT t.status, COUNT(t) FROM Transaction t GROUP BY t.status")
    List<Object[]> getTransactionCountByStatus();
    
    /**
     * Get transaction count by type
     */
    @Query("SELECT t.transactionType, COUNT(t) FROM Transaction t GROUP BY t.transactionType")
    List<Object[]> getTransactionCountByType();
    
    /**
     * Find user's daily deposit total
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = 'DEPOSIT' " +
           "AND DATE(t.createdAt) = CURRENT_DATE AND t.status = 'COMPLETED'")
    BigDecimal getUserDailyDepositTotal(@Param("userId") String userId);
    
    /**
     * Find user's daily withdrawal total
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = 'WITHDRAWAL' " +
           "AND DATE(t.createdAt) = CURRENT_DATE AND t.status = 'COMPLETED'")
    BigDecimal getUserDailyWithdrawalTotal(@Param("userId") String userId);
    
    /**
     * Find transactions created today
     */
    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) = CURRENT_DATE")
    List<Transaction> findTransactionsCreatedToday();
    
    /**
     * Find pending transactions older than specified time
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'PROCESSING') AND t.createdAt < :cutoffTime")
    List<Transaction> findStalePendingTransactions(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find transactions awaiting approval for too long
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING_APPROVAL' AND t.createdAt < :cutoffTime")
    List<Transaction> findStaleApprovalTransactions(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find high-frequency transactions by user
     */
    @Query("SELECT t.user.id, COUNT(t) FROM Transaction t " +
           "WHERE t.createdAt > :cutoffTime " +
           "GROUP BY t.user.id " +
           "HAVING COUNT(t) > :threshold")
    List<Object[]> findHighFrequencyTransactionUsers(@Param("cutoffTime") Instant cutoffTime, 
                                                     @Param("threshold") Long threshold);
    
    /**
     * Find transactions by user and currency in date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.currencyType = :currencyType " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactionsByCurrencyAndDateRange(@Param("userId") String userId,
                                                               @Param("currencyType") CurrencyType currencyType,
                                                               @Param("startDate") Instant startDate,
                                                               @Param("endDate") Instant endDate);
    
    /**
     * Update transaction status
     */
    @Query("UPDATE Transaction t SET t.status = :status, t.processedAt = :processedAt WHERE t.id = :transactionId")
    void updateTransactionStatus(@Param("transactionId") String transactionId,
                                @Param("status") Transaction.TransactionStatus status,
                                @Param("processedAt") Instant processedAt);
    
    /**
     * Mark transaction as failed
     */
    @Query("UPDATE Transaction t SET t.status = 'FAILED', t.failedAt = :failedAt, t.failureReason = :reason WHERE t.id = :transactionId")
    void markTransactionAsFailed(@Param("transactionId") String transactionId,
                                @Param("failedAt") Instant failedAt,
                                @Param("reason") String reason);
}
