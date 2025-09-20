package com.luckxpress.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration
 * CRITICAL: Configures Swagger/OpenAPI documentation for the LuckXpress API
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${luckxpress.api.version:1.0.0}")
    private String apiVersion;
    
    @Value("${luckxpress.api.server.url:http://localhost:8080}")
    private String serverUrl;
    
    @Bean
    public OpenAPI luckXpressOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url(serverUrl)
                    .description("LuckXpress API Server")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer Token Authentication")
                )
            );
    }
    
    private Info apiInfo() {
        return new Info()
            .title("LuckXpress Backend API")
            .description("""
                LuckXpress Backend API provides comprehensive sweepstakes gaming platform functionality 
                with strict compliance validation and dual-currency support.
                
                ## Key Features:
                - **Dual Currency System**: Gold Coins (GC) for social gaming, Sweeps Coins (SC) for prizes
                - **Compliance First**: State restrictions, KYC verification, and regulatory compliance
                - **Security**: JWT authentication, role-based access control, audit trails
                - **Financial Controls**: Dual/triple approval workflows, balance integrity, ledger system
                - **User Management**: Registration, profile management, self-exclusion tools
                
                ## Currency Types:
                - **GOLD**: Gold Coins - Social gaming currency (non-withdrawable)
                - **SWEEPS**: Sweeps Coins - Prize currency (withdrawable with KYC)
                
                ## Compliance Notes:
                - All monetary amounts use 4 decimal precision
                - State restrictions apply (ID, WA, MT, NV not supported)
                - KYC verification required for withdrawals
                - Age verification required (21+ only)
                - Comprehensive audit logging for all operations
                
                ## Authentication:
                Include JWT token in Authorization header: `Bearer <your-jwt-token>`
                """)
            .version(apiVersion)
            .contact(new Contact()
                .name("LuckXpress Development Team")
                .email("dev@luckxpress.com")
                .url("https://luckxpress.com")
            )
            .license(new License()
                .name("Proprietary")
                .url("https://luckxpress.com/license")
            );
    }
}
