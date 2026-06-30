package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("team_members")
public record TeamMember(
    @Id UUID id,
    UUID teamId,
    UUID userId,
    Role role,
    Instant joinedAt
) {}
