package com.luckxpress.web.controller.player;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.service.wallet.WalletService;
import com.luckxpress.web.dto.request.DepositRequest;
import com.luckxpress.web.dto.request.WithdrawalRequest;
import com.luckxpress.web.dto.response.TransactionResponse;
import com.luckxpress.web.dto.response.WalletResponse;
import com.luckxpress.web.security.CurrentUser;
import io.micrometer.core.annotation.Timed;
import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

/**
 * Player wallet operations controller
 * CRITICAL: All financial operations require idempotency keys
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/player/wallet")
@Tag(name = "Player Wallet", description = """
    Wallet operations for players.
    
    **Important Compliance Notes:**
    - Gold Coins are purchased and NOT withdrawable
    - Sweeps Coins are promotional and withdrawable after KYC
    - All amounts must have exactly 4 decimal places
    - States WA and ID cannot play with Sweeps
    """)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PlayerWalletController {
    
    private final WalletService walletService;
    private final WithdrawalService withdrawalService;
    private final PaymentService paymentService;
    
    @GetMapping("/balance")
    @Operation(
        summary = "Get wallet balance",
        description = "Returns current Gold and Sweeps balances for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Balance retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WalletResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "gold_balance": "1000.0000",
                        "sweeps_balance": "50.0000",
                        "gold_locked": "0.0000",
                        "sweeps_locked": "0.0000",
                        "available_gold": "1000.0000",
                        "available_sweeps": "50.0000",
                        "can_withdraw_sweeps": true,
                        "kyc_status": "VERIFIED",
                        "state_restricted": false
                    }
                    """)
            )
        )
    })
    @Timed(value = "api.player.wallet.balance", description = "Player balance retrieval")
    public ResponseEntity<WalletResponse> getBalance(@CurrentUser String userId) {
        log.info("Fetching balance for user: {}", userId);
        
        var balance = walletService.getBalance(userId);
        var user = userService.findById(userId);
        
        return ResponseEntity.ok(WalletResponse.builder()
            .goldBalance(balance.getGoldBalance())
            .sweepsBalance(balance.getSweepsBalance())
            .goldLocked(balance.getGoldLocked())
            .sweepsLocked(balance.getSweepsLocked())
            .availableGold(balance.getAvailableGold())
            .availableSweeps(balance.getAvailableSweeps())
            .canWithdrawSweeps(user.canWithdraw())
            .kycStatus(user.getKycStatus().name())
            .stateRestricted(StateRestriction.isStateRestricted(user.getState()))
            .build());
    }
    
    @PostMapping("/deposit")
    @Operation(
        summary = "Deposit Gold Coins",
        description = """
            Process a Gold Coin purchase.
            
            **Important:**
            - Requires valid payment token from payment provider
            - Automatically grants bonus Sweeps based on promotion rules
            - Idempotency key required to prevent duplicate charges
            - Minimum deposit: $5.00, Maximum: $10,000.00
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Deposit processed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "transaction_id": "TXN_01J8XYZABC",
                        "status": "SUCCESS",
                        "amount": "100.0000",
                        "currency": "GOLD",
                        "gold_balance": "1100.0000",
                        "sweeps_balance": "55.0000",
                        "bonus_sweeps": "5.0000",
                        "payment_reference": "pi_1234567890",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid amount or payment token"
        ),
        @ApiResponse(
            responseCode = "402",
            description = "Payment failed"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate request (idempotency conflict)"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Deposit limit exceeded"
        )
    })
    @Timed(value = "api.player.wallet.deposit", description = "Deposit processing time")
    public ResponseEntity<TransactionResponse> deposit(
            @CurrentUser String userId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        
        log.info("Processing deposit for user: {}, amount: {}, idempotency: {}", 
            userId, request.getAmount(), idempotencyKey);
        
        try {
            // Check daily deposit limit
            if (exceedsDailyLimit(userId, request.getAmount())) {
                throw new LimitExceededException("Daily deposit limit exceeded");
            }
            
            // Process payment
            var paymentResult = paymentService.processPayment(
                userId,
                request.getAmount(),
                request.getPaymentToken(),
                request.getPaymentMethod(),
                idempotencyKey
            );
            
            // Credit Gold
            var goldTransaction = walletService.creditGold(
                userId,
                request.getAmount(),
                paymentResult.getPaymentId(),
                idempotencyKey
            );
            
            // Apply bonus Sweeps (if applicable)
            BigDecimal bonusSweeps = calculateBonusSweeps(request.getAmount());
            if (bonusSweeps.compareTo(BigDecimal.ZERO) > 0) {
                walletService.creditSweeps(
                    userId,
                    bonusSweeps,
                    "DEPOSIT_BONUS",
                    "Bonus for Gold purchase"
                );
            }
            
            // Track in Sentry
            Sentry.captureMessage(
                String.format("Deposit success: User %s, Amount %s", userId, request.getAmount()),
                SentryLevel.INFO
            );
            
            return ResponseEntity.ok(TransactionResponse.builder()
                .transactionId(goldTransaction.getTransactionId())
                .status("SUCCESS")
                .amount(request.getAmount())
                .currency(CurrencyType.GOLD.name())
                .goldBalance(goldTransaction.getBalanceAfter())
                .sweepsBalance(walletService.getBalance(userId).getSweepsBalance())
                .bonusSweeps(bonusSweeps)
                .paymentReference(paymentResult.getPaymentId())
                .timestamp(Instant.now())
                .build());
                
        } catch (IdempotencyException e) {
            // Return cached response for duplicate request
            return ResponseEntity.ok(getCachedResponse(idempotencyKey));
        } catch (PaymentException e) {
            log.error("Payment failed for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(TransactionResponse.failed(e.getMessage()));
        }
    }
    
    @PostMapping("/withdraw")
    @Operation(
        summary = "Withdraw Sweeps Coins",
        description = """
            Request withdrawal of Sweeps Coins to cash.
            
            **Requirements:**
            - KYC must be verified
            - Minimum withdrawal: $50.00
            - Maximum withdrawal: $5,000.00 per transaction
            - Daily limit: $5,000.00
            - Weekly limit: $25,000.00
            - Not available in WA or ID
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Withdrawal request created",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "withdrawal_id": "WDR_01J8XYZABC",
                        "status": "PENDING",
                        "amount": "100.0000",
                        "method": "ACH",
                        "estimated_arrival": "2024-01-03T00:00:00Z",
                        "sweeps_balance": "450.0000",
                        "message": "Withdrawal request created and pending review"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "KYC not verified or state restricted",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "KYC_REQUIRED",
                        "message": "KYC verification required before withdrawal",
                        "kyc_status": "NOT_STARTED",
                        "trace_id": "TRC-123456"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Insufficient balance or invalid amount"
        )
    })
    @PreAuthorize("@complianceService.canWithdraw(#userId)")
    @Timed(value = "api.player.wallet.withdraw", description = "Withdrawal processing time")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @CurrentUser String userId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request) {
        
        log.info("Processing withdrawal for user: {}, amount: {}", userId, request.getAmount());
        
        // Check compliance
        var user = userService.findById(userId);
        if (!user.canWithdraw()) {
            throw new ComplianceException(
                ComplianceException.ComplianceType.KYC_REQUIRED,
                "Cannot withdraw: " + (user.getKycStatus() == KycStatus.NOT_STARTED ? 
                    "KYC not started" : "State restricted"),
                userId
            );
        }
        
        // Process withdrawal
        var withdrawal = withdrawalService.createWithdrawal(
            userId,
            request.getAmount(),
            request.getMethod(),
            request.getAccountDetails(),
            idempotencyKey
        );
        
        return ResponseEntity.ok(WithdrawalResponse.from(withdrawal));
    }
}
