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
            if (token.startsWith("demo-jwt-")) {
                return Mono.fromCallable(() -> UUID.fromString(token.substring(9)))
                    .map(userId -> new CurrentUser(userId, "User-" + userId.toString().substring(0, 8)))
                    .flatMap(currentUser -> chain.filter(exchange)
                        .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser)))
                    .onErrorResume(IllegalArgumentException.class, e -> chain.filter(exchange));
            }
            // If using real JwtUtil in future:
            // return Mono.fromCallable(() -> JwtUtil.getUserIdFromToken(token))
            //     .flatMap(...) ...
        }

        // 2. Fallback to demo X-User-Id header
        String userIdHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(CurrentUser.HEADER_NAME);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Mono.fromCallable(() -> UUID.fromString(userIdHeader))
                .map(userId -> new CurrentUser(userId, "User-" + userId.toString().substring(0, 8)))
                .flatMap(currentUser -> chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser)))
                .onErrorResume(IllegalArgumentException.class, e -> chain.filter(exchange));
        }

        // Allow request to proceed (resolvers/services will handle missing user)
        return chain.filter(exchange);
    }
}
