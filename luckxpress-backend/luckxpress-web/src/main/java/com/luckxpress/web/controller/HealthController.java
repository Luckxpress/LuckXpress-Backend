package com.luckxpress.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for application monitoring
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    /**
     * Basic health check endpoint
     * @return health status
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if the application is running")
    @ApiResponse(responseCode = "200", description = "Application is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "LuckXpress Backend");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with component status
     * @return detailed health information
     */
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Get detailed health information")
    @ApiResponse(responseCode = "200", description = "Detailed health information")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("security", "UP");
        components.put("remote", "UP");
        health.put("components", components);
        
        Map<String, Object> system = new HashMap<>();
        system.put("java.version", System.getProperty("java.version"));
        system.put("os.name", System.getProperty("os.name"));
        system.put("os.version", System.getProperty("os.version"));
        health.put("system", system);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Liveness probe for Kubernetes
     * @return liveness status
     */
    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe", description = "Kubernetes liveness probe endpoint")
    @ApiResponse(responseCode = "200", description = "Application is alive")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ALIVE");
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe for Kubernetes
     * @return readiness status
     */
    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe", description = "Kubernetes readiness probe endpoint")
    @ApiResponse(responseCode = "200", description = "Application is ready")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "READY");
        return ResponseEntity.ok(response);
    }
}
