package com.notetaking.notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response from successful demo login.
 * The returned token can be used as Authorization: Bearer &lt;token&gt; on GraphQL calls.
 *
 * @param token  demo token (format: demo-jwt-&lt;userId&gt;)
 * @param userId echoed user id
 */
@Schema(description = "Demo authentication token response")
public record LoginResponse(
    @Schema(
        description = "Demo JWT-like token. Send as 'Bearer <token>' in the Authorization header.",
        example = "demo-jwt-11111111-1111-1111-1111-111111111111"
    )
    String token,

    @Schema(
        description = "The user id for which the token was issued",
        example = "11111111-1111-1111-1111-111111111111"
    )
    String userId
) {}
