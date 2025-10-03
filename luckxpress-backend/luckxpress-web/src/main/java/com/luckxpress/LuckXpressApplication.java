package com.luckxpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Spring Boot Application class for LuckXpress
 * Entry point for the application
 */
@SpringBootApplication(scanBasePackages = "com.luckxpress")
@EnableFeignClients(basePackages = "com.luckxpress.remote.client")
@EnableJpaAuditing
public class LuckXpressApplication {

    /**
     * Main method to start the Spring Boot application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuckXpressApplication.class, args);
    }
}
