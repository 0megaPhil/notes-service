package com.notetaking.notes.repository;

import com.notetaking.notes.domain.Team;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for {@link Team} entities.
 */
public interface TeamRepository extends ReactiveCrudRepository<Team, UUID> {

    /**
     * Finds teams created by the specified user.
     *
     * @param createdBy creator user id
     * @return flux of teams
     */
    Flux<Team> findByCreatedBy(UUID createdBy);

    @Query("SELECT t.* FROM TEAMS t JOIN TEAM_MEMBERS tm ON t.ID = tm.TEAM_ID WHERE tm.USER_ID = :userId")
    Flux<Team> findByMemberUserId(UUID userId);
}
