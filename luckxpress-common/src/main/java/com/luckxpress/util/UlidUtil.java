package com.luckxpress.util;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * ULID Generator Utility - COMPLIANCE CRITICAL
 * 
 * COMPLIANCE SPECIFICATION: All IDs must be ULID (NOT UUID, NOT Long)
 * 
 * ULID Benefits over UUID:
 * - Lexicographically sortable (time-ordered)
 * - Case insensitive
 * - No special characters
 * - URL safe
 * - 128-bit compatibility with UUID
 * 
 * Format: 01ARZ3NDEKTSV4RRFFQ69G5FAV
 * - 10 characters timestamp (millisecond precision)
 * - 16 characters randomness
 */
public final class UlidUtil {
    
    // Private constructor - utility class
    private UlidUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a new ULID string
     * COMPLIANCE: Use this for ALL entity IDs in LuckXpress
     * 
     * @return new ULID string (26 characters)
     */
    public static String generate() {
        return UlidCreator.getUlid().toString();
    }

    /**
     * Generates a monotonic ULID string
     * Ensures sequential ordering even when generated in quick succession
     * Use for high-frequency operations like transactions
     * 
     * @return new monotonic ULID string (26 characters)
     */
    public static String generateMonotonic() {
        return UlidCreator.getMonotonicUlid().toString();
    }

    /**
     * Validates if a string is a valid ULID format
     * 
     * @param ulid the string to validate
     * @return true if valid ULID format
     */
    public static boolean isValid(String ulid) {
        if (ulid == null || ulid.length() != 26) {
            return false;
        }
        
        try {
            // Simple validation for ULID format (26 chars, base32)
            return ulid.matches("[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts timestamp from ULID
     * 
     * @param ulid the ULID string
     * @return timestamp in milliseconds
     * @throws IllegalArgumentException if ULID is invalid
     */
    public static long extractTimestamp(String ulid) {
        if (!isValid(ulid)) {
            throw new IllegalArgumentException("Invalid ULID format: " + ulid);
        }
        
        // Extract timestamp from first 10 characters of ULID
        String timestampPart = ulid.substring(0, 10);
        return decodeBase32Timestamp(timestampPart);
    }
    
    /**
     * Decodes base32 timestamp from ULID
     */
    private static long decodeBase32Timestamp(String base32) {
        final String BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        long result = 0;
        for (char c : base32.toCharArray()) {
            result = result * 32 + BASE32.indexOf(c);
        }
        return result;
    }

    /**
     * Compares two ULIDs lexicographically
     * ULIDs are naturally sortable by time due to their structure
     * 
     * @param ulid1 first ULID
     * @param ulid2 second ULID
     * @return comparison result (-1, 0, 1)
     */
    public static int compare(String ulid1, String ulid2) {
        if (!isValid(ulid1)) {
            throw new IllegalArgumentException("Invalid ULID format: " + ulid1);
        }
        if (!isValid(ulid2)) {
            throw new IllegalArgumentException("Invalid ULID format: " + ulid2);
        }
        
        return ulid1.compareTo(ulid2);
    }

    /**
     * Converts ULID to lowercase (canonical form)
     * 
     * @param ulid the ULID string
     * @return lowercase ULID
     */
    public static String normalize(String ulid) {
        if (!isValid(ulid)) {
            throw new IllegalArgumentException("Invalid ULID format: " + ulid);
        }
        
        return ulid.toUpperCase(); // ULIDs are typically uppercase
    }
}
