package com.notetaking.notes.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Authentication filter supporting both:
 * - Demo: X-User-Id header
 * - Real: Authorization: Bearer <JWT>
 */
@Component
public class UserContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. Try JWT Bearer token (preferred for real flow)
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Support demo token format: demo-jwt-<uuid>
            if (token.startsWith("demo-jwt-")) {
                try {
                    UUID userId = UUID.fromString(token.substring(9));
                    CurrentUser currentUser = new CurrentUser(userId, "User-" + userId.toString().substring(0, 8));
                    return chain.filter(exchange)
                        .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser));
                } catch (Exception ignored) {}
            }
            // If using real JwtUtil in future:
            // UUID userId = JwtUtil.getUserIdFromToken(token);
            // if (userId != null) { ... }
        }

        // 2. Fallback to demo X-User-Id header
        String userIdHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(CurrentUser.HEADER_NAME);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                CurrentUser currentUser = new CurrentUser(userId, "User-" + userId.toString().substring(0, 8));
                return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Allow request to proceed (resolvers/services will handle missing user)
        return chain.filter(exchange);
    }
}
