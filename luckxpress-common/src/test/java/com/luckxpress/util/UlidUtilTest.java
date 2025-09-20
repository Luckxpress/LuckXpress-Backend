package com.luckxpress.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ULID Utility Tests - COMPLIANCE VALIDATION
 * 
 * COMPLIANCE SPECIFICATION: All IDs must be ULID (NOT UUID, NOT Long)
 */
class UlidUtilTest {

    @Test
    void testUlidGeneration() {
        // COMPLIANCE: All IDs must be ULID (NOT UUID, NOT Long)
        String ulid1 = UlidUtil.generate();
        String ulid2 = UlidUtil.generate();
        
        assertNotNull(ulid1);
        assertNotNull(ulid2);
        assertNotEquals(ulid1, ulid2);
        assertEquals(26, ulid1.length());
        assertEquals(26, ulid2.length());
        
        assertTrue(UlidUtil.isValid(ulid1));
        assertTrue(UlidUtil.isValid(ulid2));
    }

    @Test
    void testUlidValidation() {
        // Test valid ULID format
        String validUlid = UlidUtil.generate();
        assertTrue(UlidUtil.isValid(validUlid));
        
        // Test invalid formats
        assertFalse(UlidUtil.isValid(null));
        assertFalse(UlidUtil.isValid(""));
        assertFalse(UlidUtil.isValid("too-short"));
        assertFalse(UlidUtil.isValid("this-is-way-too-long-for-ulid"));
        assertFalse(UlidUtil.isValid("invalid-chars-!@#$%^&*()"));
    }

    @Test
    void testUlidComparison() {
        String ulid1 = UlidUtil.generate();
        String ulid2 = UlidUtil.generate();
        
        // ULIDs should be comparable
        int comparison = UlidUtil.compare(ulid1, ulid2);
        assertTrue(comparison != 0); // Should be different
        
        // Same ULID should compare as equal
        assertEquals(0, UlidUtil.compare(ulid1, ulid1));
    }

    @Test
    void testUlidNormalization() {
        String ulid = UlidUtil.generate();
        String normalized = UlidUtil.normalize(ulid);
        
        assertNotNull(normalized);
        assertEquals(26, normalized.length());
        assertTrue(UlidUtil.isValid(normalized));
    }
}
