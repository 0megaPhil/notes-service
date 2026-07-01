package com.notetaking.notes.controller;

import com.notetaking.notes.dto.LoginRequest;
import com.notetaking.notes.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Demo authentication REST endpoint.
 * <p>
 * Provides a simple login that returns a demo token usable with the primary
 * GraphQL API (via Authorization: Bearer &lt;token&gt; or X-User-Id fallback).
 * <p>
 * Swagger documents these REST endpoints only. The main API surface is GraphQL
 * (explore at /graphiql or via introspection).
 */
@Tag(name = "Authentication", description = "Demo authentication for obtaining tokens used with the GraphQL API")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Performs a demo login.
     * <p>
     * Accepts a known demo user UUID and returns a trivial token.
     * In a real system this would validate credentials and issue a signed JWT.
     *
     * @param request contains the target demo userId (UUID string)
     * @return reactive response containing token and userId
     */
    @PostMapping("/login")
    @Operation(
        summary = "Demo login",
        description = "Exchange a demo userId (one of the seeded UUIDs) for a demo token. " +
                      "Use the returned token as `Authorization: Bearer <token>` header on GraphQL requests. " +
                      "See README for the list of demo user IDs (Alice, Bob, Carol)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Demo token successfully issued",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(
                    name = "Alice example",
                    value = "{\"token\":\"demo-jwt-11111111-1111-1111-1111-111111111111\"," +
                            "\"userId\":\"11111111-1111-1111-1111-111111111111\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "userId is missing or not a valid UUID",
            content = @Content
        )
    })
    public Mono<LoginResponse> login(
        @RequestBody
        @Parameter(description = "Request containing the demo user UUID", required = true)
        LoginRequest request
    ) {
        String userIdStr = request.userId();

        return Mono.justOrEmpty(userIdStr)
            .filter(str -> !str.isBlank())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("userId is required")))
            .flatMap(str -> Mono.fromCallable(() -> UUID.fromString(str))
                .onErrorMap(IllegalArgumentException.class, e -> {
                    log.warn("Invalid userId format provided for login: {}", userIdStr);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId format", e);
                })
            )
            .map(userId -> {
                // Simple demo token. Production: use signed JWT.
                String token = "demo-jwt-" + userId;
                log.info("User {} logged in (demo token issued)", userId);
                return new LoginResponse(token, userId.toString());
            });
    }
}
