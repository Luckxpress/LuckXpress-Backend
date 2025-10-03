package com.luckxpress.remote.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Feign clients
 * Provides common configuration for all Feign clients
 */
@Configuration
public class FeignClientConfig {

    /**
     * Configure Feign logging level
     * @return Logger.Level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Configure request options with timeouts
     * @return Request.Options
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // Connect timeout
                60, TimeUnit.SECONDS,  // Read timeout
                true                   // Follow redirects
        );
    }

    /**
     * Configure retry mechanism
     * @return Retryer
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100,     // Period
                1000,    // Max period
                3        // Max attempts
        );
    }

    /**
     * Custom error decoder for Feign clients
     * @return ErrorDecoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Custom error decoder implementation
     */
    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            if (response.status() >= 400 && response.status() < 500) {
                return new ClientException(
                        String.format("Client error calling %s: Status %d", 
                                    methodKey, response.status())
                );
            } else if (response.status() >= 500) {
                return new ServerException(
                        String.format("Server error calling %s: Status %d", 
                                    methodKey, response.status())
                );
            }
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }

    /**
     * Custom exception for client errors
     */
    public static class ClientException extends RuntimeException {
        public ClientException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for server errors
     */
    public static class ServerException extends RuntimeException {
        public ServerException(String message) {
            super(message);
        }
    }
}
