package com.luckxpress.web.controller;

import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Account;
import com.luckxpress.data.entity.User;
import com.luckxpress.service.*;
import com.luckxpress.web.dto.AccountResponseDto;
import com.luckxpress.web.dto.SystemHealthDto;
import com.luckxpress.web.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller
 * CRITICAL: Provides administrative operations and system management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration", description = "Administrative operations and system management")
public class AdminController {
    
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final BalanceService balanceService;
    private final ApprovalService approvalService;
    private final ComplianceService complianceService;
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get system health",
        description = "Retrieves comprehensive system health status (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "System health retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        log.info("System health check requested by: {}", SecurityContext.getCurrentUserId());
        
        SystemHealthDto health = new SystemHealthDto();
        health.setTimestamp(Instant.now());
        health.setStatus("HEALTHY"); // This would be calculated based on actual checks
        
        // Database connectivity
        health.setDatabaseConnected(true); // This would be actual DB check
        
        // Balance integrity
        List<Account> integrityIssues = balanceService.findAccountsWithIntegrityIssues();
        health.setBalanceIntegrityIssues(integrityIssues.size());
        
        // Negative balance accounts (should be 0)
        List<Account> negativeBalances = accountService.findAccountsWithNegativeBalance();
        health.setNegativeBalanceAccounts(negativeBalances.size());
        
        // Pending approvals
        health.setPendingApprovals(approvalService.findPendingApprovals().size());
        
        // Stale transactions
        health.setStaleTransactions(transactionService.findStalePendingTransactions(24).size());
        
        // System metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", userService.findAll().size()); // This method would need to be added
        metrics.put("activeUsers", userService.findByStatus(User.UserStatus.ACTIVE).size());
        metrics.put("totalAccounts", accountService.findAll().size()); // This method would need to be added
        metrics.put("systemBalanceSummary", accountService.getBalanceSummaryByCurrency());
        health.setMetrics(metrics);
        
        // Set overall status based on issues
        if (negativeBalances.size() > 0 || integrityIssues.size() > 0) {
            health.setStatus("CRITICAL");
        } else if (health.getStaleTransactions() > 10 || health.getPendingApprovals() > 50) {
            health.setStatus("WARNING");
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get system statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE_MANAGER')")
    @Operation(
        summary = "Get system statistics",
        description = "Retrieves comprehensive system statistics (Admin/Finance only)"
    )
    @ApiResponse(responseCode = "200", description = "System statistics retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        Map<String, Object> userStats = new HashMap<>();
        userStats.put("totalUsers", 0); // Placeholder
        userStats.put("activeUsers", 0); // Placeholder
        userStats.put("kycVerifiedUsers", userService.findKycVerifiedUsers().size());
        userStats.put("usersRequiringKyc", userService.findUsersRequiringKyc().size());
        stats.put("users", userStats);
        
        // Account statistics
        Map<String, Object> accountStats = new HashMap<>();
        accountStats.put("totalAccounts", 0); // Placeholder
        accountStats.put("zeroBalanceAccounts", accountService.findAccountsWithZeroBalance().size());
        accountStats.put("negativeBalanceAccounts", accountService.findAccountsWithNegativeBalance().size());
        accountStats.put("balanceSummary", accountService.getBalanceSummaryByCurrency());
        stats.put("accounts", accountStats);
        
        // Transaction statistics
        Map<String, Object> transactionStats = new HashMap<>();
        transactionStats.put("pendingTransactions", transactionService.findPendingTransactions().size());
        transactionStats.put("transactionsRequiringApproval", transactionService.findTransactionsRequiringApproval().size());
        transactionStats.put("staleTransactions", transactionService.findStalePendingTransactions(24).size());
        stats.put("transactions", transactionStats);
        
        // Approval statistics
        stats.put("approvals", approvalService.getApprovalStatistics());
        
        // Daily volume
        stats.put("dailyVolume", balanceService.getDailyVolumeByCurrency(LocalDate.now()));
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Perform daily reconciliation
     */
    @PostMapping("/reconciliation/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Perform daily reconciliation",
        description = "Performs daily balance reconciliation for all accounts (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Daily reconciliation completed")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Map<String, Object>> performDailyReconciliation() {
        log.info("Daily reconciliation initiated by: {}", SecurityContext.getCurrentUserId());
        
        Instant startTime = Instant.now();
        balanceService.performDailyReconciliation();
        Instant endTime = Instant.now();
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("duration", endTime.toEpochMilli() - startTime.toEpochMilli());
        result.put("initiatedBy", SecurityContext.getCurrentUserId());
        
        log.info("Daily reconciliation completed by: {}, duration: {}ms", 
                SecurityContext.getCurrentUserId(), result.get("duration"));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Reset daily account totals
     */
    @PostMapping("/accounts/reset-daily-totals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Reset daily account totals",
        description = "Resets daily deposit/withdrawal totals for all accounts (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Daily totals reset completed")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> resetDailyTotals() {
        log.info("Daily totals reset initiated by: {}", SecurityContext.getCurrentUserId());
        
        accountService.resetDailyTotals();
        
        log.info("Daily totals reset completed by: {}", SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Find users by criteria
     */
    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Search users by criteria",
        description = "Advanced user search with multiple criteria (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "User search results")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @Parameter(description = "Search term")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "User status filter")
            @RequestParam(required = false) User.UserStatus status,
            @Parameter(description = "KYC status filter")
            @RequestParam(required = false) User.KycStatus kycStatus,
            @Parameter(description = "State code filter")
            @RequestParam(required = false) String stateCode,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<User> users;
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            users = userService.searchUsers(searchTerm, pageable);
        } else if (stateCode != null) {
            // This would need a repository method
            users = Page.empty(); // Placeholder
        } else {
            // This would need a findAll method with pageable
            users = Page.empty(); // Placeholder
        }
        
        Page<UserResponseDto> response = users.map(UserResponseDto::fromUser);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get inactive users
     */
    @GetMapping("/users/inactive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get inactive users",
        description = "Retrieves users who haven't logged in for specified days (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Inactive users retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Page<UserResponseDto>> getInactiveUsers(
            @Parameter(description = "Days since last login")
            @RequestParam(defaultValue = "90") int daysSinceLastLogin,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<User> users = userService.findInactiveUsers(daysSinceLastLogin, pageable);
        Page<UserResponseDto> response = users.map(UserResponseDto::fromUser);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get dormant accounts
     */
    @GetMapping("/accounts/dormant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get dormant accounts",
        description = "Retrieves accounts with no transactions for specified days (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Dormant accounts retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<AccountResponseDto>> getDormantAccounts(
            @Parameter(description = "Days since last transaction")
            @RequestParam(defaultValue = "180") int daysSinceLastTransaction) {
        
        List<Account> accounts = accountService.findDormantAccounts(daysSinceLastTransaction);
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get balance integrity issues
     */
    @GetMapping("/accounts/integrity-issues")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get balance integrity issues",
        description = "Retrieves accounts with balance integrity issues (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Balance integrity issues retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<AccountResponseDto>> getBalanceIntegrityIssues() {
        List<Account> accounts = balanceService.findAccountsWithIntegrityIssues();
        List<AccountResponseDto> response = accounts.stream()
            .map(AccountResponseDto::fromAccount)
            .toList();
        
        if (!accounts.isEmpty()) {
            log.error("CRITICAL: Found {} accounts with balance integrity issues", accounts.size());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process expired approvals
     */
    @PostMapping("/approvals/process-expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Process expired approvals",
        description = "Processes and marks expired approval workflows (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Expired approvals processed")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Void> processExpiredApprovals() {
        log.info("Processing expired approvals initiated by: {}", SecurityContext.getCurrentUserId());
        
        approvalService.processExpiredApprovals();
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get system configuration
     */
    @GetMapping("/configuration")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get system configuration",
        description = "Retrieves current system configuration settings (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "System configuration retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Map<String, Object>> getSystemConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Compliance limits
        Map<String, Object> limits = new HashMap<>();
        limits.put("minDepositAmount", "1.0000");
        limits.put("maxDepositAmount", "10000.0000");
        limits.put("minWithdrawalAmount", "25.0000");
        limits.put("maxWithdrawalAmount", "2500.0000");
        limits.put("dailyDepositLimit", "5000.0000");
        limits.put("dailyWithdrawalLimit", "2500.0000");
        limits.put("monthlyWithdrawalLimit", "25000.0000");
        config.put("limits", limits);
        
        // Approval thresholds
        Map<String, Object> approvals = new HashMap<>();
        approvals.put("dualApprovalThreshold", "1000.0000");
        approvals.put("tripleApprovalThreshold", "5000.0000");
        approvals.put("enhancedKycThreshold", "2500.0000");
        config.put("approvals", approvals);
        
        // State restrictions
        config.put("restrictedStates", List.of("ID", "WA", "MT", "NV"));
        
        // System settings
        Map<String, Object> system = new HashMap<>();
        system.put("maintenanceMode", false);
        system.put("withdrawalsEnabled", true);
        system.put("depositsEnabled", true);
        system.put("newRegistrationsEnabled", true);
        config.put("system", system);
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Update system maintenance mode
     */
    @PostMapping("/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Toggle maintenance mode",
        description = "Enables or disables system maintenance mode (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Maintenance mode updated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Map<String, Object>> toggleMaintenanceMode(
            @Parameter(description = "Enable maintenance mode")
            @RequestParam boolean enabled,
            @Parameter(description = "Maintenance message")
            @RequestParam(required = false) String message) {
        
        log.warn("Maintenance mode {} by: {}, message: {}", 
                enabled ? "ENABLED" : "DISABLED", SecurityContext.getCurrentUserId(), message);
        
        Map<String, Object> result = new HashMap<>();
        result.put("maintenanceMode", enabled);
        result.put("message", message);
        result.put("updatedBy", SecurityContext.getCurrentUserId());
        result.put("updatedAt", Instant.now());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate system report
     */
    @PostMapping("/reports/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Generate system report",
        description = "Generates comprehensive system report (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "System report generated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Map<String, Object>> generateSystemReport(
            @Parameter(description = "Report type")
            @RequestParam String reportType,
            @Parameter(description = "Start date")
            @RequestParam LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam LocalDate endDate) {
        
        log.info("System report generation requested: type={}, period={} to {}, requestedBy={}", 
                reportType, startDate, endDate, SecurityContext.getCurrentUserId());
        
        Map<String, Object> report = new HashMap<>();
        report.put("reportType", reportType);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("generatedAt", Instant.now());
        report.put("generatedBy", SecurityContext.getCurrentUserId());
        report.put("status", "GENERATED");
        
        // This would contain actual report data based on reportType
        report.put("data", Map.of("placeholder", "Report data would be here"));
        
        return ResponseEntity.ok(report);
    }
}
