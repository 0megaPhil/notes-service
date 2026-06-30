package com.notetaking.notes.repository;

import com.notetaking.notes.domain.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NoteRepository extends ReactiveCrudRepository<Note, UUID> {

    Flux<Note> findByOwnerId(UUID ownerId);

    Flux<Note> findByTeamId(UUID teamId);

    Flux<Note> findByOwnerIdOrTeamId(UUID ownerId, UUID teamId, Pageable pageable);

    Flux<Note> findByOwnerIdOrTeamIdAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            UUID ownerId, UUID teamId, String titleQuery, String contentQuery, Pageable pageable);

    // For simple access check
    Mono<Note> findByIdAndOwnerId(UUID id, UUID ownerId);
}
