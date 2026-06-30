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

@Controller
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @QueryMapping
    public Mono<Note> note(@Argument UUID id) {
        return noteService.getNoteById(id);
    }

    @QueryMapping
    public Flux<Note> myNotes(@Argument UUID teamId,
                              @Argument Integer limit,
                              @Argument Integer offset) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : 20;
        int effectiveOffset = (offset != null && offset > 0) ? offset : 0;
        return noteService.getMyNotes(teamId, effectiveLimit, effectiveOffset);
    }

    @QueryMapping
    public Flux<Note> searchNotes(@Argument String query,
                                  @Argument UUID teamId,
                                  @Argument Integer limit) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : 20;
        return noteService.searchNotes(query, teamId, effectiveLimit);
    }

    @MutationMapping
    public Mono<Note> createNote(@Argument CreateNoteInput input) {
        return noteService.createNote(input.title(), input.content(), input.teamId());
    }

    @MutationMapping
    public Mono<Note> updateNote(@Argument UUID id, @Argument UpdateNoteInput input) {
        return noteService.updateNote(id, input.title(), input.content());
    }

    @MutationMapping
    public Mono<Boolean> deleteNote(@Argument UUID id) {
        return noteService.deleteNote(id);
    }
}
