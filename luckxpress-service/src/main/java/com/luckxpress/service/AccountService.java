package com.luckxpress.service;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.exception.InsufficientBalanceException;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Account Service
 * CRITICAL: Manages user accounts and balance operations with compliance
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    
    /**
     * Find account by ID
     */
    public Optional<Account> findById(String accountId) {
        return accountRepository.findById(accountId);
    }
    
    /**
     * Find account by user and currency
     */
    public Optional<Account> findByUserAndCurrency(User user, CurrencyType currencyType) {
        return accountRepository.findByUserAndCurrencyType(user, currencyType);
    }
    
    /**
     * Find account by user ID and currency
     */
    public Optional<Account> findByUserIdAndCurrency(String userId, CurrencyType currencyType) {
        return accountRepository.findByUserIdAndCurrencyType(userId, currencyType);
    }
    
    /**
     * Find all accounts for user
     */
    public List<Account> findUserAccounts(String userId) {
        return accountRepository.findByUserId(userId);
    }
    
    /**
     * Create accounts for new user (both Gold and Sweeps)
     */
    @Transactional
    public void createUserAccounts(User user) {
        log.info("Creating accounts for user: userId={}", user.getId());
        
        // Create Gold Coin account
        Account goldAccount = createAccount(user, CurrencyType.GOLD);
        
        // Create Sweeps Coin account
        Account sweepsAccount = createAccount(user, CurrencyType.SWEEPS);
        
        auditService.logAccountsCreated(user, List.of(goldAccount, sweepsAccount));
        
        log.info("Accounts created for user: userId={}, goldAccountId={}, sweepsAccountId={}", 
                user.getId(), goldAccount.getId(), sweepsAccount.getId());
    }
    
    /**
     * Create single account for user and currency
     */
    @Transactional
    public Account createAccount(User user, CurrencyType currencyType) {
        // Check if account already exists
        Optional<Account> existingAccount = findByUserAndCurrency(user, currencyType);
        if (existingAccount.isPresent()) {
            throw new IllegalStateException("Account already exists for user and currency");
        }
        
        Account account = new Account();
        account.setId(IdGenerator.generateId("ACC"));
        account.setUser(user);
        account.setCurrencyType(currencyType);
        account.setBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setPendingBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setDailyDepositTotal(BigDecimal.ZERO);
        account.setDailyWithdrawalTotal(BigDecimal.ZERO);
        account.setLifetimeDeposits(BigDecimal.ZERO);
        account.setLifetimeWithdrawals(BigDecimal.ZERO);
        account.setDailyResetDate(Instant.now());
        
        return accountRepository.save(account);
    }
    
    /**
     * Credit account balance
     */
    @Transactional
    public Account creditBalance(String accountId, BigDecimal amount, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        return creditBalance(account, amount, reason);
    }
    
    /**
     * Credit account balance
     */
    @Transactional
    public Account creditBalance(Account account, BigDecimal amount, String reason) {
        log.info("Crediting account: accountId={}, amount={}, reason={}", 
                account.getId(), amount, reason);
        
        // Validate account can be credited
        if (!account.canCredit()) {
            throw new IllegalStateException("Account cannot be credited: " + account.getStatus());
        }
        
        // Normalize amount
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        
        // Update balances
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = MoneyUtil.add(oldBalance, normalizedAmount);
        BigDecimal newAvailableBalance = MoneyUtil.add(account.getAvailableBalance(), normalizedAmount);
        
        account.setBalance(newBalance);
        account.setAvailableBalance(newAvailableBalance);
        account.recordTransaction();
        
        account = accountRepository.save(account);
        
        auditService.logBalanceCredited(account, normalizedAmount, oldBalance, newBalance, reason);
        
        log.info("Account credited successfully: accountId={}, oldBalance={}, newBalance={}", 
                account.getId(), oldBalance, newBalance);
        
        return account;
    }
    
    /**
     * Debit account balance
     */
    @Transactional
    public Account debitBalance(String accountId, BigDecimal amount, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        return debitBalance(account, amount, reason);
    }
    
    /**
     * Debit account balance
     */
    @Transactional
    public Account debitBalance(Account account, BigDecimal amount, String reason) {
        log.info("Debiting account: accountId={}, amount={}, reason={}", 
                account.getId(), amount, reason);
        
        // Normalize amount
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        
        // Validate account can be debited
        if (!account.canDebit(normalizedAmount)) {
            throw new InsufficientBalanceException(
                account.getUser().getId(),
                account.getCurrencyType(),
                normalizedAmount,
                account.getAvailableBalance()
            );
        }
        
        // Update balances
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = MoneyUtil.subtract(oldBalance, normalizedAmount);
        BigDecimal newAvailableBalance = MoneyUtil.subtract(account.getAvailableBalance(), normalizedAmount);
        
        account.setBalance(newBalance);
        account.setAvailableBalance(newAvailableBalance);
        account.recordTransaction();
        
        account = accountRepository.save(account);
        
        auditService.logBalanceDebited(account, normalizedAmount, oldBalance, newBalance, reason);
        
        log.info("Account debited successfully: accountId={}, oldBalance={}, newBalance={}", 
                account.getId(), oldBalance, newBalance);
        
        return account;
    }
    
    /**
     * Hold balance (move from available to pending)
     */
    @Transactional
    public Account holdBalance(String accountId, BigDecimal amount, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        log.info("Holding balance: accountId={}, amount={}, reason={}", 
                account.getId(), amount, reason);
        
        // Normalize amount
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        
        // Validate sufficient available balance
        if (account.getAvailableBalance().compareTo(normalizedAmount) < 0) {
            throw new InsufficientBalanceException(
                account.getUser().getId(),
                account.getCurrencyType(),
                normalizedAmount,
                account.getAvailableBalance()
            );
        }
        
        // Move from available to pending
        BigDecimal newAvailableBalance = MoneyUtil.subtract(account.getAvailableBalance(), normalizedAmount);
        BigDecimal newPendingBalance = MoneyUtil.add(account.getPendingBalance(), normalizedAmount);
        
        account.setAvailableBalance(newAvailableBalance);
        account.setPendingBalance(newPendingBalance);
        account.recordTransaction();
        
        account = accountRepository.save(account);
        
        auditService.logBalanceHeld(account, normalizedAmount, reason);
        
        log.info("Balance held successfully: accountId={}, amount={}", account.getId(), normalizedAmount);
        
        return account;
    }
    
    /**
     * Release held balance (move from pending back to available)
     */
    @Transactional
    public Account releaseHeldBalance(String accountId, BigDecimal amount, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        log.info("Releasing held balance: accountId={}, amount={}, reason={}", 
                account.getId(), amount, reason);
        
        // Normalize amount
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }
        
        // Validate sufficient pending balance
        if (account.getPendingBalance().compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("Insufficient pending balance to release");
        }
        
        // Move from pending back to available
        BigDecimal newPendingBalance = MoneyUtil.subtract(account.getPendingBalance(), normalizedAmount);
        BigDecimal newAvailableBalance = MoneyUtil.add(account.getAvailableBalance(), normalizedAmount);
        
        account.setPendingBalance(newPendingBalance);
        account.setAvailableBalance(newAvailableBalance);
        account.recordTransaction();
        
        account = accountRepository.save(account);
        
        auditService.logBalanceReleased(account, normalizedAmount, reason);
        
        log.info("Held balance released successfully: accountId={}, amount={}", account.getId(), normalizedAmount);
        
        return account;
    }
    
    /**
     * Confirm held balance (remove from pending and total balance)
     */
    @Transactional
    public Account confirmHeldBalance(String accountId, BigDecimal amount, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        log.info("Confirming held balance: accountId={}, amount={}, reason={}", 
                account.getId(), amount, reason);
        
        // Normalize amount
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Confirm amount must be positive");
        }
        
        // Validate sufficient pending balance
        if (account.getPendingBalance().compareTo(normalizedAmount) < 0) {
            throw new IllegalArgumentException("Insufficient pending balance to confirm");
        }
        
        // Remove from both pending and total balance
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = MoneyUtil.subtract(oldBalance, normalizedAmount);
        BigDecimal newPendingBalance = MoneyUtil.subtract(account.getPendingBalance(), normalizedAmount);
        
        account.setBalance(newBalance);
        account.setPendingBalance(newPendingBalance);
        account.recordTransaction();
        
        account = accountRepository.save(account);
        
        auditService.logBalanceConfirmed(account, normalizedAmount, oldBalance, newBalance, reason);
        
        log.info("Held balance confirmed successfully: accountId={}, amount={}, oldBalance={}, newBalance={}", 
                account.getId(), normalizedAmount, oldBalance, newBalance);
        
        return account;
    }
    
    /**
     * Freeze account
     */
    @Transactional
    public void freezeAccount(String accountId, Instant frozenUntil, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        account.freezeUntil(frozenUntil);
        accountRepository.save(account);
        
        auditService.logAccountFrozen(account, frozenUntil, reason);
        
        log.warn("Account frozen: accountId={}, until={}, reason={}", accountId, frozenUntil, reason);
    }
    
    /**
     * Unfreeze account
     */
    @Transactional
    public void unfreezeAccount(String accountId, String reason) {
        Account account = findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        account.unfreeze();
        accountRepository.save(account);
        
        auditService.logAccountUnfrozen(account, reason);
        
        log.info("Account unfrozen: accountId={}, reason={}", accountId, reason);
    }
    
    /**
     * Reset daily totals for all accounts
     */
    @Transactional
    public void resetDailyTotals() {
        log.info("Resetting daily totals for all accounts");
        
        Instant resetDate = Instant.now();
        accountRepository.resetDailyTotalsForAllAccounts(resetDate);
        
        log.info("Daily totals reset completed");
    }
    
    /**
     * Get account balance summary
     */
    public List<Object[]> getBalanceSummaryByCurrency() {
        return accountRepository.getBalanceSummaryByCurrency();
    }
    
    /**
     * Find accounts with zero balance
     */
    public List<Account> findAccountsWithZeroBalance() {
        return accountRepository.findAccountsWithZeroBalance();
    }
    
    /**
     * Find accounts with negative balance (should not happen)
     */
    public List<Account> findAccountsWithNegativeBalance() {
        return accountRepository.findAccountsWithNegativeBalance();
    }
    
    /**
     * Find dormant accounts
     */
    public List<Account> findDormantAccounts(int daysSinceLastTransaction) {
        Instant cutoffDate = Instant.now().minusSeconds(daysSinceLastTransaction * 24 * 60 * 60);
        return accountRepository.findDormantAccounts(cutoffDate);
    }
    
    /**
     * Check if daily reset is needed for account
     */
    private boolean needsDailyReset(Account account) {
        if (account.getDailyResetDate() == null) {
            return true;
        }
        
        LocalDate resetDate = account.getDailyResetDate().atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDate.now();
        
        return resetDate.isBefore(today);
    }
    
    /**
     * Reset daily totals for specific account if needed
     */
    @Transactional
    public void resetDailyTotalsIfNeeded(Account account) {
        if (needsDailyReset(account)) {
            account.resetDailyTotals();
            accountRepository.save(account);
            
            log.info("Daily totals reset for account: accountId={}", account.getId());
        }
    }
}
