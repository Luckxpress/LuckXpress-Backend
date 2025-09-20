package com.luckxpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * LuckXpress Sweepstakes Gaming Platform
 * 
 * COMPLIANCE NOTICE:
 * This application follows strict financial compliance rules:
 * - All money amounts use BigDecimal with scale=4
 * - States WA and ID are blocked for Sweeps gameplay
 * - All payment operations require idempotency keys
 * - KYC verification required for withdrawals
 * - Immutable ledger entries only
 * 
 * Technical Stack:
 * - Java 21 with --enable-preview
 * - Spring Boot 3.3.0
 * - Maven 3.9.9
 * - PostgreSQL 15.4 (production) / H2 2.2.224 (local)
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class LuckXpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuckXpressApplication.class, args);
    }
}
