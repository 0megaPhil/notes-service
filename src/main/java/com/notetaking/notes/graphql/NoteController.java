package com.notetaking.notes.graphql;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.dto.CreateNoteInput;
import com.notetaking.notes.dto.UpdateNoteInput;
import com.notetaking.notes.service.NoteService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GraphQL controller exposing note queries and mutations.
 * Delegates to {@link NoteService} for business logic and authorization.
 */
@Controller
public class NoteController {

    private static final int DEFAULT_LIMIT = 20;

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * Fetch a single note by id (authorization enforced in service).
     */
    @QueryMapping
    public Mono<Note> note(@Argument String id) {
        return noteService.getNoteById(UUID.fromString(id));
    }

    /**
     * Current user's notes (personal and team-scoped).
     */
    @QueryMapping
    public Flux<Note> myNotes(@Argument String teamId,
                              @Argument Integer limit,
                              @Argument Integer offset) {
        int effectiveLimit = resolveLimit(limit);
        int effectiveOffset = resolveOffset(offset);
        UUID teamUuid = (teamId != null && !teamId.isBlank()) ? UUID.fromString(teamId) : null;
        return noteService.getMyNotes(teamUuid, effectiveLimit, effectiveOffset);
    }

    /**
     * Full-text search over notes the caller can see.
     */
    @QueryMapping
    public Flux<Note> searchNotes(@Argument String query,
                                  @Argument String teamId,
                                  @Argument Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        UUID teamUuid = (teamId != null && !teamId.isBlank()) ? UUID.fromString(teamId) : null;
        return noteService.searchNotes(query, teamUuid, effectiveLimit);
    }

    private int resolveLimit(Integer limit) {
        return (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
    }

    private int resolveOffset(Integer offset) {
        return (offset != null && offset > 0) ? offset : 0;
    }


    /**
     * Creates a note. Personal or team scoped.
     */
    @MutationMapping
    public Mono<Note> createNote(@Argument CreateNoteInput input) {
        UUID teamUuid = (input.teamId() != null && !input.teamId().isBlank()) ? UUID.fromString(input.teamId()) : null;
        return noteService.createNote(input.title(), input.content(), teamUuid);
    }

    /**
     * Updates a note the caller has write access to.
     */
    @MutationMapping
    public Mono<Note> updateNote(@Argument String id, @Argument UpdateNoteInput input) {
        return noteService.updateNote(UUID.fromString(id), input.title(), input.content());
    }

    /**
     * Deletes a note the caller is allowed to delete.
     */
    @MutationMapping
    public Mono<Boolean> deleteNote(@Argument String id) {
        return noteService.deleteNote(UUID.fromString(id));
    }
}
