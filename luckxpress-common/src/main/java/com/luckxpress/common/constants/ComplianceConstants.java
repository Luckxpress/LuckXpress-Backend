package com.luckxpress.common.constants;

import java.math.BigDecimal;

/**
 * Compliance thresholds and limits
 * CRITICAL: These values are legally required - DO NOT modify without legal review
 */
public class ComplianceConstants {
    
    // KYC Thresholds
    public static final BigDecimal KYC_REQUIRED_WITHDRAWAL_AMOUNT = new BigDecimal("50.0000");
    public static final BigDecimal ENHANCED_KYC_THRESHOLD = new BigDecimal("2000.0000");
    public static final BigDecimal W2G_REPORTING_THRESHOLD = new BigDecimal("600.0000");
    
    // Dual Approval Thresholds
    public static final BigDecimal DUAL_APPROVAL_THRESHOLD = new BigDecimal("500.0000");
    public static final BigDecimal TRIPLE_APPROVAL_THRESHOLD = new BigDecimal("10000.0000");
    
    // Transaction Limits
    public static final BigDecimal MIN_DEPOSIT_AMOUNT = new BigDecimal("5.0000");
    public static final BigDecimal MAX_DEPOSIT_AMOUNT = new BigDecimal("10000.0000");
    public static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("50.0000");
    public static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("5000.0000");
    
    // Daily Limits
    public static final BigDecimal DAILY_DEPOSIT_LIMIT = new BigDecimal("10000.0000");
    public static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("5000.0000");
    public static final BigDecimal WEEKLY_WITHDRAWAL_LIMIT = new BigDecimal("25000.0000");
    
    // AMOE Settings
    public static final BigDecimal AMOE_SWEEPS_AMOUNT = new BigDecimal("5.0000");
    public static final int AMOE_REQUEST_LIMIT_PER_DAY = 1;
    public static final int AMOE_REQUEST_LIMIT_PER_MONTH = 30;
    
    // Session Limits (Responsible Gaming)
    public static final int MAX_SESSION_DURATION_MINUTES = 180;
    public static final BigDecimal SESSION_LOSS_LIMIT = new BigDecimal("500.0000");
    
    // Precision Settings
    public static final int MONEY_SCALE = 4;
    public static final int PERCENTAGE_SCALE = 4;
    
    private ComplianceConstants() {
        // Prevent instantiation
    }
}
