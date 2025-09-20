package com.luckxpress.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Currency types for sweepstakes casino.
 * CRITICAL: Gold is purchased, Sweeps is promotional only
 */
@Getter
@RequiredArgsConstructor
public enum CurrencyType {
    
    /**
     * Gold Coins - Purchased with real money, NEVER withdrawable
     * Used for entertainment gameplay only
     */
    GOLD("gold", "Gold Coins", false, 4),
    
    /**
     * Sweeps Coins - Obtained through promotions/AMOE only
     * Can be redeemed for cash prizes
     */
    SWEEPS("sweeps", "Sweeps Coins", true, 4);
    
    private final String code;
    private final String displayName;
    private final boolean withdrawable;
    private final int decimalPlaces;
    
    public static CurrencyType fromCode(String code) {
        for (CurrencyType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid currency code: " + code);
    }
    
    /**
     * Validate if currency can be withdrawn
     */
    public void validateWithdrawable() {
        if (!this.withdrawable) {
            throw new IllegalStateException(
                String.format("%s cannot be withdrawn. Only Sweeps Coins are withdrawable.", displayName)
            );
        }
    }
}
