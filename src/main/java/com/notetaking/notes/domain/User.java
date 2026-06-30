package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public record User(
    @Id UUID id,
    String name,
    String email,
    Instant createdAt
) {}
