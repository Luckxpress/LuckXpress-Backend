package com.luckxpress.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify testing infrastructure is working
 */
@SpringBootTest(classes = LuckXpressApplication.class)
@ActiveProfiles("test")
@DisplayName("Testing Infrastructure Verification")
class SimpleTestInfrastructureTest {
    
    @Test
    @DisplayName("Should verify testing infrastructure is properly configured")
    void testingInfrastructureWorks() {
        // Basic assertion to verify AssertJ is working
        assertThat("LuckXpress").isNotEmpty();
        assertThat(1 + 1).isEqualTo(2);
        
        // Verify JUnit 5 is working
        org.junit.jupiter.api.Assertions.assertTrue(true);
        
        // This test passing means:
        // ✅ Spring Boot Test context loads
        // ✅ JUnit 5 is configured correctly  
        // ✅ AssertJ assertions work
        // ✅ Test profile is active
    }
    
    @Test
    @DisplayName("Should verify application context loads successfully")
    void contextLoads() {
        // This test will fail if Spring Boot context doesn't load properly
        // If this passes, it means all autowiring and configuration is correct
        assertThat(true).isTrue();
    }
}
