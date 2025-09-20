package com.luckxpress.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compliance Service - CRITICAL REGULATORY COMPLIANCE
 * 
 * COMPLIANCE RULE #5: States WA and ID must ALWAYS be blocked for Sweeps gameplay
 * 
 * This service enforces all regulatory compliance rules for LuckXpress.
 * ALL gameplay and transaction operations MUST use this service for validation.
 */
@Service
public class ComplianceService {

    private final Set<String> blockedStates;

    public ComplianceService(@Value("${luckxpress.compliance.blocked-states}") List<String> blockedStatesList) {
        // Convert to uppercase Set for case-insensitive lookups
        this.blockedStates = blockedStatesList.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        
        // Compliance validation - ensure WA and ID are blocked
        if (!blockedStates.contains("WA") || !blockedStates.contains("ID")) {
            throw new IllegalStateException(
                "COMPLIANCE VIOLATION: WA and ID states must be blocked for Sweeps gameplay. " +
                "Current blocked states: " + blockedStates
            );
        }
    }

    /**
     * Checks if a state is blocked for Sweeps gameplay
     * COMPLIANCE RULE #5: WA and ID must ALWAYS return true
     * 
     * @param stateCode the US state code (e.g., "WA", "ID", "CA")
     * @return true if state is blocked for Sweeps gameplay
     */
    public boolean isStateBlockedForSweeps(String stateCode) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            return true; // Block if no state provided
        }
        
        return blockedStates.contains(stateCode.trim().toUpperCase());
    }

    /**
     * Validates that a user from the given state can participate in Sweeps
     * COMPLIANCE RULE #5: Must throw exception for WA and ID
     * 
     * @param stateCode the user's state code
     * @param userId the user's ID for logging
     * @throws IllegalStateException if state is blocked
     */
    public void validateSweepsEligibility(String stateCode, String userId) {
        if (isStateBlockedForSweeps(stateCode)) {
            throw new IllegalStateException(
                String.format("COMPLIANCE BLOCK: User %s from state %s is not eligible for Sweeps gameplay. " +
                    "Blocked states: %s", userId, stateCode, blockedStates)
            );
        }
    }

    /**
     * Gets all blocked states for Sweeps gameplay
     * 
     * @return Set of blocked state codes
     */
    public Set<String> getBlockedStates() {
        return Set.copyOf(blockedStates);
    }

    /**
     * Validates that Gold Coins cannot be withdrawn
     * COMPLIANCE RULE #3: Gold Coins purchased with real money are NEVER withdrawable
     * 
     * @param coinType the type of coin being withdrawn
     * @throws IllegalStateException if attempting to withdraw Gold Coins
     */
    public void validateWithdrawal(CoinType coinType) {
        if (coinType == CoinType.GOLD_COIN) {
            throw new IllegalStateException(
                "COMPLIANCE VIOLATION: Gold Coins purchased with real money are NEVER withdrawable"
            );
        }
    }

    /**
     * Validates that Sweeps Coins can only come from valid sources
     * COMPLIANCE RULE #4: Sweeps Coins ONLY from promotions/AMOE/bonuses - NEVER direct purchase
     * 
     * @param coinType the type of coin being created
     * @param source the source of the coins
     * @throws IllegalStateException if attempting direct purchase of Sweeps Coins
     */
    public void validateCoinSource(CoinType coinType, CoinSource source) {
        if (coinType == CoinType.SWEEPS_COIN && source == CoinSource.DIRECT_PURCHASE) {
            throw new IllegalStateException(
                "COMPLIANCE VIOLATION: Sweeps Coins can ONLY come from promotions/AMOE/bonuses - NEVER direct purchase"
            );
        }
    }

    /**
     * Coin types in the LuckXpress system
     */
    public enum CoinType {
        GOLD_COIN,      // Purchased with real money - NOT withdrawable
        SWEEPS_COIN     // Only from promotions/AMOE/bonuses - withdrawable
    }

    /**
     * Valid sources for coins in the system
     */
    public enum CoinSource {
        DIRECT_PURCHASE,    // Only valid for Gold Coins
        PROMOTION,          // Valid for Sweeps Coins
        AMOE,              // Alternative Method of Entry - Valid for Sweeps Coins
        BONUS,             // Valid for Sweeps Coins
        REFERRAL           // Valid for Sweeps Coins
    }
}
