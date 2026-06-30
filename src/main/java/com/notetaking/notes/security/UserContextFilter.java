package com.notetaking.notes.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Demo authentication filter.
 * Expects header "X-User-Id: <uuid>".
 *
 * In a real system this would validate a JWT or call an auth service.
 */
@Component
public class UserContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userIdHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(CurrentUser.HEADER_NAME);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                // Put the user into Reactor context so services/resolvers can access it reactively
                CurrentUser currentUser = new CurrentUser(userId, "User-" + userId.toString().substring(0, 8));
                return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(CurrentUser.class, currentUser));
            } catch (IllegalArgumentException ignored) {
                // Invalid UUID header - will be handled as unauthorized below
            }
        }

        // No valid user header -> let security reject or allow anonymous for now
        // For this demo we allow proceeding but resolvers will check
        return chain.filter(exchange);
    }
}
