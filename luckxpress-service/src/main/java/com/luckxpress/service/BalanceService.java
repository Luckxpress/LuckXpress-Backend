package com.luckxpress.service;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.Balance;
import com.luckxpress.data.entity.LedgerEntry;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.data.repository.AccountRepository;
import com.luckxpress.data.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Balance Service
 * CRITICAL: Manages balance snapshots, ledger entries, and reconciliation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceService {
    
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final AuditService auditService;
    
    /**
     * Create transaction snapshot
     */
    @Transactional
    public Balance createTransactionSnapshot(Account account, Transaction transaction, BigDecimal balanceBefore) {
        log.debug("Creating transaction snapshot: accountId={}, transactionId={}", 
                account.getId(), transaction.getId());
        
        BigDecimal availableBalanceBefore = account.getAvailableBalance().subtract(
            account.getBalance().subtract(balanceBefore)
        );
        BigDecimal pendingBalanceBefore = account.getPendingBalance();
        
        Balance snapshot = Balance.createSnapshot(
            account, 
            transaction, 
            Balance.SnapshotType.TRANSACTION,
            balanceBefore,
            availableBalanceBefore,
            pendingBalanceBefore
        );
        
        // Create corresponding ledger entry
        createLedgerEntry(account, transaction, balanceBefore, account.getBalance());
        
        return snapshot;
    }
    
    /**
     * Create daily balance snapshot
     */
    @Transactional
    public Balance createDailySnapshot(Account account) {
        log.debug("Creating daily snapshot: accountId={}", account.getId());
        
        Balance snapshot = Balance.createDailySnapshot(account);
        
        auditService.logDailySnapshotCreated(account);
        
        return snapshot;
    }
    
    /**
     * Create ledger entry for transaction
     */
    @Transactional
    public LedgerEntry createLedgerEntry(Account account, Transaction transaction, 
                                       BigDecimal balanceBefore, BigDecimal balanceAfter) {
        
        log.debug("Creating ledger entry: accountId={}, transactionId={}", 
                account.getId(), transaction.getId());
        
        LedgerEntry entry;
        String description = transaction.getDisplayDescription();
        
        if (transaction.isCredit()) {
            entry = LedgerEntry.createCredit(
                account.getUser(),
                account,
                transaction,
                transaction.getAmount(),
                balanceAfter,
                description
            );
        } else {
            entry = LedgerEntry.createDebit(
                account.getUser(),
                account,
                transaction,
                transaction.getAmount(),
                balanceAfter,
                description
            );
        }
        
        entry.setPostedBy(transaction.getCreatedBy());
        entry.setExternalReference(transaction.getExternalReference());
        
        entry = ledgerRepository.save(entry);
        
        log.debug("Ledger entry created: entryId={}, referenceNumber={}", 
                entry.getId(), entry.getReferenceNumber());
        
        return entry;
    }
    
    /**
     * Create manual adjustment ledger entry
     */
    @Transactional
    public LedgerEntry createAdjustmentEntry(Account account, BigDecimal amount, 
                                           String reason, boolean isCredit) {
        
        log.info("Creating adjustment entry: accountId={}, amount={}, isCredit={}, reason={}", 
                account.getId(), amount, isCredit, reason);
        
        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter;
        
        if (isCredit) {
            balanceAfter = balanceBefore.add(amount);
            account.setBalance(balanceAfter);
            account.setAvailableBalance(account.getAvailableBalance().add(amount));
        } else {
            balanceAfter = balanceBefore.subtract(amount);
            account.setBalance(balanceAfter);
            account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        }
        
        accountRepository.save(account);
        
        LedgerEntry entry;
        if (isCredit) {
            entry = LedgerEntry.createCredit(
                account.getUser(),
                account,
                null, // No transaction for manual adjustment
                amount,
                balanceAfter,
                "Manual adjustment: " + reason
            );
        } else {
            entry = LedgerEntry.createDebit(
                account.getUser(),
                account,
                null,
                amount,
                balanceAfter,
                "Manual adjustment: " + reason
            );
        }
        
        entry = ledgerRepository.save(entry);
        
        auditService.logManualAdjustment(account, amount, isCredit, reason, balanceBefore, balanceAfter);
        
        log.info("Adjustment entry created: entryId={}, balanceBefore={}, balanceAfter={}", 
                entry.getId(), balanceBefore, balanceAfter);
        
        return entry;
    }
    
    /**
     * Get account balance at specific point in time
     */
    public Optional<BigDecimal> getAccountBalanceAtTime(String accountId, Instant pointInTime) {
        List<BigDecimal> balances = ledgerRepository.getAccountBalanceAtTime(accountId, pointInTime);
        return balances.isEmpty() ? Optional.empty() : Optional.of(balances.get(0));
    }
    
    /**
     * Get latest ledger entry for account
     */
    public Optional<LedgerEntry> getLatestLedgerEntry(String accountId) {
        return ledgerRepository.getLatestEntryForAccount(accountId);
    }
    
    /**
     * Calculate account balance from ledger
     */
    public BigDecimal calculateBalanceFromLedger(String accountId) {
        return ledgerRepository.getCalculatedBalanceForAccount(accountId);
    }
    
    /**
     * Verify account balance integrity
     */
    public boolean verifyAccountBalanceIntegrity(String accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return false;
        }
        
        BigDecimal ledgerBalance = calculateBalanceFromLedger(accountId);
        BigDecimal accountBalance = account.getBalance();
        
        boolean isIntegrityValid = ledgerBalance.compareTo(accountBalance) == 0;
        
        if (!isIntegrityValid) {
            log.error("Balance integrity issue detected: accountId={}, ledgerBalance={}, accountBalance={}", 
                    accountId, ledgerBalance, accountBalance);
            
            auditService.logBalanceIntegrityIssue(account, ledgerBalance, accountBalance);
        }
        
        return isIntegrityValid;
    }
    
    /**
     * Reconcile account balance
     */
    @Transactional
    public void reconcileAccountBalance(String accountId) {
        log.info("Reconciling account balance: accountId={}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        BigDecimal ledgerBalance = calculateBalanceFromLedger(accountId);
        BigDecimal accountBalance = account.getBalance();
        
        if (ledgerBalance.compareTo(accountBalance) != 0) {
            log.warn("Balance mismatch found during reconciliation: accountId={}, ledgerBalance={}, accountBalance={}", 
                    accountId, ledgerBalance, accountBalance);
            
            // Create reconciliation adjustment
            BigDecimal difference = ledgerBalance.subtract(accountBalance);
            boolean isCredit = difference.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal adjustmentAmount = difference.abs();
            
            createAdjustmentEntry(
                account, 
                adjustmentAmount, 
                "Reconciliation adjustment", 
                isCredit
            );
            
            auditService.logReconciliationAdjustment(account, difference, ledgerBalance, accountBalance);
        }
        
        // Create reconciliation snapshot
        Balance reconciliationSnapshot = Balance.createSnapshot(
            account,
            null,
            Balance.SnapshotType.RECONCILIATION,
            accountBalance,
            account.getAvailableBalance(),
            account.getPendingBalance()
        );
        
        reconciliationSnapshot.setReconciliationId(IdGenerator.generateId("REC"));
        
        log.info("Account reconciliation completed: accountId={}", accountId);
    }
    
    /**
     * Perform daily reconciliation for all accounts
     */
    @Transactional
    public void performDailyReconciliation() {
        log.info("Starting daily reconciliation for all accounts");
        
        List<Account> allAccounts = accountRepository.findAll();
        int reconciledCount = 0;
        int issuesFound = 0;
        
        for (Account account : allAccounts) {
            try {
                boolean hasIntegrityIssue = !verifyAccountBalanceIntegrity(account.getId());
                if (hasIntegrityIssue) {
                    issuesFound++;
                    reconcileAccountBalance(account.getId());
                }
                
                // Create daily snapshot
                createDailySnapshot(account);
                reconciledCount++;
                
            } catch (Exception e) {
                log.error("Error during reconciliation for account: accountId={}", account.getId(), e);
            }
        }
        
        log.info("Daily reconciliation completed: reconciledAccounts={}, issuesFound={}", 
                reconciledCount, issuesFound);
        
        auditService.logDailyReconciliationCompleted(reconciledCount, issuesFound);
    }
    
    /**
     * Get balance summary by currency
     */
    public List<Object[]> getBalanceSummaryByCurrency() {
        return accountRepository.getBalanceSummaryByCurrency();
    }
    
    /**
     * Get daily volume by currency
     */
    public List<Object[]> getDailyVolumeByCurrency(LocalDate date) {
        return ledgerRepository.getDailyVolumeByCurrency(date);
    }
    
    /**
     * Find accounts with balance integrity issues
     */
    public List<Account> findAccountsWithIntegrityIssues() {
        List<Account> allAccounts = accountRepository.findAll();
        return allAccounts.stream()
            .filter(account -> !verifyAccountBalanceIntegrity(account.getId()))
            .toList();
    }
    
    /**
     * Find unreconciled ledger entries
     */
    public List<LedgerEntry> findUnreconciledEntries() {
        return ledgerRepository.findUnreconciledEntries();
    }
    
    /**
     * Mark ledger entries as reconciled
     */
    @Transactional
    public void markEntriesAsReconciled(List<String> entryIds, String batchId) {
        log.info("Marking entries as reconciled: count={}, batchId={}", entryIds.size(), batchId);
        
        Instant reconciledAt = Instant.now();
        ledgerRepository.markEntriesAsReconciled(entryIds, reconciledAt, batchId);
        
        log.info("Entries marked as reconciled: count={}, batchId={}", entryIds.size(), batchId);
    }
    
    /**
     * Get entries requiring reconciliation
     */
    public List<LedgerEntry> getEntriesRequiringReconciliation(int hoursOld) {
        Instant cutoffTime = Instant.now().minusSeconds(hoursOld * 60 * 60);
        return ledgerRepository.getEntriesRequiringReconciliation(cutoffTime);
    }
    
    /**
     * Create reversal ledger entry
     */
    @Transactional
    public LedgerEntry createReversalEntry(String originalEntryId, String reversalReason) {
        log.info("Creating reversal entry: originalEntryId={}, reason={}", originalEntryId, reversalReason);
        
        LedgerEntry originalEntry = ledgerRepository.findById(originalEntryId)
            .orElseThrow(() -> new IllegalArgumentException("Original ledger entry not found: " + originalEntryId));
        
        Account account = originalEntry.getAccount();
        BigDecimal currentBalance = account.getBalance();
        BigDecimal newBalance;
        
        // Calculate new balance after reversal
        if (originalEntry.isCredit()) {
            newBalance = currentBalance.subtract(originalEntry.getCreditAmount());
            account.setBalance(newBalance);
            account.setAvailableBalance(account.getAvailableBalance().subtract(originalEntry.getCreditAmount()));
        } else {
            newBalance = currentBalance.add(originalEntry.getDebitAmount());
            account.setBalance(newBalance);
            account.setAvailableBalance(account.getAvailableBalance().add(originalEntry.getDebitAmount()));
        }
        
        accountRepository.save(account);
        
        // Create reversal entry
        LedgerEntry reversalEntry = originalEntry.createReversal(newBalance, reversalReason);
        reversalEntry = ledgerRepository.save(reversalEntry);
        
        auditService.logLedgerEntryReversed(originalEntry, reversalEntry, reversalReason);
        
        log.info("Reversal entry created: reversalEntryId={}, originalEntryId={}", 
                reversalEntry.getId(), originalEntryId);
        
        return reversalEntry;
    }
    
    /**
     * Get ledger entries for account in date range
     */
    public List<LedgerEntry> getAccountLedgerEntries(String accountId, CurrencyType currencyType, 
                                                   Instant startDate, Instant endDate) {
        return ledgerRepository.findUserEntriesByCurrencyAndDateRange(
            accountId, currencyType, startDate, endDate
        );
    }
    
    /**
     * Get account transaction history
     */
    public Page<LedgerEntry> getAccountTransactionHistory(String accountId, Pageable pageable) {
        return ledgerRepository.findByAccountId(accountId, pageable);
    }
}
