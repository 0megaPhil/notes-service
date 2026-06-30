package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("teams")
public record Team(
    @Id UUID id,
    String name,
    UUID createdBy,
    Instant createdAt
) {}
