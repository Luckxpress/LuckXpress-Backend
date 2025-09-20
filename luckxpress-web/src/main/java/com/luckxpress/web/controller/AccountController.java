package com.luckxpress.web.controller;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Account;
import com.luckxpress.service.AccountService;
import com.luckxpress.service.BalanceService;
import com.luckxpress.web.dto.AccountResponseDto;
import com.luckxpress.web.dto.BalanceSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Account Controller
 * CRITICAL: Provides account and balance management REST endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account Management", description = "Account and balance operations")
public class AccountController {
    
    private final AccountService accountService;
    private final BalanceService balanceService;
    
    /**
     * Get user accounts
     */
    @GetMapping
    @Operation(
        summary = "Get user accounts",
        description = "Retrieves all accounts for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<List<AccountResponseDto>> getUserAccounts() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        List<Account> accounts = accountService.findUserAccounts(currentUserId);
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get specific account by currency
     */
    @GetMapping("/{currencyType}")
    @Operation(
        summary = "Get account by currency",
        description = "Retrieves user account for specific currency type"
    )
    @ApiResponse(responseCode = "200", description = "Account retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<AccountResponseDto> getAccountByCurrency(
            @Parameter(description = "Currency type (GOLD or SWEEPS)")
            @PathVariable CurrencyType currencyType) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        Optional<Account> account = accountService.findByUserIdAndCurrency(currentUserId, currencyType);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AccountResponseDto response = AccountResponseDto.fromAccount(account.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get account balance summary
     */
    @GetMapping("/balance-summary")
    @Operation(
        summary = "Get balance summary",
        description = "Retrieves balance summary for all user accounts"
    )
    @ApiResponse(responseCode = "200", description = "Balance summary retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<List<BalanceSummaryDto>> getBalanceSummary() {
        String currentUserId = SecurityContext.getCurrentUserId();
        
        List<Account> accounts = accountService.findUserAccounts(currentUserId);
        List<BalanceSummaryDto> summary = accounts.stream()
            .map(account -> new BalanceSummaryDto(
                account.getCurrencyType(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getPendingBalance(),
                account.getLastTransactionAt()
            ))
            .toList();
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get account by ID (Admin/Compliance only)
     */
    @GetMapping("/admin/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get account by ID",
        description = "Retrieves account information by ID (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<AccountResponseDto> getAccountById(
            @Parameter(description = "Account ID")
            @PathVariable String accountId) {
        
        Optional<Account> account = accountService.findById(accountId);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AccountResponseDto response = AccountResponseDto.fromAccount(account.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Freeze account (Admin/Compliance only)
     */
    @PostMapping("/admin/{accountId}/freeze")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Freeze account",
        description = "Freezes an account until specified time (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Account frozen successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> freezeAccount(
            @Parameter(description = "Account ID")
            @PathVariable String accountId,
            @Parameter(description = "Freeze until timestamp")
            @RequestParam Instant frozenUntil,
            @Parameter(description = "Freeze reason")
            @RequestParam String reason) {
        
        accountService.freezeAccount(accountId, frozenUntil, reason);
        
        log.warn("Account frozen: accountId={}, until={}, reason={}, frozenBy={}", 
                accountId, frozenUntil, reason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Unfreeze account (Admin/Compliance only)
     */
    @PostMapping("/admin/{accountId}/unfreeze")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    @Operation(
        summary = "Unfreeze account",
        description = "Unfreezes a frozen account (Compliance/Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Account unfrozen successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> unfreezeAccount(
            @Parameter(description = "Account ID")
            @PathVariable String accountId,
            @Parameter(description = "Unfreeze reason")
            @RequestParam String reason) {
        
        accountService.unfreezeAccount(accountId, reason);
        
        log.info("Account unfrozen: accountId={}, reason={}, unfrozenBy={}", 
                accountId, reason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Manual balance adjustment (Admin only)
     */
    @PostMapping("/admin/{accountId}/adjust-balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Manual balance adjustment",
        description = "Manually adjusts account balance (Admin only - use with extreme caution)"
    )
    @ApiResponse(responseCode = "200", description = "Balance adjusted successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> adjustBalance(
            @Parameter(description = "Account ID")
            @PathVariable String accountId,
            @Parameter(description = "Adjustment amount")
            @RequestParam BigDecimal amount,
            @Parameter(description = "Is credit adjustment")
            @RequestParam boolean isCredit,
            @Parameter(description = "Adjustment reason")
            @RequestParam String reason) {
        
        Optional<Account> account = accountService.findById(accountId);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        balanceService.createAdjustmentEntry(account.get(), amount, reason, isCredit);
        
        log.warn("Manual balance adjustment: accountId={}, amount={}, isCredit={}, reason={}, adjustedBy={}", 
                accountId, amount, isCredit, reason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Verify account balance integrity (Admin only)
     */
    @PostMapping("/admin/{accountId}/verify-integrity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Verify balance integrity",
        description = "Verifies account balance integrity against ledger (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Integrity verification completed")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Boolean> verifyBalanceIntegrity(
            @Parameter(description = "Account ID")
            @PathVariable String accountId) {
        
        boolean isIntegrityValid = balanceService.verifyAccountBalanceIntegrity(accountId);
        
        log.info("Balance integrity check: accountId={}, valid={}, checkedBy={}", 
                accountId, isIntegrityValid, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok(isIntegrityValid);
    }
    
    /**
     * Reconcile account balance (Admin only)
     */
    @PostMapping("/admin/{accountId}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Reconcile account balance",
        description = "Reconciles account balance with ledger entries (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Account reconciled successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> reconcileAccount(
            @Parameter(description = "Account ID")
            @PathVariable String accountId) {
        
        balanceService.reconcileAccountBalance(accountId);
        
        log.info("Account reconciled: accountId={}, reconciledBy={}", 
                accountId, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get system-wide balance summary (Admin only)
     */
    @GetMapping("/admin/system-balance-summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Get system balance summary",
        description = "Retrieves system-wide balance summary by currency (Admin/Finance only)"
    )
    @ApiResponse(responseCode = "200", description = "System balance summary retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<Object[]>> getSystemBalanceSummary() {
        List<Object[]> summary = accountService.getBalanceSummaryByCurrency();
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Find accounts with zero balance (Admin only)
     */
    @GetMapping("/admin/zero-balance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Find zero balance accounts",
        description = "Finds all accounts with zero balance (Admin/Finance only)"
    )
    @ApiResponse(responseCode = "200", description = "Zero balance accounts retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<AccountResponseDto>> getZeroBalanceAccounts() {
        List<Account> accounts = accountService.findAccountsWithZeroBalance();
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Find accounts with negative balance (Admin only)
     */
    @GetMapping("/admin/negative-balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Find negative balance accounts",
        description = "Finds all accounts with negative balance - should not exist (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Negative balance accounts retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<AccountResponseDto>> getNegativeBalanceAccounts() {
        List<Account> accounts = accountService.findAccountsWithNegativeBalance();
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        if (!accounts.isEmpty()) {
            log.error("CRITICAL: Found {} accounts with negative balance", accounts.size());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Find dormant accounts (Admin only)
     */
    @GetMapping("/admin/dormant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Find dormant accounts",
        description = "Finds accounts with no transactions in specified days (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Dormant accounts retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<AccountResponseDto>> getDormantAccounts(
            @Parameter(description = "Days since last transaction")
            @RequestParam(defaultValue = "90") int daysSinceLastTransaction) {
        
        List<Account> accounts = accountService.findDormantAccounts(daysSinceLastTransaction);
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        return ResponseEntity.ok(response);
    }
}
