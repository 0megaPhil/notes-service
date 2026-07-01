package com.notetaking.notes.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
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
@Table("TEAM_MEMBERS")
public record TeamMember(
    @Id UUID id,
    @Column("TEAM_ID") UUID teamId,
    @Column("USER_ID") UUID userId,
    Role role,
    @Column("JOINED_AT") Instant joinedAt
) {}
