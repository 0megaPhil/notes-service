package com.notetaking.notes.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Demo authentication endpoint.
 * <p>
 * Accepts a userId and returns a trivial demo token. Real auth would issue
 * signed JWTs or delegate to an identity provider.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Performs a demo login.
     *
     * @param body must contain "userId"
     * @return map with "token" and "userId", or error
     */
    @PostMapping("/login")
    public Mono<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");

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
                return Map.of("token", token, "userId", userId.toString());
            });
    }
}
