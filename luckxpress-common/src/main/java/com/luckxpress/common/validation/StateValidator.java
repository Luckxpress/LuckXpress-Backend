package com.luckxpress.common.validation;

import com.luckxpress.common.constants.StateRestriction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StateValidator implements ConstraintValidator<ValidState, String> {
    
    private boolean checkRestriction;
    
    @Override
    public void initialize(ValidState annotation) {
        this.checkRestriction = annotation.checkRestriction();
    }
    
    @Override
    public boolean isValid(String stateCode, ConstraintValidatorContext context) {
        if (stateCode == null || stateCode.isEmpty()) {
            return true; // Let @NotNull handle null validation
        }
        
        if (checkRestriction && StateRestriction.isStateRestricted(stateCode)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Sweeps play is not available in %s", stateCode)
            ).addConstraintViolation();
            return false;
        }
        
        // Validate state code format (2 letters)
        if (!stateCode.matches("^[A-Z]{2}$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Invalid state code format. Must be 2 uppercase letters"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
