package com.luckxpress.service.ledger;

import com.luckxpress.common.constants.CurrencyType;
import com.luckxpress.common.constants.TransactionType;
import com.luckxpress.common.util.IdGenerator;
import com.luckxpress.data.entity.LedgerEntry;
import com.luckxpress.data.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Ledger service for financial audit trail
 * CRITICAL: All financial operations must be recorded
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    
    /**
     * Record deposit transaction
     */
    @Transactional
    public String recordDeposit(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String paymentReference,
            String description) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payment_reference", paymentReference);
        metadata.put("payment_method", "CARD"); // This would come from payment processor
        
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId)
            .currency(currency)
            .transactionType(TransactionType.DEPOSIT)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .referenceId(paymentReference)
            .referenceType("PAYMENT")
            .metadata(metadata)
            .description(description)
            .actor("SYSTEM")
            .actorType("AUTOMATED")
            .reason("Customer deposit")
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        log.info("Recorded deposit ledger entry: {} for user: {}, amount: {} {}",
            entry.getId(), userId, amount, currency.getCode());
        
        return entry.getId();
    }
    
    /**
     * Record bonus/promotional credit
     */
    @Transactional
    public String recordBonus(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String promotionReference,
            String reason) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("promotion_reference", promotionReference);
        metadata.put("bonus_type", "PROMOTIONAL");
        
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId)
            .currency(currency)
            .transactionType(TransactionType.BONUS)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .referenceId(promotionReference)
            .referenceType("PROMOTION")
            .metadata(metadata)
            .description("Promotional bonus: " + reason)
            .actor("SYSTEM")
            .actorType("AUTOMATED")
            .reason(reason)
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        log.info("Recorded bonus ledger entry: {} for user: {}, amount: {} {}",
            entry.getId(), userId, amount, currency.getCode());
        
        return entry.getId();
    }
    
    /**
     * Record bet/wager transaction
     */
    @Transactional
    public String recordBet(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String gameReference,
            String description) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("game_reference", gameReference);
        metadata.put("bet_type", "GAME_WAGER");
        
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId)
            .currency(currency)
            .transactionType(TransactionType.BET)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .referenceId(gameReference)
            .referenceType("GAME")
            .metadata(metadata)
            .description(description)
            .actor("SYSTEM")
            .actorType("AUTOMATED")
            .reason("Game wager")
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        log.info("Recorded bet ledger entry: {} for user: {}, amount: {} {}",
            entry.getId(), userId, amount, currency.getCode());
        
        return entry.getId();
    }
    
    /**
     * Record win/payout transaction
     */
    @Transactional
    public String recordWin(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String gameReference,
            String description) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("game_reference", gameReference);
        metadata.put("win_type", "GAME_PAYOUT");
        
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId)
            .currency(currency)
            .transactionType(TransactionType.WIN)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .referenceId(gameReference)
            .referenceType("GAME")
            .metadata(metadata)
            .description(description)
            .actor("SYSTEM")
            .actorType("AUTOMATED")
            .reason("Game win")
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        log.info("Recorded win ledger entry: {} for user: {}, amount: {} {}",
            entry.getId(), userId, amount, currency.getCode());
        
        return entry.getId();
    }
    
    /**
     * Record withdrawal transaction
     */
    @Transactional
    public String recordWithdrawal(
            String userId,
            CurrencyType currency,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String withdrawalReference,
            String description) {
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("withdrawal_reference", withdrawalReference);
        metadata.put("withdrawal_method", "ACH"); // This would come from withdrawal processor
        
        LedgerEntry entry = LedgerEntry.builder()
            .userId(userId)
            .currency(currency)
            .transactionType(TransactionType.WITHDRAWAL)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .referenceId(withdrawalReference)
            .referenceType("WITHDRAWAL")
            .metadata(metadata)
            .description(description)
            .actor("SYSTEM")
            .actorType("AUTOMATED")
            .reason("Customer withdrawal")
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        log.info("Recorded withdrawal ledger entry: {} for user: {}, amount: {} {}",
            entry.getId(), userId, amount, currency.getCode());
        
        return entry.getId();
    }
}
