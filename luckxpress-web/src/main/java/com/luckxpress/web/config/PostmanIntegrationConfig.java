package com.luckxpress.web.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Postman integration endpoint for automatic collection generation
 */
@Component
@Endpoint(id = "postman")
public class PostmanIntegrationConfig {
    
    @ReadOperation
    public Map<String, Object> generatePostmanCollection() {
        Map<String, Object> collection = new HashMap<>();
        
        collection.put("info", Map.of(
            "name", "LuckXpress API - Auto Generated",
            "schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        ));
        
        // Add authentication setup
        collection.put("auth", Map.of(
            "type", "bearer",
            "bearer", Map.of("token", "{{access_token}}")
        ));
        
        // Add environment variables
        collection.put("variable", new Object[]{
            Map.of("key", "base_url", "value", "http://localhost:8080"),
            Map.of("key", "access_token", "value", ""),
            Map.of("key", "idempotency_key", "value", "")
        });
        
        // Add pre-request scripts for all requests
        collection.put("event", new Object[]{
            Map.of(
                "listen", "prerequest",
                "script", Map.of(
                    "type", "text/javascript",
                    "exec", new String[]{
                        "// Generate idempotency key for requests that need it",
                        "if (pm.request.method === 'POST' && ",
                        "    (pm.request.url.toString().includes('/deposit') ||",
                        "     pm.request.url.toString().includes('/withdraw'))) {",
                        "    pm.collectionVariables.set('idempotency_key', ",
                        "        'IDEMP_' + Date.now() + '_' + Math.random().toString(36));",
                        "}",
                        "",
                        "// Add correlation ID to all requests",
                        "pm.request.headers.add({",
                        "    key: 'X-Correlation-ID',",
                        "    value: 'COR_' + Date.now()",
                        "});"
                    }
                )
            )
        });
        
        return collection;
    }
}
