package com.notetaking.notes.service;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.repository.NoteRepository;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Core business service for note operations.
 * <p>
 * All operations are fully reactive and enforce ownership + team membership
 * permissions via the {@link CurrentUserService} and team membership lookups.
 */
@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository noteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;
    private final R2dbcEntityTemplate entityTemplate;

    /**
     * Constructs the service with required repositories.
     */
    public NoteService(NoteRepository noteRepository,
                       TeamMemberRepository teamMemberRepository,
                       CurrentUserService currentUserService,
                       R2dbcEntityTemplate entityTemplate) {
        this.noteRepository = noteRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
        this.entityTemplate = entityTemplate;
    }

    /**
     * Retrieves a single note by id if the current user has access.
     *
     * @param id the note identifier
     * @return the note or empty Mono if not found or no access
     */
    public Mono<Note> getNoteById(UUID id) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> hasAccess(note, userId))
            );
    }

    /**
     * Returns notes visible to the current user (personal + team notes they belong to).
     * Supports optional filtering by team and pagination via limit/offset.
     *
     * @param teamId optional team filter
     * @param limit  max results (defaulted upstream)
     * @param offset skip count
     * @return flux of accessible notes
     */
    public Flux<Note> getMyNotes(UUID teamId, int limit, int offset) {
        int effectiveLimit = Math.max(limit, 1);
        int effectiveOffset = Math.max(offset, 0);

        return currentUserService.getCurrentUserId()
            .flatMapMany(userId -> {
                if (teamId != null) {
                    return noteRepository.findByTeamId(teamId)
                        .filterWhen(note -> hasAccess(note, userId));
                }
                // Personal notes + all notes from teams the user belongs to.
                // For simplicity in MVP: owned by user OR in any of their teams.
                return teamMemberRepository.findByUserId(userId)
                    .map(TeamMember::teamId)
                    .flatMap(noteRepository::findByTeamId)
                    .concatWith(noteRepository.findByOwnerId(userId))
                    .distinct(Note::id);
            })
            .skip(effectiveOffset)
            .take(effectiveLimit);
    }

    /**
     * Searches notes visible to the current user by title or content (case-insensitive contains).
     *
     * @param query  search text
     * @param teamId optional team scope
     * @param limit  result cap
     * @return matching notes
     */
    public Flux<Note> searchNotes(String query, UUID teamId, int limit) {
        return currentUserService.getCurrentUserId()
            .flatMapMany(userId -> {
                String q = query.toLowerCase();
                if (teamId != null) {
                    return noteRepository.findByTeamId(teamId)
                        .filterWhen(note -> hasAccess(note, userId)
                            .map(hasAccess -> hasAccess && matchesSearch(note, q)));
                }
                return getMyNotes(null, limit * 2, 0) // broader then filter
                    .filter(note -> note.title().toLowerCase().contains(q) || note.content().toLowerCase().contains(q));
            })
            .take(limit);
    }

    /**
     * Creates and persists a new note.
     * If teamId is supplied, verifies the caller is a team member.
     *
     * @param title   note title
     * @param content body
     * @param teamId  optional team (null = personal)
     * @return the saved note
     */
    public Mono<Note> createNote(String title, String content, UUID teamId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                UUID userId = currentUser.id();
                return Mono.justOrEmpty(teamId)
                    .flatMap(tid -> ensureTeamMembership(userId, tid).thenReturn(tid))
                    .map(finalTeamId -> new Note(
                        UUID.randomUUID(),
                        title,
                        content,
                        userId,
                        finalTeamId,
                        Instant.now(),
                        Instant.now(),
                        null
                    ))
                    .defaultIfEmpty(new Note(
                        UUID.randomUUID(),
                        title,
                        content,
                        userId,
                        null,
                        Instant.now(),
                        Instant.now(),
                        null
                    ));
            })
            .flatMap(entityTemplate::insert);
    }

    /**
     * Updates title/content of a note the current user can access.
     *
     * @param id      note id
     * @param title   optional new title
     * @param content optional new content
     * @return updated note
     */
    public Mono<Note> updateNote(UUID id, String title, String content) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> hasAccess(note, userId))
                .flatMap(note -> {
                    Note updated = note.withUpdatedContent(title, content, Instant.now());
                    log.info("User {} updating note {}", userId, id);
                    return noteRepository.save(updated);
                })
            );
    }

    /**
     * Deletes a note if the current user is the owner or a team admin.
     *
     * @param id the note id
     * @return true if deleted, false otherwise
     */
    public Mono<Boolean> deleteNote(UUID id) {
        return currentUserService.getCurrentUserId()
            .flatMap(userId -> noteRepository.findById(id)
                .filterWhen(note -> canDelete(note, userId))
                .flatMap(note -> {
                    log.warn("User {} deleting note {}", userId, id);
                    return noteRepository.delete(note).thenReturn(true);
                })
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

    private static boolean matchesSearch(Note note, String q) {
        return note.title().toLowerCase().contains(q) ||
               note.content().toLowerCase().contains(q);
    }
}
