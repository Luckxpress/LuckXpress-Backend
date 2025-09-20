package com.luckxpress.common.exception;

import lombok.Getter;
import java.math.BigDecimal;

/**
 * Exception when operation requires dual approval
 */
@Getter
public class DualApprovalRequiredException extends BaseException {
    
    private final String operationType;
    private final BigDecimal amount;
    private final String initiatorId;
    
    public DualApprovalRequiredException(
            String operationType,
            BigDecimal amount,
            String initiatorId) {
        super(
            "DUAL_APPROVAL_REQUIRED",
            String.format(
                "Operation %s with amount %s requires dual approval",
                operationType,
                amount.toPlainString()
            )
        );
        this.operationType = operationType;
        this.amount = amount;
        this.initiatorId = initiatorId;
    }
}
