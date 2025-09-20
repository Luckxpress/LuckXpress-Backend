package com.luckxpress.common.exception;

import com.luckxpress.common.constants.CurrencyType;
import lombok.Getter;
import java.math.BigDecimal;

/**
 * Exception for insufficient balance
 * CRITICAL: Always include exact amounts for audit trail
 */
@Getter
public class InsufficientBalanceException extends BaseException {
    
    private final String userId;
    private final CurrencyType currency;
    private final BigDecimal requestedAmount;
    private final BigDecimal availableAmount;
    
    public InsufficientBalanceException(
            String userId,
            CurrencyType currency,
            BigDecimal requestedAmount,
            BigDecimal availableAmount) {
        super(
            "INSUFFICIENT_BALANCE",
            String.format(
                "Insufficient %s balance. Requested: %s, Available: %s",
                currency.getDisplayName(),
                requestedAmount.toPlainString(),
                availableAmount.toPlainString()
            )
        );
        this.userId = userId;
        this.currency = currency;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }
}
