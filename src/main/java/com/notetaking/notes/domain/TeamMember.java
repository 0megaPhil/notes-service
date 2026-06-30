package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Association between a user and a team, including the assigned role.
 *
 * @param id       membership identifier
 * @param teamId   referenced team
 * @param userId   referenced user
 * @param role     role within the team
 * @param joinedAt when the membership was created
 */
@Table("team_members")
public record TeamMember(
    @Id UUID id,
    UUID teamId,
    UUID userId,
    Role role,
    Instant joinedAt
) {}
