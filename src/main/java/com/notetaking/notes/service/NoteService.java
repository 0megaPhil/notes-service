package com.notetaking.notes.service;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.repository.NoteRepository;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;

    public NoteService(NoteRepository noteRepository,
                       TeamMemberRepository teamMemberRepository,
                       CurrentUserService currentUserService) {
        this.noteRepository = noteRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
    }

    public Mono<Note> getNoteById(UUID id) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> hasAccess(note, userId))
            );
    }

    public Flux<Note> getMyNotes(UUID teamId, int limit, int offset) {
        return currentUserService.getCurrentUserId()
            .flatMapMany(userId -> {
                PageRequest page = PageRequest.of(offset / Math.max(limit, 1), limit);
                if (teamId != null) {
                    return noteRepository.findByTeamId(teamId)
                        .filterWhen(note -> hasAccess(note, userId));
                }
                // Personal notes + all notes from teams the user belongs to
                return teamMemberRepository.findByUserId(userId)
                    .map(TeamMember::teamId)
                    .collectList()
                    .flatMapMany(teamIds -> {
                        if (teamIds.isEmpty()) {
                            return noteRepository.findByOwnerId(userId);
                        }
                        // For simplicity: return notes owned by user OR in any of their teams
                        return Flux.fromIterable(teamIds)
                            .flatMap(noteRepository::findByTeamId)
                            .concatWith(noteRepository.findByOwnerId(userId))
                            .distinct(Note::id);
                    });
            })
            .take(limit);
    }

    public Flux<Note> searchNotes(String query, UUID teamId, int limit) {
        return currentUserService.getCurrentUserId()
            .flatMapMany(userId -> {
                String q = query.toLowerCase();
                if (teamId != null) {
                    return noteRepository.findByTeamId(teamId)
                        .filterWhen(note -> hasAccess(note, userId)
                            .map(has -> has && (note.title().toLowerCase().contains(q) || note.content().toLowerCase().contains(q))));
                }
                return getMyNotes(null, limit * 2, 0) // broader then filter
                    .filter(note -> note.title().toLowerCase().contains(q) || note.content().toLowerCase().contains(q));
            })
            .take(limit);
    }

    public Mono<Note> createNote(String title, String content, UUID teamId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                UUID userId = currentUser.id();
                Mono<UUID> resolvedTeam = (teamId != null)
                    ? ensureTeamMembership(userId, teamId).then(Mono.just(teamId))
                    : Mono.just((UUID) null);

                return resolvedTeam.map(finalTeamId -> new Note(
                    UUID.randomUUID(),
                    title,
                    content,
                    userId,
                    finalTeamId,
                    Instant.now(),
                    Instant.now(),
                    0L
                ));
            })
            .flatMap(noteRepository::save);
    }

    public Mono<Note> updateNote(UUID id, String title, String content) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> hasAccess(note, userId))
                .flatMap(note -> {
                    Note updated = note.withUpdatedContent(title, content, Instant.now());
                    return noteRepository.save(updated);
                })
            );
    }

    public Mono<Boolean> deleteNote(UUID id) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> canDelete(note, userId))
                .flatMap(note -> noteRepository.delete(note).thenReturn(true))
            )
            .defaultIfEmpty(false);
    }

    // --- Helper permission logic (centralized - good practice) ---
    // Fully reactive to respect WebFlux / Project Reactor principles.

    private Mono<Boolean> hasAccess(Note note, UUID userId) {
        if (note.ownerId().equals(userId)) {
            return Mono.just(true);
        }
        if (note.teamId() == null) {
            return Mono.just(false);
        }
        // All team members have access for this MVP
        return teamMemberRepository.findByTeamIdAndUserId(note.teamId(), userId)
            .hasElement();
    }

    private Mono<Boolean> canDelete(Note note, UUID userId) {
        if (note.ownerId().equals(userId)) {
            return Mono.just(true);
        }
        return isTeamAdmin(userId, note.teamId());
    }

    private Mono<Boolean> ensureTeamMembership(UUID userId, UUID teamId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(m -> true)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User is not a member of the team")));
    }

    private Mono<Boolean> isTeamAdmin(UUID userId, UUID teamId) {
        if (teamId == null) {
            return Mono.just(false);
        }
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(m -> m.role() == Role.OWNER || m.role() == Role.ADMIN)
            .defaultIfEmpty(false);
    }
}
