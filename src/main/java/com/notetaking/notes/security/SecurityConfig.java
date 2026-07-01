package com.notetaking.notes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the reactive (WebFlux) stack.
 * <p>
 * Deliberately permissive for demo purposes (GraphQL protected at service layer).
 * See README for production hardening notes.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain: CORS, disabled CSRF, public paths, etc.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Allow actuator health for k8s / load balancers
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                // Public auth endpoint
                .pathMatchers("/auth/**").permitAll()
                // Allow the simple frontend (for demo)
                .pathMatchers("/", "/index.html", "/frontend/**").permitAll()
                // GraphQL is protected by our custom UserContextFilter + service layer permissions
                .pathMatchers("/graphql").permitAll()
                .pathMatchers("/graphiql", "/graphiql/**").permitAll() // Dev UI
                .pathMatchers("/debug/**").permitAll() // temp diagnostics
                .anyExchange().authenticated()
            )
            // For demo: we use a custom header-based "authentication"
            // This is intentionally simple. Real production would use:
            // - JWT via oauth2ResourceServer()
            // - or Spring Security OAuth2 / OIDC
            .build();
    }

    /**
     * Permissive CORS for demo (allows all origins). Restrict in production.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // In production, restrict this!
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
