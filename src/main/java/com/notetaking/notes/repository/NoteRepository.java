package com.notetaking.notes.repository;

import com.notetaking.notes.domain.Note;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for {@link Note} entities.
 * <p>
 * Extends Spring Data R2DBC reactive CRUD with custom query methods.
 */
public interface NoteRepository extends ReactiveCrudRepository<Note, UUID> {

    /**
     * Finds all notes owned directly by the user (personal notes).
     *
     * @param ownerId the user id
     * @return flux of matching notes
     */
    Flux<Note> findByOwnerId(UUID ownerId);

    /**
     * Finds all notes belonging to the given team.
     *
     * @param teamId the team id
     * @return flux of team notes
     */
    Flux<Note> findByTeamId(UUID teamId);
}
