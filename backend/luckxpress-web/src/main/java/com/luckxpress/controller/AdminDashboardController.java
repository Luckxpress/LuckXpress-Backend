package com.luckxpress.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminDashboardController {
    
    // Test endpoint for dashboard metrics
    @GetMapping("/dashboard/metrics")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("revenue", 60);
        metrics.put("activeUsers", 8);
        metrics.put("deposits", 15);
        metrics.put("withdrawals", 3);
        metrics.put("revenueChange", 12.3);
        metrics.put("usersChange", 8.1);
        metrics.put("depositsChange", -2.4);
        metrics.put("withdrawalsStatus", "pending");
        
        return ResponseEntity.ok(metrics);
    }
    
    // Test endpoint for revenue trend
    @GetMapping("/dashboard/revenue-trend")
    public ResponseEntity<List<Map<String, Object>>> getRevenueTrend(
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int[] values = {45, 52, 48, 60, 55, 65, 60};
        
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("name", labels[i]);
            point.put("value", values[i]);
            trend.add(point);
        }
        
        return ResponseEntity.ok(trend);
    }
    
    // Test endpoint for conversion funnel
    @GetMapping("/dashboard/conversion-funnel")
    public ResponseEntity<List<Map<String, Object>>> getConversionFunnel() {
        List<Map<String, Object>> funnel = new ArrayList<>();
        
        funnel.add(Map.of("label", "Visitors", "value", 45291, "percentage", 100));
        funnel.add(Map.of("label", "Signups", "value", 12847, "percentage", 28.4));
        funnel.add(Map.of("label", "First Deposit", "value", 3921, "percentage", 8.7));
        funnel.add(Map.of("label", "Active Players", "value", 2847, "percentage", 6.3));
        
        return ResponseEntity.ok(funnel);
    }
    
    // Test endpoint for provider status
    @GetMapping("/dashboard/provider-status")
    public ResponseEntity<List<Map<String, Object>>> getProviderStatus() {
        List<Map<String, Object>> providers = new ArrayList<>();
        
        providers.add(Map.of("name", "Evolution Gaming", "status", "Active", "uptime", "99.99%"));
        providers.add(Map.of("name", "Nuxii", "status", "Active", "uptime", "99.95%"));
        providers.add(Map.of("name", "PayPal", "status", "Disrupted", "uptime", "96.72%"));
        providers.add(Map.of("name", "Stripe", "status", "Active", "uptime", "99.98%"));
        
        return ResponseEntity.ok(providers);
    }
}
