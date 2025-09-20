package com.luckxpress.service;

import com.luckxpress.common.constants.ComplianceConstants;
import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.common.exception.DualApprovalRequiredException;
import com.luckxpress.common.exception.IdempotencyException;
import com.luckxpress.common.exception.InsufficientBalanceException;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.common.util.MoneyUtil;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.data.entity.User;
import com.luckxpress.data.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Service
 * CRITICAL: Processes financial transactions with full compliance and audit trail
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final BalanceService balanceService;
    private final PaymentIdempotencyService idempotencyService;
    private final ApprovalService approvalService;
    private final ComplianceService complianceService;
    private final AuditService auditService;
    
    /**
     * Find transaction by ID
     */
    public Optional<Transaction> findById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * Find transaction by idempotency key
     */
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }
    
    /**
     * Find user transactions
     */
    public Page<Transaction> findUserTransactions(String userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Find account transactions
     */
    public Page<Transaction> findAccountTransactions(String accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }
    
    /**
     * Process deposit transaction
     */
    @Transactional
    public Transaction processDeposit(String userId, CurrencyType currencyType, 
                                    BigDecimal amount, String paymentMethod, 
                                    String externalReference, String idempotencyKey) {
        
        log.info("Processing deposit: userId={}, currency={}, amount={}, paymentMethod={}", 
                userId, currencyType, amount, paymentMethod);
        
        // Check idempotency
        if (idempotencyService.isDuplicateRequest(idempotencyKey)) {
            Transaction existingTransaction = findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException(idempotencyKey, null));
            throw new IdempotencyException(idempotencyKey, existingTransaction.getId());
        }
        
        // Get user account
        Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Validate deposit amount
        validateDepositAmount(amount, account.getUser());
        
        // Create transaction
        Transaction transaction = createTransaction(
            account.getUser(), 
            account, 
            TransactionType.DEPOSIT, 
            currencyType, 
            amount, 
            idempotencyKey
        );
        
        transaction.setPaymentMethod(paymentMethod);
        transaction.setExternalReference(externalReference);
        transaction.setDescription("Deposit via " + paymentMethod);
        
        // Check if requires approval
        if (requiresApproval(transaction)) {
            transaction.markRequiresApproval();
            transaction = transactionRepository.save(transaction);
            
            approvalService.createApprovalWorkflow(transaction, "Large deposit amount");
            
            log.info("Deposit requires approval: transactionId={}", transaction.getId());
            return transaction;
        }
        
        // Process the deposit
        return executeTransaction(transaction);
    }
    
    /**
     * Process withdrawal transaction
     */
    @Transactional
    public Transaction processWithdrawal(String userId, CurrencyType currencyType, 
                                       BigDecimal amount, String paymentMethod, 
                                       String idempotencyKey) {
        
        log.info("Processing withdrawal: userId={}, currency={}, amount={}, paymentMethod={}", 
                userId, currencyType, amount, paymentMethod);
        
        // Check idempotency
        if (idempotencyService.isDuplicateRequest(idempotencyKey)) {
            Transaction existingTransaction = findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException(idempotencyKey, null));
            throw new IdempotencyException(idempotencyKey, existingTransaction.getId());
        }
        
        // Get user account
        Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Validate withdrawal eligibility
        validateWithdrawalEligibility(account, amount);
        
        // Create transaction
        Transaction transaction = createTransaction(
            account.getUser(), 
            account, 
            TransactionType.WITHDRAWAL, 
            currencyType, 
            amount, 
            idempotencyKey
        );
        
        transaction.setPaymentMethod(paymentMethod);
        transaction.setDescription("Withdrawal via " + paymentMethod);
        
        // Check if requires approval
        if (requiresApproval(transaction)) {
            transaction.markRequiresApproval();
            transaction = transactionRepository.save(transaction);
            
            approvalService.createApprovalWorkflow(transaction, "Large withdrawal amount");
            
            log.info("Withdrawal requires approval: transactionId={}", transaction.getId());
            return transaction;
        }
        
        // Hold the balance first
        accountService.holdBalance(account.getId(), amount, "Withdrawal processing");
        
        // Process the withdrawal
        return executeTransaction(transaction);
    }
    
    /**
     * Process game bet transaction
     */
    @Transactional
    public Transaction processBet(String userId, CurrencyType currencyType, 
                                BigDecimal amount, String gameId, String idempotencyKey) {
        
        log.info("Processing bet: userId={}, currency={}, amount={}, gameId={}", 
                userId, currencyType, amount, gameId);
        
        // Check idempotency
        if (idempotencyService.isDuplicateRequest(idempotencyKey)) {
            Transaction existingTransaction = findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException(idempotencyKey, null));
            throw new IdempotencyException(idempotencyKey, existingTransaction.getId());
        }
        
        // Get user account
        Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Validate bet amount and user eligibility
        validateBetEligibility(account, amount);
        
        // Create transaction
        Transaction transaction = createTransaction(
            account.getUser(), 
            account, 
            TransactionType.BET, 
            currencyType, 
            amount, 
            idempotencyKey
        );
        
        transaction.setDescription("Game bet - " + gameId);
        transaction.setMetadata("{\"gameId\":\"" + gameId + "\"}");
        
        // Process the bet immediately (no approval needed for bets)
        return executeTransaction(transaction);
    }
    
    /**
     * Process game win transaction
     */
    @Transactional
    public Transaction processWin(String userId, CurrencyType currencyType, 
                                BigDecimal amount, String gameId, String betTransactionId, 
                                String idempotencyKey) {
        
        log.info("Processing win: userId={}, currency={}, amount={}, gameId={}", 
                userId, currencyType, amount, gameId);
        
        // Check idempotency
        if (idempotencyService.isDuplicateRequest(idempotencyKey)) {
            Transaction existingTransaction = findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException(idempotencyKey, null));
            throw new IdempotencyException(idempotencyKey, existingTransaction.getId());
        }
        
        // Get user account
        Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Create transaction
        Transaction transaction = createTransaction(
            account.getUser(), 
            account, 
            TransactionType.WIN, 
            currencyType, 
            amount, 
            idempotencyKey
        );
        
        transaction.setDescription("Game win - " + gameId);
        transaction.setRelatedTransactionId(betTransactionId);
        transaction.setMetadata("{\"gameId\":\"" + gameId + "\",\"betTransactionId\":\"" + betTransactionId + "\"}");
        
        // Process the win immediately
        return executeTransaction(transaction);
    }
    
    /**
     * Process bonus transaction
     */
    @Transactional
    public Transaction processBonus(String userId, CurrencyType currencyType, 
                                  BigDecimal amount, String bonusType, String idempotencyKey) {
        
        log.info("Processing bonus: userId={}, currency={}, amount={}, bonusType={}", 
                userId, currencyType, amount, bonusType);
        
        // Check idempotency
        if (idempotencyService.isDuplicateRequest(idempotencyKey)) {
            Transaction existingTransaction = findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException(idempotencyKey, null));
            throw new IdempotencyException(idempotencyKey, existingTransaction.getId());
        }
        
        // Get user account
        Account account = accountService.findByUserIdAndCurrency(userId, currencyType)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Create transaction
        Transaction transaction = createTransaction(
            account.getUser(), 
            account, 
            TransactionType.BONUS, 
            currencyType, 
            amount, 
            idempotencyKey
        );
        
        transaction.setDescription("Bonus: " + bonusType);
        transaction.setMetadata("{\"bonusType\":\"" + bonusType + "\"}");
        
        // Process the bonus immediately
        return executeTransaction(transaction);
    }
    
    /**
     * Execute transaction (credit or debit account)
     */
    @Transactional
    public Transaction executeTransaction(Transaction transaction) {
        log.info("Executing transaction: transactionId={}, type={}, amount={}", 
                transaction.getId(), transaction.getTransactionType(), transaction.getAmount());
        
        Account account = transaction.getAccount();
        BigDecimal balanceBefore = account.getBalance();
        
        try {
            // Update account balance based on transaction type
            if (transaction.isCredit()) {
                accountService.creditBalance(account, transaction.getAmount(), 
                    "Transaction: " + transaction.getId());
            } else {
                accountService.debitBalance(account, transaction.getAmount(), 
                    "Transaction: " + transaction.getId());
            }
            
            // Refresh account to get updated balance
            account = accountService.findById(account.getId()).orElseThrow();
            BigDecimal balanceAfter = account.getBalance();
            
            // Mark transaction as completed
            transaction.markCompleted(balanceBefore, balanceAfter);
            transaction = transactionRepository.save(transaction);
            
            // Create balance snapshot
            balanceService.createTransactionSnapshot(account, transaction, balanceBefore);
            
            // Store idempotency record
            idempotencyService.storeIdempotencyRecord(
                transaction.getIdempotencyKey(), 
                transaction.getId()
            );
            
            // Update daily totals
            updateDailyTotals(account, transaction);
            
            auditService.logTransactionExecuted(transaction, balanceBefore, balanceAfter);
            
            log.info("Transaction executed successfully: transactionId={}, balanceBefore={}, balanceAfter={}", 
                    transaction.getId(), balanceBefore, balanceAfter);
            
            return transaction;
            
        } catch (Exception e) {
            log.error("Transaction execution failed: transactionId={}", transaction.getId(), e);
            
            transaction.markFailed(e.getMessage());
            transactionRepository.save(transaction);
            
            auditService.logTransactionFailed(transaction, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Approve transaction
     */
    @Transactional
    public Transaction approveTransaction(String transactionId, String approvalNotes) {
        Transaction transaction = findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        if (!transaction.needsApproval()) {
            throw new IllegalStateException("Transaction does not require approval");
        }
        
        String approvedBy = SecurityContext.getCurrentUserId();
        transaction.approve(approvedBy, approvalNotes);
        transaction = transactionRepository.save(transaction);
        
        // Execute the approved transaction
        return executeTransaction(transaction);
    }
    
    /**
     * Reject transaction
     */
    @Transactional
    public Transaction rejectTransaction(String transactionId, String rejectionReason) {
        Transaction transaction = findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        if (!transaction.needsApproval()) {
            throw new IllegalStateException("Transaction does not require approval");
        }
        
        String rejectedBy = SecurityContext.getCurrentUserId();
        transaction.reject(rejectedBy, rejectionReason);
        
        // Release any held balance
        if (transaction.getTransactionType() == TransactionType.WITHDRAWAL) {
            accountService.releaseHeldBalance(
                transaction.getAccount().getId(), 
                transaction.getAmount(), 
                "Transaction rejected"
            );
        }
        
        transaction = transactionRepository.save(transaction);
        
        auditService.logTransactionRejected(transaction, rejectionReason);
        
        return transaction;
    }
    
    /**
     * Create transaction entity
     */
    private Transaction createTransaction(User user, Account account, TransactionType type, 
                                       CurrencyType currency, BigDecimal amount, String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setId(IdGenerator.generateId("TXN"));
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setCurrencyType(currency);
        transaction.setAmount(MoneyUtil.normalize(amount));
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Check if transaction requires approval
     */
    private boolean requiresApproval(Transaction transaction) {
        // Check dual approval threshold
        if (transaction.getAmount().compareTo(ComplianceConstants.DUAL_APPROVAL_THRESHOLD) >= 0) {
            return true;
        }
        
        // Check if transaction type requires approval
        return transaction.getTransactionType().isRequiresApproval();
    }
    
    /**
     * Validate deposit amount
     */
    private void validateDepositAmount(BigDecimal amount, User user) {
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        
        if (normalizedAmount.compareTo(ComplianceConstants.MIN_DEPOSIT_AMOUNT) < 0) {
            throw new IllegalArgumentException("Deposit amount below minimum: " + ComplianceConstants.MIN_DEPOSIT_AMOUNT);
        }
        
        if (normalizedAmount.compareTo(ComplianceConstants.MAX_DEPOSIT_AMOUNT) > 0) {
            throw new IllegalArgumentException("Deposit amount exceeds maximum: " + ComplianceConstants.MAX_DEPOSIT_AMOUNT);
        }
        
        // Check daily deposit limit
        BigDecimal dailyTotal = transactionRepository.getUserDailyDepositTotal(user.getId());
        if (dailyTotal.add(normalizedAmount).compareTo(ComplianceConstants.DAILY_DEPOSIT_LIMIT) > 0) {
            throw new IllegalArgumentException("Daily deposit limit exceeded");
        }
    }
    
    /**
     * Validate withdrawal eligibility
     */
    private void validateWithdrawalEligibility(Account account, BigDecimal amount) {
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        
        // Check currency is withdrawable
        account.getCurrencyType().validateWithdrawable();
        
        // Check account can withdraw
        if (!account.canWithdraw()) {
            throw new IllegalStateException("Account not eligible for withdrawals");
        }
        
        // Check minimum withdrawal amount
        if (normalizedAmount.compareTo(ComplianceConstants.MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException("Withdrawal amount below minimum: " + ComplianceConstants.MIN_WITHDRAWAL_AMOUNT);
        }
        
        // Check maximum withdrawal amount
        if (normalizedAmount.compareTo(ComplianceConstants.MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds maximum: " + ComplianceConstants.MAX_WITHDRAWAL_AMOUNT);
        }
        
        // Check daily withdrawal limit
        BigDecimal dailyTotal = transactionRepository.getUserDailyWithdrawalTotal(account.getUser().getId());
        if (dailyTotal.add(normalizedAmount).compareTo(ComplianceConstants.DAILY_WITHDRAWAL_LIMIT) > 0) {
            throw new IllegalArgumentException("Daily withdrawal limit exceeded");
        }
        
        // Check sufficient balance
        if (account.getAvailableBalance().compareTo(normalizedAmount) < 0) {
            throw new InsufficientBalanceException(
                account.getUser().getId(),
                account.getCurrencyType(),
                normalizedAmount,
                account.getAvailableBalance()
            );
        }
    }
    
    /**
     * Validate bet eligibility
     */
    private void validateBetEligibility(Account account, BigDecimal amount) {
        BigDecimal normalizedAmount = MoneyUtil.normalize(amount);
        
        // Check user is active and not self-excluded
        if (!account.getUser().isActive() || account.getUser().isSelfExcluded()) {
            throw new IllegalStateException("User not eligible for betting");
        }
        
        // Check sufficient balance
        if (account.getAvailableBalance().compareTo(normalizedAmount) < 0) {
            throw new InsufficientBalanceException(
                account.getUser().getId(),
                account.getCurrencyType(),
                normalizedAmount,
                account.getAvailableBalance()
            );
        }
        
        // Check state restrictions for Sweeps
        if (account.getCurrencyType() == CurrencyType.SWEEPS) {
            complianceService.validateStateRestrictions(account.getUser().getStateCode(), account.getUser().getId());
        }
    }
    
    /**
     * Update daily totals for account
     */
    private void updateDailyTotals(Account account, Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
            account.addDailyDeposit(transaction.getAmount());
        } else if (transaction.getTransactionType() == TransactionType.WITHDRAWAL) {
            account.addDailyWithdrawal(transaction.getAmount());
        }
        
        accountService.resetDailyTotalsIfNeeded(account);
    }
    
    /**
     * Find pending transactions
     */
    public List<Transaction> findPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }
    
    /**
     * Find transactions requiring approval
     */
    public List<Transaction> findTransactionsRequiringApproval() {
        return transactionRepository.findTransactionsRequiringApproval();
    }
    
    /**
     * Find stale pending transactions
     */
    public List<Transaction> findStalePendingTransactions(int hoursOld) {
        Instant cutoffTime = Instant.now().minusSeconds(hoursOld * 60 * 60);
        return transactionRepository.findStalePendingTransactions(cutoffTime);
    }
}
