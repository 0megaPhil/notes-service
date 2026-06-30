package com.notetaking.notes.security;

import java.util.UUID;

/**
 * Simple representation of the authenticated user in the reactive context.
 * <p>
 * In production this would be populated from JWT/OAuth2 claims by a proper filter.
 *
 * @param id   user identifier (UUID)
 * @param name display name
 */
public record CurrentUser(
    UUID id,
    String name
) {

    /** Header used for the simple demo authentication fallback. */
    public static final String HEADER_NAME = "X-User-Id";
}
