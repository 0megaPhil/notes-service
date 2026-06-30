package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Application user (for demo seeding / future expansion).
 *
 * @param id        identifier
 * @param name      display name
 * @param email     contact email
 * @param createdAt registration time
 */
@Table("USERS")
public record User(
    @Id UUID id,
    String name,
    String email,
    Instant createdAt
) {}
