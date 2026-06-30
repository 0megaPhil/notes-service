package com.notetaking.notes.service;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.repository.NoteRepository;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.security.CurrentUser;
import com.notetaking.notes.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NoteService} using Mockito + StepVerifier.
 * Focuses on reactive flows and permission-less paths (mocks current user).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteServiceTest {

    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NoteService noteService;

    /**
     * Stubs the current user for all test cases (Alice).
     */
    @BeforeEach
    void setup() {
        lenient().when(currentUserService.getCurrentUser())
                .thenReturn(Mono.just(new CurrentUser(ALICE_ID, "Alice")));
        lenient().when(currentUserService.getCurrentUserId())
                .thenReturn(Mono.just(ALICE_ID));
    }

    /**
     * Verifies create for a personal note succeeds and title is preserved.
     */
    @Test
    void createNoteReturnsSavedNote() {
        Note savedNote = new Note(UUID.randomUUID(), "Test Title", "Test Content", ALICE_ID, null, Instant.now(), Instant.now(), 0L);
        when(noteRepository.save(any(Note.class))).thenReturn(Mono.just(savedNote));

        StepVerifier.create(noteService.createNote("Test Title", "Test Content", null))
                .expectNextMatches(note -> note.title().equals("Test Title"))
                .verifyComplete();
    }

    /**
     * Verifies that the owner can retrieve their own personal note.
     */
    @Test
    void getNoteByIdReturnsOwnedNote() {
        UUID noteId = UUID.randomUUID();
        Note ownedNote = new Note(noteId, "Owned", "Content", ALICE_ID, null, Instant.now(), Instant.now(), 0L);
        when(noteRepository.findById(noteId)).thenReturn(Mono.just(ownedNote));

        StepVerifier.create(noteService.getNoteById(noteId))
                .expectNextMatches(note -> note.id().equals(noteId))
                .verifyComplete();
    }
}
