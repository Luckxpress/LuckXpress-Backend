package com.luckxpress.web.controller;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.core.security.SecurityContext;
import com.luckxpress.data.entity.Transaction;
import com.luckxpress.service.TransactionService;
import com.luckxpress.web.dto.BetRequestDto;
import com.luckxpress.web.dto.BonusRequestDto;
import com.luckxpress.web.dto.DepositRequestDto;
import com.luckxpress.web.dto.TransactionResponseDto;
import com.luckxpress.web.dto.WinRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Controller
 * CRITICAL: Provides transaction processing REST endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transaction Management", description = "Financial transaction operations")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    /**
     * Process deposit
     */
    @PostMapping("/deposit")
    @Operation(
        summary = "Process deposit",
        description = "Processes a deposit transaction for the authenticated user"
    )
    @ApiResponse(responseCode = "201", description = "Deposit processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid deposit request")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "409", description = "Duplicate request")
    public ResponseEntity<TransactionResponseDto> processDeposit(
            @Valid @RequestBody DepositRequestDto depositRequest,
            HttpServletRequest request) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("Processing deposit: userId={}, amount={}, currency={}, paymentMethod={}", 
                currentUserId, depositRequest.getAmount(), depositRequest.getCurrencyType(), 
                depositRequest.getPaymentMethod());
        
        Transaction transaction = transactionService.processDeposit(
            currentUserId,
            depositRequest.getCurrencyType(),
            depositRequest.getAmount(),
            depositRequest.getPaymentMethod(),
            depositRequest.getExternalReference(),
            depositRequest.getIdempotencyKey()
        );
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Process game bet
     */
    @PostMapping("/bet")
    @Operation(
        summary = "Process game bet",
        description = "Processes a bet transaction for game play"
    )
    @ApiResponse(responseCode = "201", description = "Bet processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid bet request")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "409", description = "Duplicate request")
    public ResponseEntity<TransactionResponseDto> processBet(
            @Valid @RequestBody BetRequestDto betRequest) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        log.info("Processing bet: userId={}, amount={}, currency={}, gameId={}", 
                currentUserId, betRequest.getAmount(), betRequest.getCurrencyType(), 
                betRequest.getGameId());
        
        Transaction transaction = transactionService.processBet(
            currentUserId,
            betRequest.getCurrencyType(),
            betRequest.getAmount(),
            betRequest.getGameId(),
            betRequest.getIdempotencyKey()
        );
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Process game win
     */
    @PostMapping("/win")
    @Operation(
        summary = "Process game win",
        description = "Processes a win transaction for game results"
    )
    @ApiResponse(responseCode = "201", description = "Win processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid win request")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    @ApiResponse(responseCode = "409", description = "Duplicate request")
    public ResponseEntity<TransactionResponseDto> processWin(
            @Valid @RequestBody WinRequestDto winRequest) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        log.info("Processing win: userId={}, amount={}, currency={}, gameId={}", 
                currentUserId, winRequest.getAmount(), winRequest.getCurrencyType(), 
                winRequest.getGameId());
        
        Transaction transaction = transactionService.processWin(
            currentUserId,
            winRequest.getCurrencyType(),
            winRequest.getAmount(),
            winRequest.getGameId(),
            winRequest.getBetTransactionId(),
            winRequest.getIdempotencyKey()
        );
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Process bonus
     */
    @PostMapping("/bonus")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    @Operation(
        summary = "Process bonus",
        description = "Processes a bonus transaction (Admin/Customer Service only)"
    )
    @ApiResponse(responseCode = "201", description = "Bonus processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid bonus request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "409", description = "Duplicate request")
    public ResponseEntity<TransactionResponseDto> processBonus(
            @Valid @RequestBody BonusRequestDto bonusRequest) {
        
        log.info("Processing bonus: userId={}, amount={}, currency={}, bonusType={}, issuedBy={}", 
                bonusRequest.getUserId(), bonusRequest.getAmount(), bonusRequest.getCurrencyType(), 
                bonusRequest.getBonusType(), SecurityContext.getCurrentUserId());
        
        Transaction transaction = transactionService.processBonus(
            bonusRequest.getUserId(),
            bonusRequest.getCurrencyType(),
            bonusRequest.getAmount(),
            bonusRequest.getBonusType(),
            bonusRequest.getIdempotencyKey()
        );
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get user transactions
     */
    @GetMapping
    @Operation(
        summary = "Get user transactions",
        description = "Retrieves transaction history for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Page<TransactionResponseDto>> getUserTransactions(
            @Parameter(description = "Currency type filter")
            @RequestParam(required = false) CurrencyType currencyType,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        Page<Transaction> transactions = transactionService.findUserTransactions(currentUserId, pageable);
        Page<TransactionResponseDto> response = transactions.map(TransactionResponseDto::fromTransaction);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get transaction by ID
     */
    @GetMapping("/{transactionId}")
    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves a specific transaction by ID"
    )
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<TransactionResponseDto> getTransactionById(
            @Parameter(description = "Transaction ID")
            @PathVariable String transactionId) {
        
        String currentUserId = SecurityContext.getCurrentUserId();
        
        Optional<Transaction> transaction = transactionService.findById(transactionId);
        if (transaction.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user owns this transaction or has admin privileges
        if (!transaction.get().getUser().getId().equals(currentUserId) && 
            !SecurityContext.isAdmin() && !SecurityContext.isComplianceOfficer()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Approve transaction (Admin/Compliance only)
     */
    @PostMapping("/{transactionId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Approve transaction",
        description = "Approves a pending transaction (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Transaction approved successfully")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "400", description = "Transaction cannot be approved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<TransactionResponseDto> approveTransaction(
            @Parameter(description = "Transaction ID")
            @PathVariable String transactionId,
            @Parameter(description = "Approval notes")
            @RequestParam(required = false) String approvalNotes) {
        
        Transaction transaction = transactionService.approveTransaction(transactionId, approvalNotes);
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        log.info("Transaction approved: transactionId={}, approvedBy={}", 
                transactionId, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reject transaction (Admin/Compliance only)
     */
    @PostMapping("/{transactionId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Reject transaction",
        description = "Rejects a pending transaction (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Transaction rejected successfully")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "400", description = "Transaction cannot be rejected")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<TransactionResponseDto> rejectTransaction(
            @Parameter(description = "Transaction ID")
            @PathVariable String transactionId,
            @Parameter(description = "Rejection reason")
            @RequestParam String rejectionReason) {
        
        Transaction transaction = transactionService.rejectTransaction(transactionId, rejectionReason);
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction);
        
        log.warn("Transaction rejected: transactionId={}, reason={}, rejectedBy={}", 
                transactionId, rejectionReason, SecurityContext.getCurrentUserId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get pending transactions (Admin/Compliance only)
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get pending transactions",
        description = "Retrieves all pending transactions (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Pending transactions retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<TransactionResponseDto>> getPendingTransactions() {
        List<Transaction> transactions = transactionService.findPendingTransactions();
        List<TransactionResponseDto> response = transactions.stream()
            .map(TransactionResponseDto::fromTransaction)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get transactions requiring approval (Admin/Compliance only)
     */
    @GetMapping("/admin/requiring-approval")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get transactions requiring approval",
        description = "Retrieves all transactions requiring approval (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Transactions requiring approval retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsRequiringApproval() {
        List<Transaction> transactions = transactionService.findTransactionsRequiringApproval();
        List<TransactionResponseDto> response = transactions.stream()
            .map(TransactionResponseDto::fromTransaction)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get stale pending transactions (Admin only)
     */
    @GetMapping("/admin/stale-pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get stale pending transactions",
        description = "Retrieves transactions that have been pending too long (Admin only)"
    )
    @ApiResponse(responseCode = "200", description = "Stale pending transactions retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<TransactionResponseDto>> getStalePendingTransactions(
            @Parameter(description = "Hours old threshold")
            @RequestParam(defaultValue = "24") int hoursOld) {
        
        List<Transaction> transactions = transactionService.findStalePendingTransactions(hoursOld);
        List<TransactionResponseDto> response = transactions.stream()
            .map(TransactionResponseDto::fromTransaction)
            .toList();
        
        if (!transactions.isEmpty()) {
            log.warn("Found {} stale pending transactions older than {} hours", 
                    transactions.size(), hoursOld);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get transaction by idempotency key (Admin only)
     */
    @GetMapping("/admin/by-idempotency/{idempotencyKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get transaction by idempotency key",
        description = "Retrieves transaction by idempotency key (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<TransactionResponseDto> getTransactionByIdempotencyKey(
            @Parameter(description = "Idempotency key")
            @PathVariable String idempotencyKey) {
        
        Optional<Transaction> transaction = transactionService.findByIdempotencyKey(idempotencyKey);
        if (transaction.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TransactionResponseDto response = TransactionResponseDto.fromTransaction(transaction.get());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user transactions by user ID (Admin/Compliance only)
     */
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(
        summary = "Get user transactions by user ID",
        description = "Retrieves transaction history for specific user (Admin/Compliance only)"
    )
    @ApiResponse(responseCode = "200", description = "User transactions retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Page<TransactionResponseDto>> getUserTransactionsByUserId(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Transaction> transactions = transactionService.findUserTransactions(userId, pageable);
        Page<TransactionResponseDto> response = transactions.map(TransactionResponseDto::fromTransaction);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
