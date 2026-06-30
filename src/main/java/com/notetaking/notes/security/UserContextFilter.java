package com.notetaking.notes.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFilter that establishes the {@link CurrentUser} in the Reactor context
 * from either a demo Bearer token or the X-User-Id header.
 * <p>
 * Non-blocking and tolerant of bad input (falls back gracefully).
 */
@Component
public class UserContextFilter implements WebFilter {

    /**
     * Inspects request headers and populates the user in context when possible.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. Try Bearer (demo-jwt-...) first
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (token.startsWith("demo-jwt-")) {
                return attachUserAndContinue(token.substring(9), exchange, chain);
            }
            // Future: real JWT path here.
        }

        // 2. Fallback X-User-Id header (demo)
        String userIdHeader = exchange.getRequest().getHeaders().getFirst(CurrentUser.HEADER_NAME);
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return attachUserAndContinue(userIdHeader, exchange, chain);
        }

        // Proceed without user (downstream will use demo fallback or reject)
        return chain.filter(exchange);
    }

    private Mono<Void> attachUserAndContinue(String userIdStr, ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.fromCallable(() -> UUID.fromString(userIdStr))
            .map(userId -> new CurrentUser(userId, "User-" + userId.toString().substring(0, 8)))
            .flatMap(currentUser -> chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser)))
            .onErrorResume(IllegalArgumentException.class, e -> chain.filter(exchange));
    }
}
