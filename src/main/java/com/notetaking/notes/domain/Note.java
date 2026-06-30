package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("notes")
public record Note(
    @Id UUID id,
    String title,
    String content,
    UUID ownerId,
    UUID teamId,           // null = personal note
    Instant createdAt,
    Instant updatedAt,
    @Version Long version   // Optimistic locking - good practice
) {
    public Note withUpdatedContent(String newTitle, String newContent, Instant now) {
        return new Note(id, newTitle != null ? newTitle : title, 
                       newContent != null ? newContent : content, 
                       ownerId, teamId, createdAt, now, version);
    }
}
