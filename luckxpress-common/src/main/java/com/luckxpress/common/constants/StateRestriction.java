package com.luckxpress.common.constants;

import lombok.Getter;
import java.util.Set;

/**
 * State restrictions for sweepstakes operations
 * CRITICAL: WA and ID are completely blocked for Sweeps play
 */
@Getter
public class StateRestriction {
    
    /**
     * States where Sweeps Coins gameplay is PROHIBITED
     */
    public static final Set<String> BLOCKED_STATES = Set.of("WA", "ID");
    
    /**
     * States requiring enhanced KYC
     */
    public static final Set<String> ENHANCED_KYC_STATES = Set.of("NY", "FL", "TX");
    
    /**
     * Check if state is restricted for Sweeps play
     */
    public static boolean isStateRestricted(String stateCode) {
        if (stateCode == null) {
            return false;
        }
        return BLOCKED_STATES.contains(stateCode.toUpperCase());
    }
    
    /**
     * Validate state for Sweeps operations
     * @throws IllegalStateException if state is restricted
     */
    public static void validateState(String stateCode) {
        if (isStateRestricted(stateCode)) {
            throw new IllegalStateException(
                String.format("Sweeps Coins play is not available in %s", stateCode)
            );
        }
    }
    
    /**
     * Check if state requires enhanced KYC
     */
    public static boolean requiresEnhancedKYC(String stateCode) {
        if (stateCode == null) {
            return false;
        }
        return ENHANCED_KYC_STATES.contains(stateCode.toUpperCase());
    }
}
