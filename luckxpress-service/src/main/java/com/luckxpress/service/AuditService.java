package com.luckxpress.service;

import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.core.mdc.RequestContext;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Audit Service
 * CRITICAL: Provides comprehensive audit logging for compliance and security
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    
    /**
     * Create compliance audit entry
     */
    public ComplianceAudit createComplianceAudit(ComplianceAudit.EventType eventType,
                                               ComplianceAudit.Severity severity,
                                               String description,
                                               User user) {
        
        ComplianceAudit audit = ComplianceAudit.create(eventType, severity, description, user);
        audit.setRequestId(RequestContext.getRequestId());
        audit.setIpAddress(RequestContext.getProperty(RequestContext.IP_ADDRESS));
        audit.setUserAgent(RequestContext.getProperty(RequestContext.USER_AGENT));
        audit.setSessionId(RequestContext.getProperty(RequestContext.SESSION_ID));
        
        // This would save to ComplianceAuditRepository when implemented
        log.info("Compliance audit created: eventType={}, severity={}, userId={}, description={}", 
                eventType, severity, user != null ? user.getId() : null, description);
        
        return audit;
    }
    
    /**
     * Log user creation
     */
    public void logUserCreated(User user, String ipAddress) {
        log.info("AUDIT: User created - userId={}, username={}, email={}, state={}, ip={}", 
                user.getId(), user.getUsername(), user.getEmail(), user.getStateCode(), ipAddress);
        
        createComplianceAudit(
            ComplianceAudit.EventType.GDPR_REQUEST_RECEIVED, // Placeholder event type
            ComplianceAudit.Severity.LOW,
            "User account created: " + user.getUsername(),
            user
        );
    }
    
    /**
     * Log user profile update
     */
    public void logUserProfileUpdated(User user) {
        log.info("AUDIT: User profile updated - userId={}, username={}", 
                user.getId(), user.getUsername());
    }
    
    /**
     * Log email verification
     */
    public void logEmailVerified(User user) {
        log.info("AUDIT: Email verified - userId={}, email={}", 
                user.getId(), user.getEmail());
    }
    
    /**
     * Log phone verification
     */
    public void logPhoneVerified(User user) {
        log.info("AUDIT: Phone verified - userId={}, phone={}", 
                user.getId(), user.getPhoneNumber());
    }
    
    /**
     * Log successful login
     */
    public void logSuccessfulLogin(User user, String ipAddress) {
        log.info("AUDIT: Successful login - userId={}, username={}, ip={}", 
                user.getId(), user.getUsername(), ipAddress);
    }
    
    /**
     * Log failed login
     */
    public void logFailedLogin(User user) {
        log.warn("AUDIT: Failed login - userId={}, username={}, attempts={}", 
                user.getId(), user.getUsername(), user.getFailedLoginAttempts());
        
        if (user.getFailedLoginAttempts() >= 3) {
            createComplianceAudit(
                ComplianceAudit.EventType.MULTIPLE_FAILED_LOGINS,
                ComplianceAudit.Severity.MEDIUM,
                "Multiple failed login attempts: " + user.getFailedLoginAttempts(),
                user
            );
        }
    }
    
    /**
     * Log self-exclusion set
     */
    public void logSelfExclusionSet(User user, Instant exclusionUntil) {
        log.info("AUDIT: Self-exclusion set - userId={}, until={}", 
                user.getId(), exclusionUntil);
        
        createComplianceAudit(
            ComplianceAudit.EventType.SELF_EXCLUDED_USER_ACCESS,
            ComplianceAudit.Severity.MEDIUM,
            "Self-exclusion period set until: " + exclusionUntil,
            user
        );
    }
    
    /**
     * Log role added
     */
    public void logRoleAdded(User user, User.UserRole role) {
        log.info("AUDIT: Role added - userId={}, role={}, addedBy={}", 
                user.getId(), role, SecurityContext.getCurrentUserId());
    }
    
    /**
     * Log role removed
     */
    public void logRoleRemoved(User user, User.UserRole role) {
        log.info("AUDIT: Role removed - userId={}, role={}, removedBy={}", 
                user.getId(), role, SecurityContext.getCurrentUserId());
    }
    
    /**
     * Log user suspended
     */
    public void logUserSuspended(User user, String reason) {
        log.warn("AUDIT: User suspended - userId={}, reason={}, suspendedBy={}", 
                user.getId(), reason, SecurityContext.getCurrentUserId());
        
        createComplianceAudit(
            ComplianceAudit.EventType.ACCOUNT_TAKEOVER_ATTEMPT,
            ComplianceAudit.Severity.HIGH,
            "User account suspended: " + reason,
            user
        );
    }
    
    /**
     * Log user reactivated
     */
    public void logUserReactivated(User user, String reason) {
        log.info("AUDIT: User reactivated - userId={}, reason={}, reactivatedBy={}", 
                user.getId(), reason, SecurityContext.getCurrentUserId());
    }
    
    /**
     * Log accounts created
     */
    public void logAccountsCreated(User user, List<Account> accounts) {
        log.info("AUDIT: Accounts created - userId={}, accountCount={}", 
                user.getId(), accounts.size());
        
        for (Account account : accounts) {
            log.info("AUDIT: Account created - accountId={}, currency={}, userId={}", 
                    account.getId(), account.getCurrencyType(), user.getId());
        }
    }
    
    /**
     * Log balance credited
     */
    public void logBalanceCredited(Account account, BigDecimal amount, 
                                 BigDecimal balanceBefore, BigDecimal balanceAfter, String reason) {
        log.info("AUDIT: Balance credited - accountId={}, currency={}, amount={}, before={}, after={}, reason={}", 
                account.getId(), account.getCurrencyType(), amount, balanceBefore, balanceAfter, reason);
    }
    
    /**
     * Log balance debited
     */
    public void logBalanceDebited(Account account, BigDecimal amount, 
                                BigDecimal balanceBefore, BigDecimal balanceAfter, String reason) {
        log.info("AUDIT: Balance debited - accountId={}, currency={}, amount={}, before={}, after={}, reason={}", 
                account.getId(), account.getCurrencyType(), amount, balanceBefore, balanceAfter, reason);
    }
    
    /**
     * Log balance held
     */
    public void logBalanceHeld(Account account, BigDecimal amount, String reason) {
        log.info("AUDIT: Balance held - accountId={}, currency={}, amount={}, reason={}", 
                account.getId(), account.getCurrencyType(), amount, reason);
    }
    
    /**
     * Log balance released
     */
    public void logBalanceReleased(Account account, BigDecimal amount, String reason) {
        log.info("AUDIT: Balance released - accountId={}, currency={}, amount={}, reason={}", 
                account.getId(), account.getCurrencyType(), amount, reason);
    }
    
    /**
     * Log balance confirmed
     */
    public void logBalanceConfirmed(Account account, BigDecimal amount, 
                                  BigDecimal balanceBefore, BigDecimal balanceAfter, String reason) {
        log.info("AUDIT: Balance confirmed - accountId={}, currency={}, amount={}, before={}, after={}, reason={}", 
                account.getId(), account.getCurrencyType(), amount, balanceBefore, balanceAfter, reason);
    }
    
    /**
     * Log account frozen
     */
    public void logAccountFrozen(Account account, Instant frozenUntil, String reason) {
        log.warn("AUDIT: Account frozen - accountId={}, until={}, reason={}, frozenBy={}", 
                account.getId(), frozenUntil, reason, SecurityContext.getCurrentUserId());
        
        createComplianceAudit(
            ComplianceAudit.EventType.ACCOUNT_TAKEOVER_ATTEMPT,
            ComplianceAudit.Severity.HIGH,
            "Account frozen: " + reason,
            account.getUser()
        );
    }
    
    /**
     * Log account unfrozen
     */
    public void logAccountUnfrozen(Account account, String reason) {
        log.info("AUDIT: Account unfrozen - accountId={}, reason={}, unfrozenBy={}", 
                account.getId(), reason, SecurityContext.getCurrentUserId());
    }
    
    /**
     * Log transaction executed
     */
    public void logTransactionExecuted(Transaction transaction, BigDecimal balanceBefore, BigDecimal balanceAfter) {
        log.info("AUDIT: Transaction executed - transactionId={}, type={}, currency={}, amount={}, before={}, after={}, userId={}", 
                transaction.getId(), transaction.getTransactionType(), transaction.getCurrencyType(), 
                transaction.getAmount(), balanceBefore, balanceAfter, transaction.getUser().getId());
    }
    
    /**
     * Log transaction failed
     */
    public void logTransactionFailed(Transaction transaction, String reason) {
        log.error("AUDIT: Transaction failed - transactionId={}, type={}, amount={}, reason={}, userId={}", 
                transaction.getId(), transaction.getTransactionType(), transaction.getAmount(), 
                reason, transaction.getUser().getId());
    }
    
    /**
     * Log transaction rejected
     */
    public void logTransactionRejected(Transaction transaction, String reason) {
        log.warn("AUDIT: Transaction rejected - transactionId={}, type={}, amount={}, reason={}, rejectedBy={}", 
                transaction.getId(), transaction.getTransactionType(), transaction.getAmount(), 
                reason, SecurityContext.getCurrentUserId());
    }
    
    /**
     * Log withdrawal attempted
     */
    public void logWithdrawalAttempted(User user, Transaction withdrawal, String paymentMethod) {
        log.info("AUDIT: Withdrawal attempted - userId={}, transactionId={}, amount={}, currency={}, paymentMethod={}", 
                user.getId(), withdrawal.getId(), withdrawal.getAmount(), withdrawal.getCurrencyType(), paymentMethod);
        
        // Create compliance audit for large withdrawals
        if (withdrawal.getAmount().compareTo(new BigDecimal("1000.0000")) >= 0) {
            createComplianceAudit(
                ComplianceAudit.EventType.LARGE_TRANSACTION_DETECTED,
                ComplianceAudit.Severity.MEDIUM,
                "Large withdrawal attempted: " + withdrawal.getAmount(),
                user
            );
        }
    }
    
    /**
     * Log KYC submitted
     */
    public void logKycSubmitted(User user, KycVerification verification) {
        log.info("AUDIT: KYC submitted - userId={}, verificationId={}, type={}", 
                user.getId(), verification.getId(), verification.getVerificationType());
    }
    
    /**
     * Log KYC approved
     */
    public void logKycApproved(User user, KycVerification verification, String approvedBy) {
        log.info("AUDIT: KYC approved - userId={}, verificationId={}, approvedBy={}", 
                user.getId(), verification.getId(), approvedBy);
    }
    
    /**
     * Log KYC rejected
     */
    public void logKycRejected(User user, KycVerification verification, String rejectedBy, String reason) {
        log.warn("AUDIT: KYC rejected - userId={}, verificationId={}, rejectedBy={}, reason={}", 
                user.getId(), verification.getId(), rejectedBy, reason);
        
        createComplianceAudit(
            ComplianceAudit.EventType.KYC_VERIFICATION_FAILED,
            ComplianceAudit.Severity.MEDIUM,
            "KYC verification rejected: " + reason,
            user
        );
    }
    
    /**
     * Log KYC requires manual review
     */
    public void logKycRequiresManualReview(User user, KycVerification verification, String reason) {
        log.info("AUDIT: KYC requires manual review - userId={}, verificationId={}, reason={}", 
                user.getId(), verification.getId(), reason);
    }
    
    /**
     * Log approval workflow created
     */
    public void logApprovalWorkflowCreated(ApprovalWorkflow workflow, Transaction transaction) {
        log.info("AUDIT: Approval workflow created - workflowId={}, type={}, transactionId={}, amount={}, requiredApprovals={}", 
                workflow.getId(), workflow.getApprovalType(), transaction.getId(), 
                workflow.getAmount(), workflow.getRequiredApprovals());
    }
    
    /**
     * Log approval workflow completed
     */
    public void logApprovalWorkflowCompleted(ApprovalWorkflow workflow, String completedBy) {
        log.info("AUDIT: Approval workflow completed - workflowId={}, completedBy={}, receivedApprovals={}", 
                workflow.getId(), completedBy, workflow.getReceivedApprovals());
    }
    
    /**
     * Log approval submitted
     */
    public void logApprovalSubmitted(ApprovalWorkflow workflow, String approver, String notes) {
        log.info("AUDIT: Approval submitted - workflowId={}, approver={}, remaining={}, notes={}", 
                workflow.getId(), approver, workflow.getRemainingApprovals(), notes);
    }
    
    /**
     * Log approval workflow rejected
     */
    public void logApprovalWorkflowRejected(ApprovalWorkflow workflow, String rejectedBy, String reason) {
        log.warn("AUDIT: Approval workflow rejected - workflowId={}, rejectedBy={}, reason={}", 
                workflow.getId(), rejectedBy, reason);
    }
    
    /**
     * Log approval workflow escalated
     */
    public void logApprovalWorkflowEscalated(ApprovalWorkflow workflow, String escalatedBy, String reason) {
        log.warn("AUDIT: Approval workflow escalated - workflowId={}, escalatedBy={}, reason={}", 
                workflow.getId(), escalatedBy, reason);
    }
    
    /**
     * Log approval workflow cancelled
     */
    public void logApprovalWorkflowCancelled(ApprovalWorkflow workflow, String cancelledBy, String reason) {
        log.info("AUDIT: Approval workflow cancelled - workflowId={}, cancelledBy={}, reason={}", 
                workflow.getId(), cancelledBy, reason);
    }
    
    /**
     * Log approval workflow expired
     */
    public void logApprovalWorkflowExpired(ApprovalWorkflow workflow) {
        log.warn("AUDIT: Approval workflow expired - workflowId={}, transactionId={}", 
                workflow.getId(), workflow.getTransactionId());
    }
    
    /**
     * Log daily snapshot created
     */
    public void logDailySnapshotCreated(Account account) {
        log.debug("AUDIT: Daily snapshot created - accountId={}, balance={}", 
                account.getId(), account.getBalance());
    }
    
    /**
     * Log manual adjustment
     */
    public void logManualAdjustment(Account account, BigDecimal amount, boolean isCredit, 
                                  String reason, BigDecimal balanceBefore, BigDecimal balanceAfter) {
        log.warn("AUDIT: Manual adjustment - accountId={}, amount={}, isCredit={}, reason={}, before={}, after={}, adjustedBy={}", 
                account.getId(), amount, isCredit, reason, balanceBefore, balanceAfter, SecurityContext.getCurrentUserId());
        
        createComplianceAudit(
            ComplianceAudit.EventType.SYSTEM_CORRECTION,
            ComplianceAudit.Severity.HIGH,
            "Manual balance adjustment: " + (isCredit ? "+" : "-") + amount + " - " + reason,
            account.getUser()
        );
    }
    
    /**
     * Log balance integrity issue
     */
    public void logBalanceIntegrityIssue(Account account, BigDecimal ledgerBalance, BigDecimal accountBalance) {
        log.error("AUDIT: Balance integrity issue - accountId={}, ledgerBalance={}, accountBalance={}, difference={}", 
                account.getId(), ledgerBalance, accountBalance, ledgerBalance.subtract(accountBalance));
        
        createComplianceAudit(
            ComplianceAudit.EventType.DATA_BREACH_DETECTED,
            ComplianceAudit.Severity.CRITICAL,
            "Balance integrity issue detected - difference: " + ledgerBalance.subtract(accountBalance),
            account.getUser()
        );
    }
    
    /**
     * Log reconciliation adjustment
     */
    public void logReconciliationAdjustment(Account account, BigDecimal difference, 
                                          BigDecimal ledgerBalance, BigDecimal accountBalance) {
        log.warn("AUDIT: Reconciliation adjustment - accountId={}, difference={}, ledgerBalance={}, accountBalance={}", 
                account.getId(), difference, ledgerBalance, accountBalance);
    }
    
    /**
     * Log daily reconciliation completed
     */
    public void logDailyReconciliationCompleted(int reconciledCount, int issuesFound) {
        log.info("AUDIT: Daily reconciliation completed - reconciledAccounts={}, issuesFound={}", 
                reconciledCount, issuesFound);
    }
    
    /**
     * Log ledger entry reversed
     */
    public void logLedgerEntryReversed(LedgerEntry originalEntry, LedgerEntry reversalEntry, String reason) {
        log.warn("AUDIT: Ledger entry reversed - originalEntryId={}, reversalEntryId={}, reason={}, reversedBy={}", 
                originalEntry.getId(), reversalEntry.getId(), reason, SecurityContext.getCurrentUserId());
        
        createComplianceAudit(
            ComplianceAudit.EventType.SYSTEM_CORRECTION,
            ComplianceAudit.Severity.HIGH,
            "Ledger entry reversed: " + reason,
            originalEntry.getUser()
        );
    }
    
    /**
     * Create audit trail entry with full context
     */
    public String createAuditTrail(String action, String entityType, String entityId, String details) {
        String auditEntry = RequestContext.createAuditEntry(action, details);
        
        log.info("AUDIT_TRAIL: {} - EntityType: {}, EntityId: {}, Details: {}", 
                action, entityType, entityId, details);
        
        return auditEntry;
    }
    
    /**
     * Create compliance violation entry
     */
    public String createComplianceViolation(String violation, String details) {
        String complianceEntry = RequestContext.createComplianceEntry(violation, details);
        
        log.error("COMPLIANCE_VIOLATION: {} - Details: {}", violation, details);
        
        return complianceEntry;
    }
}
