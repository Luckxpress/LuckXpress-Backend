package com.luckxpress.data.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA Configuration for LuckXpress data module
 * Configures entity scanning, repositories, auditing, and transactions
 */
@Configuration
@EntityScan(basePackages = "com.luckxpress.data.entity")
@EnableJpaRepositories(basePackages = "com.luckxpress.data.repository")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Provides the current auditor for JPA auditing
     * This is used to automatically set createdBy and updatedBy fields
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Implementation of AuditorAware to provide current auditor
     */
    private static class AuditorAwareImpl implements AuditorAware<String> {
        
        @Override
        public Optional<String> getCurrentAuditor() {
            // In a real application, this would get the current user from SecurityContext
            // For now, return a default value
            return Optional.of("system");
        }
    }
}
