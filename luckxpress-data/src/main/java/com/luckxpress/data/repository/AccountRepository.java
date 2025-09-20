package com.luckxpress.data.repository;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Account Repository
 * CRITICAL: Provides data access for user accounts and balance management
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    
    /**
     * Find account by user and currency type
     */
    Optional<Account> findByUserAndCurrencyType(User user, CurrencyType currencyType);
    
    /**
     * Find account by user ID and currency type
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.currencyType = :currencyType")
    Optional<Account> findByUserIdAndCurrencyType(@Param("userId") String userId, 
                                                 @Param("currencyType") CurrencyType currencyType);
    
    /**
     * Find all accounts for a user
     */
    List<Account> findByUser(User user);
    
    /**
     * Find all accounts for a user by user ID
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    List<Account> findByUserId(@Param("userId") String userId);
    
    /**
     * Find accounts by currency type
     */
    List<Account> findByCurrencyType(CurrencyType currencyType);
    
    /**
     * Find accounts by status
     */
    List<Account> findByStatus(Account.AccountStatus status);
    
    /**
     * Find active accounts
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE'")
    List<Account> findActiveAccounts();
    
    /**
     * Find frozen accounts
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'FROZEN' OR (a.frozenUntil IS NOT NULL AND a.frozenUntil > :now)")
    List<Account> findFrozenAccounts(@Param("now") Instant now);
    
    /**
     * Find accounts with balance greater than amount
     */
    @Query("SELECT a FROM Account a WHERE a.balance > :amount")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("amount") BigDecimal amount);
    
    /**
     * Find accounts with zero balance
     */
    @Query("SELECT a FROM Account a WHERE a.balance = 0")
    List<Account> findAccountsWithZeroBalance();
    
    /**
     * Find accounts with negative balance (should not happen)
     */
    @Query("SELECT a FROM Account a WHERE a.balance < 0")
    List<Account> findAccountsWithNegativeBalance();
    
    /**
     * Find accounts with pending balance
     */
    @Query("SELECT a FROM Account a WHERE a.pendingBalance > 0")
    List<Account> findAccountsWithPendingBalance();
    
    /**
     * Get total balance by currency type
     */
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.currencyType = :currencyType AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByCurrencyType(@Param("currencyType") CurrencyType currencyType);
    
    /**
     * Get total available balance by currency type
     */
    @Query("SELECT SUM(a.availableBalance) FROM Account a WHERE a.currencyType = :currencyType AND a.status = 'ACTIVE'")
    BigDecimal getTotalAvailableBalanceByCurrencyType(@Param("currencyType") CurrencyType currencyType);
    
    /**
     * Count accounts by status
     */
    @Query("SELECT a.status, COUNT(a) FROM Account a GROUP BY a.status")
    List<Object[]> countAccountsByStatus();
    
    /**
     * Count accounts by currency type
     */
    @Query("SELECT a.currencyType, COUNT(a) FROM Account a GROUP BY a.currencyType")
    List<Object[]> countAccountsByCurrencyType();
    
    /**
     * Find accounts with recent transactions
     */
    @Query("SELECT a FROM Account a WHERE a.lastTransactionAt > :cutoffDate")
    List<Account> findAccountsWithRecentTransactions(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find dormant accounts (no transactions within specified period)
     */
    @Query("SELECT a FROM Account a WHERE a.lastTransactionAt IS NULL OR a.lastTransactionAt < :cutoffDate")
    List<Account> findDormantAccounts(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find accounts exceeding daily deposit limit
     */
    @Query("SELECT a FROM Account a WHERE a.dailyDepositTotal > :limit")
    List<Account> findAccountsExceedingDailyDepositLimit(@Param("limit") BigDecimal limit);
    
    /**
     * Find accounts exceeding daily withdrawal limit
     */
    @Query("SELECT a FROM Account a WHERE a.dailyWithdrawalTotal > :limit")
    List<Account> findAccountsExceedingDailyWithdrawalLimit(@Param("limit") BigDecimal limit);
    
    /**
     * Find accounts needing daily reset
     */
    @Query("SELECT a FROM Account a WHERE a.dailyResetDate IS NULL OR a.dailyResetDate < :cutoffDate")
    List<Account> findAccountsNeedingDailyReset(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Find Gold Coin accounts
     */
    @Query("SELECT a FROM Account a WHERE a.currencyType = 'GOLD'")
    List<Account> findGoldCoinAccounts();
    
    /**
     * Find Sweeps Coin accounts
     */
    @Query("SELECT a FROM Account a WHERE a.currencyType = 'SWEEPS'")
    List<Account> findSweepsCoinAccounts();
    
    /**
     * Find accounts by user state code
     */
    @Query("SELECT a FROM Account a WHERE a.user.stateCode = :stateCode")
    List<Account> findAccountsByUserState(@Param("stateCode") String stateCode);
    
    /**
     * Find accounts for KYC verified users
     */
    @Query("SELECT a FROM Account a WHERE a.user.kycStatus = 'APPROVED'")
    List<Account> findAccountsForKycVerifiedUsers();
    
    /**
     * Find accounts for non-KYC verified users
     */
    @Query("SELECT a FROM Account a WHERE a.user.kycStatus != 'APPROVED'")
    List<Account> findAccountsForNonKycVerifiedUsers();
    
    /**
     * Update account balance (use with caution - prefer service layer)
     */
    @Query("UPDATE Account a SET a.balance = :balance, a.availableBalance = :availableBalance, " +
           "a.pendingBalance = :pendingBalance, a.lastTransactionAt = :transactionTime WHERE a.id = :accountId")
    void updateBalance(@Param("accountId") String accountId,
                      @Param("balance") BigDecimal balance,
                      @Param("availableBalance") BigDecimal availableBalance,
                      @Param("pendingBalance") BigDecimal pendingBalance,
                      @Param("transactionTime") Instant transactionTime);
    
    /**
     * Reset daily totals for all accounts
     */
    @Query("UPDATE Account a SET a.dailyDepositTotal = 0, a.dailyWithdrawalTotal = 0, a.dailyResetDate = :resetDate")
    void resetDailyTotalsForAllAccounts(@Param("resetDate") Instant resetDate);
    
    /**
     * Freeze account
     */
    @Query("UPDATE Account a SET a.status = 'FROZEN', a.frozenUntil = :frozenUntil WHERE a.id = :accountId")
    void freezeAccount(@Param("accountId") String accountId, @Param("frozenUntil") Instant frozenUntil);
    
    /**
     * Unfreeze account
     */
    @Query("UPDATE Account a SET a.status = 'ACTIVE', a.frozenUntil = null WHERE a.id = :accountId")
    void unfreezeAccount(@Param("accountId") String accountId);
    
    /**
     * Get balance summary by currency
     */
    @Query("SELECT a.currencyType, SUM(a.balance), SUM(a.availableBalance), SUM(a.pendingBalance), COUNT(a) " +
           "FROM Account a WHERE a.status = 'ACTIVE' GROUP BY a.currencyType")
    List<Object[]> getBalanceSummaryByCurrency();
}
