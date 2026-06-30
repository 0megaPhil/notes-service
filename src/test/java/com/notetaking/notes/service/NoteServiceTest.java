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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NoteService noteService;

    private final UUID aliceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID noteId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        lenient().when(currentUserService.getCurrentUser())
                .thenReturn(Mono.just(new CurrentUser(aliceId, "Alice")));
        lenient().when(currentUserService.getCurrentUserId())
                .thenReturn(Mono.just(aliceId));
        // Default: no team membership (for owner tests the early return is used)
        lenient().when(teamMemberRepository.findByTeamIdAndUserId(any(), any()))
                .thenReturn(Mono.empty());
    }

    @Test
    void servicesAreAvailable() {
        assert noteService != null;
    }
}
