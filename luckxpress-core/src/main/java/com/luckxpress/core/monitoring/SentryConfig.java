package com.luckxpress.core.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;
import io.sentry.spring.jakarta.EnableSentry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.sentry.spring.jakarta.SentryUserProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentry error monitoring configuration
 * CRITICAL: Never log sensitive data like passwords or payment info
 */
@Slf4j
@Configuration
@EnableSentry(dsn = "${sentry.dsn}")
public class SentryConfig {
    
    @Value("${sentry.environment:development}")
    private String environment;
    
    @Value("${sentry.release:unknown}")
    private String release;
    
    @Value("${sentry.traces-sample-rate:0.2}")
    private Double tracesSampleRate;
    
    @Bean
    public void configureSentry() {
        Sentry.init(options -> {
            options.setDsn("${sentry.dsn}");
            options.setEnvironment(environment);
            options.setRelease(release);
            options.setTracesSampleRate(tracesSampleRate);
            options.setAttachThreads(true);
            options.setAttachStacktrace(true);
            options.setEnableExternalConfiguration(true);
            
            // Set minimum event level
            options.setMinimumEventLevel(SentryLevel.WARNING);
            
            // Set tags for filtering
            options.setTag("service", "luckxpress-backend");
            options.setTag("component", "core");
            
            // Sensitive data scrubbing
            options.setBeforeSend((event, hint) -> {
                // Remove sensitive data from events
                if (event.getRequest() != null) {
                    event.getRequest().setCookies(null);
                    event.getRequest().getHeaders().remove("Authorization");
                    event.getRequest().getHeaders().remove("X-API-Key");
                }
                
                // Don't send events for health checks
                if (event.getMessage() != null && 
                    event.getMessage().getMessage().contains("/actuator/health")) {
                    return null;
                }
                
                return event;
            });
            
            // Ignore certain exceptions
            options.addIgnoredExceptionForType(jakarta.validation.ConstraintViolationException.class);
        });
        
        log.info("Sentry configured for environment: {}, release: {}", environment, release);
    }
    
    @Bean
    public SentryUserProvider sentryUserProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                User sentryUser = new User();
                sentryUser.setUsername(authentication.getName());
                // Don't send email or other PII unless necessary
                sentryUser.setId(authentication.getName());
                return sentryUser;
            }
            return null;
        };
    }
}
