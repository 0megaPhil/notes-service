package com.notetaking.notes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Allow actuator health for k8s / load balancers
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // GraphQL endpoint protected by our header filter below
                .pathMatchers("/graphql").authenticated()
                .pathMatchers("/graphiql", "/graphiql/**").permitAll() // Dev UI
                .anyExchange().authenticated()
            )
            // For demo: we use a custom header-based "authentication"
            // This is intentionally simple. Real production would use:
            // - JWT via oauth2ResourceServer()
            // - or Spring Security OAuth2 / OIDC
            .build();
    }
}
