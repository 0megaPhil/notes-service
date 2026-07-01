package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A note belonging to a user. Can be personal (teamId null) or shared with a team.
 * <p>
 * Uses optimistic locking via {@code version}.
 *
 * @param id        unique id
 * @param title     note title
 * @param content   markdown/text body
 * @param ownerId   creator/owner
 * @param teamId    owning team or null for personal
 * @param createdAt creation time
 * @param updatedAt last update time
 * @param version   optimistic lock version
 */
@Table("NOTES")
public record Note(
    @Id UUID id,
    String title,
    String content,
    @Column("OWNER_ID") UUID ownerId,
    @Column("TEAM_ID") UUID teamId,           // null = personal note
    @Column("CREATED_AT") Instant createdAt,
    @Column("UPDATED_AT") Instant updatedAt,
    @Version Long version   // Optimistic locking - good practice
) {
    /**
     * Returns a copy with updated title/content and new timestamp.
     * Nulls for title/content mean "keep existing".
     */
    public Note withUpdatedContent(String newTitle, String newContent, Instant now) {
        return new Note(
            id,
            newTitle != null ? newTitle : title,
            newContent != null ? newContent : content,
            ownerId,
            teamId,
            createdAt,
            now,
            version
        );
    }
}
