package com.luckxpress.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Transaction types for ledger entries
 * CRITICAL: Each type must maintain ledger balance integrity
 */
@Getter
@RequiredArgsConstructor
public enum TransactionType {
    
    // Credit transactions (increase balance)
    DEPOSIT("deposit", "Real money deposit", true, false),
    BONUS("bonus", "Promotional bonus", true, false),
    WIN("win", "Game win", true, false),
    PROMO("promo", "Promotional credit", true, false),
    REVERSAL_CREDIT("reversal_credit", "Transaction reversal credit", true, true),
    
    // Debit transactions (decrease balance)
    BET("bet", "Game bet", false, false),
    LOSS("loss", "Game loss", false, false),
    WITHDRAWAL("withdrawal", "Cash withdrawal", false, false),
    REVERSAL_DEBIT("reversal_debit", "Transaction reversal debit", false, true),
    ADJUSTMENT_DEBIT("adjustment_debit", "Manual adjustment debit", false, true);
    
    private final String code;
    private final String description;
    private final boolean credit;
    private final boolean requiresApproval;
    
    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: " + code);
    }
}
