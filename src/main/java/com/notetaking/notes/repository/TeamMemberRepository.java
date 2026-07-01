package com.notetaking.notes.repository;

import com.notetaking.notes.domain.TeamMember;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for {@link TeamMember} associations.
 */
public interface TeamMemberRepository extends ReactiveCrudRepository<TeamMember, UUID> {

    /**
     * Finds all members of a team.
     */
    Flux<TeamMember> findByTeamId(UUID teamId);

    /**
     * Finds all team memberships for a user.
     */
    @Query("SELECT * FROM TEAM_MEMBERS WHERE USER_ID = :userId")
    Flux<TeamMember> findByUserId(UUID userId);

    /**
     * Finds a specific membership (used for permission checks).
     */
    Mono<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Deletes a membership (used for remove member).
     */
    Mono<Void> deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
