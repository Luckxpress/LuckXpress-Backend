package com.luckxpress.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Swagger/OpenAPI configuration
 * CRITICAL: Document all compliance requirements in API specs
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${spring.application.name:LuckXpress}")
    private String applicationName;
    
    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public-api")
            .displayName("Public API")
            .pathsToMatch("/api/v1/public/**", "/api/v1/auth/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi playerApi() {
        return GroupedOpenApi.builder()
            .group("player-api")
            .displayName("Player API")
            .pathsToMatch("/api/v1/player/**")
            .addOpenApiCustomizer(addSecurityItem())
            .build();
    }
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin-api")
            .displayName("Admin API")
            .pathsToMatch("/api/v1/admin/**")
            .addOpenApiCustomizer(addSecurityItem())
            .addOpenApiCustomizer(addAdminExamples())
            .build();
    }
    
    @Bean
    public GroupedOpenApi webhookApi() {
        return GroupedOpenApi.builder()
            .group("webhook-api")
            .displayName("Webhook API")
            .pathsToMatch("/webhooks/**")
            .build();
    }
    
    @Bean
    public OpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            // Add global headers
            openApi.getPaths().values().forEach(pathItem -> 
                pathItem.readOperations().forEach(operation -> {
                    // Add correlation ID header
                    operation.addParametersItem(new Parameter()
                        .name("X-Correlation-ID")
                        .description("Unique request identifier for tracking")
                        .in("header")
                        .required(false)
                        .schema(new Schema<String>().type("string"))
                    );
                    
                    // Add idempotency key for POST operations
                    if (operation.getOperationId() != null && 
                        operation.getOperationId().toLowerCase().contains("create") ||
                        operation.getOperationId().toLowerCase().contains("deposit") ||
                        operation.getOperationId().toLowerCase().contains("withdraw")) {
                        operation.addParametersItem(new Parameter()
                            .name("X-Idempotency-Key")
                            .description("Idempotency key to prevent duplicate requests")
                            .in("header")
                            .required(true)
                            .schema(new Schema<String>().type("string"))
                        );
                    }
                    
                    // Add standard error responses
                    ApiResponses responses = operation.getResponses();
                    if (responses == null) {
                        responses = new ApiResponses();
                        operation.setResponses(responses);
                    }
                    
                    // Add common error responses
                    responses.addApiResponse("400", createErrorResponse("Bad Request", "VALIDATION_ERROR"));
                    responses.addApiResponse("401", createErrorResponse("Unauthorized", "UNAUTHORIZED"));
                    responses.addApiResponse("403", createErrorResponse("Forbidden", "FORBIDDEN"));
                    responses.addApiResponse("429", createErrorResponse("Too Many Requests", "RATE_LIMIT_EXCEEDED"));
                    responses.addApiResponse("500", createErrorResponse("Internal Server Error", "INTERNAL_ERROR"));
                })
            );
            
            // Add global schemas
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            
            // Add Money schema with validation
            openApi.getComponents().addSchemas("Money", 
                new Schema<String>()
                    .type("string")
                    .pattern("^\\d+\\.\\d{4}$")
                    .description("Monetary amount with exactly 4 decimal places")
                    .example("100.0000")
            );
            
            // Add Currency enum schema
            openApi.getComponents().addSchemas("Currency",
                new Schema<String>()
                    .type("string")
                    .enum(Arrays.asList("GOLD", "SWEEPS"))
                    .description("Currency type - GOLD is purchased, SWEEPS is promotional")
            );
            
            // Add State code schema
            openApi.getComponents().addSchemas("StateCode",
                new Schema<String>()
                    .type("string")
                    .pattern("^[A-Z]{2}$")
                    .description("US state code (2 letters). WA and ID are restricted for Sweeps play")
                    .example("CA")
            );
        };
    }
    
    private OpenApiCustomizer addSecurityItem() {
        return openApi -> openApi.addSecurityItem(
            new SecurityRequirement().addList("bearerAuth")
        );
    }
    
    private OpenApiCustomizer addAdminExamples() {
        return openApi -> {
            // Add admin-specific examples
            openApi.getComponents().addExamples("DualApprovalRequired",
                new Example()
                    .summary("Dual Approval Required Response")
                    .value(Map.of(
                        "status", "PENDING_APPROVAL",
                        "approval_id", "APR_01J8XYZABC",
                        "required_approvers", 2,
                        "current_approvers", 1,
                        "message", "Transaction requires dual approval"
                    ))
            );
        };
    }
    
    private ApiResponse createErrorResponse(String description, String errorCode) {
        return new ApiResponse()
            .description(description)
            .content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>()
                    .$ref("#/components/schemas/ErrorResponse"))
                    .example(Map.of(
                        "error", errorCode,
                        "message", description,
                        "timestamp", "2024-01-01T00:00:00Z",
                        "trace_id", "TRC-1234567890"
                    ))
            ));
    }
}
