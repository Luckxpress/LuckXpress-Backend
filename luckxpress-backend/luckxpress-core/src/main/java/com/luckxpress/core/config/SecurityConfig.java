package com.luckxpress.core.config;

import com.luckxpress.core.security.JwtAuthenticationEntryPoint;
import com.luckxpress.core.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Main security configuration for LuckXpress application
 * Configures JWT authentication, CORS, and security filter chain
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter authenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                         JwtAuthenticationFilter authenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager getAuthenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain configureSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // Disable CSRF for stateless API
        httpSecurity.csrf(csrfConfig -> csrfConfig.disable());
        
        // Configure CORS
        httpSecurity.cors(corsConfig -> 
            corsConfig.configurationSource(configureCorsSource()));
        
        // Configure authorization rules
        httpSecurity.authorizeHttpRequests(authzConfig -> {
            authzConfig
                // Public endpoints
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/health", "/api/v1/health/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Protected endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
                // All other requests need authentication
                .anyRequest().authenticated();
        });
        
        // Configure exception handling
        httpSecurity.exceptionHandling(exceptionConfig -> 
            exceptionConfig.authenticationEntryPoint(authenticationEntryPoint));
        
        // Configure session management
        httpSecurity.sessionManagement(sessionConfig -> 
            sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // Add JWT filter
        httpSecurity.addFilterBefore(
            authenticationFilter, 
            UsernamePasswordAuthenticationFilter.class
        );
        
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource configureCorsSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Configure allowed origins
        corsConfig.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1:*",
            "https://*.luckxpress.com"
        ));
        
        // Configure allowed methods
        corsConfig.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        
        // Configure allowed headers
        corsConfig.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Configure exposed headers
        corsConfig.setExposedHeaders(List.of(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization"
        ));
        
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(Duration.ofHours(1));
        
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/api/**", corsConfig);
        
        return corsSource;
    }
}
