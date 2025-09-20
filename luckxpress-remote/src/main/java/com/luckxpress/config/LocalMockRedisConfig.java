package com.luckxpress.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.mockito.Mockito;

/**
 * Mock Redis Configuration for Local Development.
 * <p>
 * This configuration is ONLY active for the 'local' profile.
 * It provides a mock RedisTemplate bean to satisfy dependencies
 * when a real Redis server is not available.
 */
@Configuration
@Profile("local")
public class LocalMockRedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // Return a mock object that does nothing.
        // This prevents the application from crashing when Redis is not running.
        return Mockito.mock(RedisTemplate.class);
    }
}
