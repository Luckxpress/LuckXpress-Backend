package com.luckxpress.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validator for money amounts
 * CRITICAL: Ensures proper precision and range
 */
public class MoneyValidator implements ConstraintValidator<ValidMoney, BigDecimal> {
    
    private BigDecimal min;
    private BigDecimal max;
    private boolean allowZero;
    
    @Override
    public void initialize(ValidMoney annotation) {
        this.min = new BigDecimal(annotation.min());
        this.max = new BigDecimal(annotation.max());
        this.allowZero = annotation.allowZero();
    }
    
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check scale (decimal places)
        if (value.scale() > 4) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Money amount cannot have more than 4 decimal places"
            ).addConstraintViolation();
            return false;
        }
        
        // Check if zero when not allowed
        if (!allowZero && value.compareTo(BigDecimal.ZERO) == 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Money amount must be greater than zero"
            ).addConstraintViolation();
            return false;
        }
        
        // Check minimum
        if (value.compareTo(min) < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Money amount must be at least %s", min.toPlainString())
            ).addConstraintViolation();
            return false;
        }
        
        // Check maximum
        if (value.compareTo(max) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Money amount cannot exceed %s", max.toPlainString())
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
