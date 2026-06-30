package com.notetaking.notes.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

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
            throw new IllegalArgumentException("Invalid userId format");
        }

        // Simple "JWT" for demo - in production use proper signing
        String token = "demo-jwt-" + userId;
        return Map.of("token", token, "userId", userId.toString());
    }
}
