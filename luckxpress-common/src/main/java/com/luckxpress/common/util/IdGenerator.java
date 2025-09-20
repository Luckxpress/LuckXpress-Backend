package com.luckxpress.common.util;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * ID generation utility using ULID
 * CRITICAL: Use this for ALL entity IDs
 */
public class IdGenerator {
    
    /**
     * Generate new ULID
     */
    public static String generateId() {
        return UlidCreator.getUlid().toString();
    }
    
    /**
     * Generate prefixed ID
     */
    public static String generateId(String prefix) {
        return prefix + "_" + generateId();
    }
    
    /**
     * Validate ULID format
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        // Remove prefix if present
        String actualId = id.contains("_") ? id.substring(id.lastIndexOf("_") + 1) : id;
        
        // ULID is 26 characters
        return actualId.matches("^[0-9A-HJKMNP-TV-Z]{26}$");
    }
    
    private IdGenerator() {
        // Prevent instantiation
    }
}
