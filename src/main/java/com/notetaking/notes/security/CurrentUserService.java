package com.notetaking.notes.security;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive accessor for the current user in the request context.
 * <p>
 * Falls back to a hard-coded demo user when no context is present (useful for tests/dev).
 */
@Service
public class CurrentUserService {

    /**
     * Returns the current user from Reactor context or a demo fallback.
     */
    public Mono<CurrentUser> getCurrentUser() {
        return Mono.deferContextual(ctx ->
            ctx.<CurrentUser>getOrEmpty(CurrentUser.class)
                .map(Mono::just)
                .orElseGet(() -> Mono.just(new CurrentUser(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Demo User"
                )))
        );
    }

    /**
     * Convenience for just the ID.
     */
    public Mono<UUID> getCurrentUserId() {
        return getCurrentUser().map(CurrentUser::id);
    }
}
