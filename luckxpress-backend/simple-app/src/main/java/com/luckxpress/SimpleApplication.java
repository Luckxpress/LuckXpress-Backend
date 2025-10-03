package com.luckxpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class SimpleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleApplication.class, args);
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to LuckXpress Backend!");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "UP");
        return response;
    }

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "LuckXpress Backend");
        health.put("version", "1.0.0");
        return health;
    }

    @GetMapping("/api/v1/info")
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "LuckXpress Backend");
        info.put("description", "Multi-module Spring Boot Application");
        info.put("version", "1.0.0");
        info.put("java.version", System.getProperty("java.version"));
        info.put("spring.boot.version", "3.2.0");
        return info;
    }
}
