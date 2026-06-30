package com.notetaking.notes.security;

import java.util.UUID;

/**
 * Simple representation of the authenticated user.
 * In production this would come from JWT / OAuth2 claims.
 */
public record CurrentUser(
    UUID id,
    String name
) {
    public static final String HEADER_NAME = "X-User-Id";
}
