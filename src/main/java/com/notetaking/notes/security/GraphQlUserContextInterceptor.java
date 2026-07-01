package com.notetaking.notes.security;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebGraphQlInterceptor to reliably populate the CurrentUser in Reactor context
 * for GraphQL requests using the X-User-Id or demo Bearer header.
 * <p>
 * This ensures the current user is available inside @QueryMapping / @MutationMapping
 * methods even if the outer WebFilter context propagation has edge cases for the
 * GraphQL handler.
 */
@Component
public class GraphQlUserContextInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String userId = request.getHeaders().getFirst(CurrentUser.HEADER_NAME);
        if (userId == null || userId.isBlank()) {
            // also check Bearer for completeness
            String auth = request.getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer demo-jwt-")) {
                userId = auth.substring("Bearer demo-jwt-".length());
            }
        }
        if (userId != null && !userId.isBlank()) {
            try {
                UUID uid = UUID.fromString(userId);
                CurrentUser cu = new CurrentUser(uid, "User-" + uid.toString().substring(0, 8));
                return chain.next(request)
                    .contextWrite(ctx -> ctx.put(CurrentUser.class, cu));
            } catch (IllegalArgumentException ignored) {
                // fall through to no context
            }
        }
        return chain.next(request);
    }
}
