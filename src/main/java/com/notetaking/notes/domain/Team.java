package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A team that groups users for collaborative note sharing.
 *
 * @param id        unique identifier
 * @param name      display name of the team
 * @param createdBy user who created the team
 * @param createdAt creation timestamp
 */
@Table("teams")
public record Team(
    @Id UUID id,
    String name,
    UUID createdBy,
    Instant createdAt
) {}
