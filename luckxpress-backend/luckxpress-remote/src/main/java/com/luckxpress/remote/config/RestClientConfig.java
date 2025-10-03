package com.luckxpress.remote.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for REST client templates
 * Provides configured RestTemplate beans for external API calls
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a configured RestTemplate bean
     * @param builder RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Creates a RestTemplate with custom timeout for long-running operations
     * @param builder RestTemplateBuilder
     * @return configured RestTemplate for long operations
     */
    @Bean(name = "longRunningRestTemplate")
    public RestTemplate longRunningRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofMinutes(5))
                .build();
    }
}
