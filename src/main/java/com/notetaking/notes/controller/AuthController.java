package com.notetaking.notes.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId format provided for login: {}", userIdStr);
            throw new IllegalArgumentException("Invalid userId format");
        }

        // Simple "JWT" for demo - in production use proper signing
        String token = "demo-jwt-" + userId;
        log.info("User {} logged in (demo JWT issued)", userId);
        return Map.of("token", token, "userId", userId.toString());
    }
}
