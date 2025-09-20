package com.luckxpress.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Money Utility Tests - COMPLIANCE VALIDATION
 * 
 * COMPLIANCE SPECIFICATION: Money MUST use BigDecimal with scale=4 and RoundingMode.HALF_UP
 */
class MoneyUtilTest {

    @Test
    void testMoneyCreation() {
        // COMPLIANCE: Money must use BigDecimal with scale=4
        BigDecimal money = MoneyUtil.createMoney("123.45");
        
        assertNotNull(money);
        assertEquals(4, money.scale());
        assertEquals("123.4500", money.toPlainString());
    }

    @Test
    void testMoneyArithmetic() {
        BigDecimal amount1 = MoneyUtil.createMoney("100.00");
        BigDecimal amount2 = MoneyUtil.createMoney("50.25");
        
        // Test addition
        BigDecimal sum = MoneyUtil.add(amount1, amount2);
        assertEquals("150.2500", sum.toPlainString());
        
        // Test subtraction
        BigDecimal difference = MoneyUtil.subtract(amount1, amount2);
        assertEquals("49.7500", difference.toPlainString());
        
        // Test multiplication
        BigDecimal product = MoneyUtil.multiply(amount1, new BigDecimal("2"));
        assertEquals("200.0000", product.toPlainString());
        
        // Test division
        BigDecimal quotient = MoneyUtil.divide(amount1, new BigDecimal("4"));
        assertEquals("25.0000", quotient.toPlainString());
    }

    @Test
    void testDualApprovalThreshold() {
        // COMPLIANCE: Transactions > $500 need dual approval
        BigDecimal underThreshold = MoneyUtil.createMoney("499.99");
        BigDecimal overThreshold = MoneyUtil.createMoney("500.01");
        BigDecimal exactThreshold = MoneyUtil.createMoney("500.00");
        
        assertFalse(MoneyUtil.requiresDualApproval(underThreshold));
        assertTrue(MoneyUtil.requiresDualApproval(overThreshold));
        assertFalse(MoneyUtil.requiresDualApproval(exactThreshold));
    }

    @Test
    void testMoneyValidation() {
        // Test null validation
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.createMoney(null));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.createMoney(""));
        
        // Test invalid format
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.createMoney("invalid"));
    }
}
