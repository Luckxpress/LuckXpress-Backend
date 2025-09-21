package com.luckxpress.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application
 * CRITICAL: Ensure all security configurations are loaded
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.luckxpress")
@EntityScan(basePackages = "com.luckxpress.data.entity")
@EnableJpaRepositories(basePackages = "com.luckxpress.data.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableCaching
@OpenAPIDefinition(
    info = @Info(
        title = "LuckXpress Sweepstakes Casino API",
        version = "1.0.0",
        description = """
            Complete API documentation for LuckXpress Sweepstakes Casino Platform.
            
            **CRITICAL COMPLIANCE NOTES:**
            - Gold Coins are purchased with real money and are NEVER withdrawable
            - Sweeps Coins are obtained through promotions/AMOE only
            - States WA and ID are restricted from Sweeps play
            - KYC verification required before first withdrawal
            - All monetary values use 4 decimal precision
            
            **Authentication:**
            Use OAuth2 Bearer token for all protected endpoints.
            Admin endpoints require additional ADMIN role.
            
            **Rate Limits:**
            - Public endpoints: 100 requests/minute
            - Authenticated endpoints: 500 requests/minute
            - Admin endpoints: 1000 requests/minute
            """,
        contact = @Contact(
            name = "LuckXpress API Support",
            email = "api-support@luckxpress.com",
            url = "https://api-docs.luckxpress.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://luckxpress.com/license"
        ),
        termsOfService = "https://luckxpress.com/terms"
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development"),
        @Server(url = "https://api-staging.luckxpress.com", description = "Staging Environment"),
        @Server(url = "https://api.luckxpress.com", description = "Production Environment")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token for API authentication"
)
@SecurityScheme(
    name = "oauth2",
    type = SecuritySchemeType.OAUTH2,
    description = "OAuth2 authentication for advanced integrations"
)
public class LuckXpressApplication {
    
    public static void main(String[] args) {
        // Set system properties for better performance
        System.setProperty("spring.jpa.open-in-view", "false");
        System.setProperty("spring.jmx.enabled", "false");
        System.setProperty("spring.main.lazy-initialization", "false");
        
        // Configure Sentry before application starts
        System.setProperty("sentry.traces-sample-rate", "0.3");
        System.setProperty("sentry.attach-stacktrace", "true");
        
        SpringApplication app = new SpringApplication(LuckXpressApplication.class);
        
        // Add custom initializers
        app.addInitializers(applicationContext -> {
            // Custom initialization logic if needed
            System.out.println("🎰 LuckXpress Backend Starting...");
        });
        
        app.run(args);
    }
}
