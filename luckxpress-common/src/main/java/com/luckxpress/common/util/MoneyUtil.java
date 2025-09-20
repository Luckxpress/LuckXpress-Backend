package com.luckxpress.common.util;

import com.luckxpress.common.constants.ComplianceConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for money operations
 * CRITICAL: All money operations MUST go through this class
 */
public class MoneyUtil {
    
    private static final int SCALE = ComplianceConstants.MONEY_SCALE;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Normalize money amount to proper scale
     */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Add two money amounts
     */
    public static BigDecimal add(BigDecimal amount1, BigDecimal amount2) {
        BigDecimal a1 = normalize(amount1);
        BigDecimal a2 = normalize(amount2);
        return a1.add(a2).setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Subtract money amounts
     */
    public static BigDecimal subtract(BigDecimal amount1, BigDecimal amount2) {
        BigDecimal a1 = normalize(amount1);
        BigDecimal a2 = normalize(amount2);
        return a1.subtract(a2).setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Multiply money by a factor
     */
    public static BigDecimal multiply(BigDecimal amount, BigDecimal factor) {
        BigDecimal a = normalize(amount);
        return a.multiply(factor).setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Calculate percentage of amount
     */
    public static BigDecimal percentage(BigDecimal amount, BigDecimal percentage) {
        BigDecimal a = normalize(amount);
        BigDecimal p = percentage.divide(new BigDecimal("100"), SCALE + 2, ROUNDING_MODE);
        return a.multiply(p).setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Check if amount is within range
     */
    public static boolean isWithinRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        BigDecimal a = normalize(amount);
        return a.compareTo(min) >= 0 && a.compareTo(max) <= 0;
    }
    
    /**
     * Format money for display
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "0.0000";
        }
        return normalize(amount).toPlainString();
    }
    
    private MoneyUtil() {
        // Prevent instantiation
    }
}
