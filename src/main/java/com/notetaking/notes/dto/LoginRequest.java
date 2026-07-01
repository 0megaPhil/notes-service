package com.notetaking.notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for the demo authentication endpoint.
 *
 * @param userId the UUID string of a known demo user (Alice, Bob, or Carol)
 */
@Schema(description = "Login request body for demo authentication")
public record LoginRequest(
    @Schema(
        description = "UUID of the demo user to impersonate",
        example = "11111111-1111-1111-1111-111111111111",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String userId
) {}
