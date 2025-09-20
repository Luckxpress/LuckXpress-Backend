package com.luckxpress.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money Handling Utility - COMPLIANCE CRITICAL
 * 
 * COMPLIANCE RULE #1: Money MUST use java.math.BigDecimal with scale=4 and RoundingMode.HALF_UP
 * COMPLIANCE RULE #2: NEVER use Double, Float, double, or float for money
 * 
 * This utility ensures all monetary calculations follow strict financial compliance.
 * ALL money operations in LuckXpress MUST use this utility.
 */
public final class MoneyUtil {
    
    // COMPLIANCE: Scale=4, RoundingMode.HALF_UP per rule #1
    public static final int MONEY_SCALE = 4;
    public static final RoundingMode MONEY_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // Common money constants
    public static final BigDecimal ZERO = createMoney("0.0000");
    public static final BigDecimal ONE_CENT = createMoney("0.0100");
    public static final BigDecimal ONE_DOLLAR = createMoney("1.0000");
    public static final BigDecimal DUAL_APPROVAL_THRESHOLD = createMoney("500.0000");

    // Private constructor - utility class
    private MoneyUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a properly scaled BigDecimal for money from a string value
     * COMPLIANCE: Always use this method to create money amounts
     * 
     * @param value the string representation of the money amount
     * @return BigDecimal with proper scale and rounding
     * @throws IllegalArgumentException if value is null or invalid
     */
    public static BigDecimal createMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Money value cannot be null or empty");
        }
        
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            return amount.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid money format: " + value, e);
        }
    }

    /**
     * Creates a properly scaled BigDecimal for money from a double value
     * WARNING: Use sparingly and only for constants. Prefer string input.
     * 
     * @param value the double representation of the money amount
     * @return BigDecimal with proper scale and rounding
     */
    public static BigDecimal createMoney(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Money value cannot be NaN or infinite");
        }
        
        BigDecimal amount = BigDecimal.valueOf(value);
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }

    /**
     * Adds two money amounts with proper scaling
     * COMPLIANCE: All monetary arithmetic must use this method
     * 
     * @param amount1 first money amount
     * @param amount2 second money amount
     * @return sum with proper money scaling
     */
    public static BigDecimal add(BigDecimal amount1, BigDecimal amount2) {
        validateMoneyAmount(amount1, "amount1");
        validateMoneyAmount(amount2, "amount2");
        
        return amount1.add(amount2).setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }

    /**
     * Subtracts two money amounts with proper scaling
     * COMPLIANCE: All monetary arithmetic must use this method
     * 
     * @param amount1 first money amount (minuend)
     * @param amount2 second money amount (subtrahend)
     * @return difference with proper money scaling
     */
    public static BigDecimal subtract(BigDecimal amount1, BigDecimal amount2) {
        validateMoneyAmount(amount1, "amount1");
        validateMoneyAmount(amount2, "amount2");
        
        return amount1.subtract(amount2).setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }

    /**
     * Multiplies a money amount by a factor with proper scaling
     * COMPLIANCE: All monetary arithmetic must use this method
     * 
     * @param amount the money amount
     * @param factor the multiplication factor
     * @return product with proper money scaling
     */
    public static BigDecimal multiply(BigDecimal amount, BigDecimal factor) {
        validateMoneyAmount(amount, "amount");
        Objects.requireNonNull(factor, "Factor cannot be null");
        
        return amount.multiply(factor).setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }

    /**
     * Divides a money amount by a divisor with proper scaling
     * COMPLIANCE: All monetary arithmetic must use this method
     * 
     * @param amount the money amount
     * @param divisor the division factor
     * @return quotient with proper money scaling
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal amount, BigDecimal divisor) {
        validateMoneyAmount(amount, "amount");
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide money by zero");
        }
        
        return amount.divide(divisor, MONEY_SCALE, MONEY_ROUNDING_MODE);
    }

    /**
     * Compares two money amounts
     * COMPLIANCE: Use this for all money comparisons
     * 
     * @param amount1 first money amount
     * @param amount2 second money amount
     * @return comparison result (-1, 0, 1)
     */
    public static int compare(BigDecimal amount1, BigDecimal amount2) {
        validateMoneyAmount(amount1, "amount1");
        validateMoneyAmount(amount2, "amount2");
        
        return amount1.compareTo(amount2);
    }

    /**
     * Checks if amount is positive (> 0)
     * 
     * @param amount the money amount to check
     * @return true if amount is positive
     */
    public static boolean isPositive(BigDecimal amount) {
        validateMoneyAmount(amount, "amount");
        return amount.compareTo(ZERO) > 0;
    }

    /**
     * Checks if amount is zero
     * 
     * @param amount the money amount to check
     * @return true if amount is zero
     */
    public static boolean isZero(BigDecimal amount) {
        validateMoneyAmount(amount, "amount");
        return amount.compareTo(ZERO) == 0;
    }

    /**
     * Checks if amount is negative (< 0)
     * 
     * @param amount the money amount to check
     * @return true if amount is negative
     */
    public static boolean isNegative(BigDecimal amount) {
        validateMoneyAmount(amount, "amount");
        return amount.compareTo(ZERO) < 0;
    }

    /**
     * Checks if transaction requires dual approval per compliance rule #8
     * 
     * @param amount the transaction amount
     * @return true if amount exceeds $500.00 threshold
     */
    public static boolean requiresDualApproval(BigDecimal amount) {
        validateMoneyAmount(amount, "amount");
        return amount.compareTo(DUAL_APPROVAL_THRESHOLD) > 0;
    }

    /**
     * Formats money amount as string with proper decimal places
     * 
     * @param amount the money amount
     * @return formatted string (e.g., "123.4500")
     */
    public static String formatMoney(BigDecimal amount) {
        validateMoneyAmount(amount, "amount");
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE).toPlainString();
    }

    /**
     * Validates that a BigDecimal represents a valid money amount
     * COMPLIANCE: Internal validation for all money operations
     * 
     * @param amount the amount to validate
     * @param paramName parameter name for error messages
     * @throws IllegalArgumentException if amount is invalid
     */
    private static void validateMoneyAmount(BigDecimal amount, String paramName) {
        Objects.requireNonNull(amount, paramName + " cannot be null");
        
        // Ensure proper scale - this is critical for compliance
        if (amount.scale() > MONEY_SCALE) {
            throw new IllegalArgumentException(
                paramName + " has invalid scale: " + amount.scale() + 
                ". Money amounts must have scale <= " + MONEY_SCALE
            );
        }
    }
}
