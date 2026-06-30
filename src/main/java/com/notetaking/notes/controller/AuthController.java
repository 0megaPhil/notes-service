package com.notetaking.notes.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public Mono<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");

        return Mono.justOrEmpty(userIdStr)
            .filter(str -> !str.isBlank())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("userId is required")))
            .flatMap(str -> Mono.fromCallable(() -> UUID.fromString(str))
                .onErrorMap(IllegalArgumentException.class, e -> {
                    log.warn("Invalid userId format provided for login: {}", userIdStr);
                    return new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid userId format", e);
                })
            )
            .map(userId -> {
                // Simple "JWT" for demo - in production use proper signing
                String token = "demo-jwt-" + userId;
                log.info("User {} logged in (demo JWT issued)", userId);
                return Map.of("token", token, "userId", userId.toString());
            });
    }
}
