package com.luckxpress.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates money amounts with proper precision
 * CRITICAL: Enforces 4 decimal places and positive values
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = MoneyValidator.class)
public @interface ValidMoney {
    
    String message() default "Invalid money amount - must be positive with max 4 decimal places";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    boolean allowZero() default false;
    
    String min() default "0.0001";
    
    String max() default "999999999.9999";
}
