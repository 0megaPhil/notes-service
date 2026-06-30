package com.notetaking.notes;

import com.notetaking.notes.domain.User;
import com.notetaking.notes.dto.CreateNoteInput;
import com.notetaking.notes.graphql.NoteController;
import com.notetaking.notes.graphql.TeamController;
import com.notetaking.notes.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Real integration tests for the GraphQL controllers.
 * Exercises the @Query/@Mutation mappings with real services and reactive repositories.
 * Uses Awaitility for non-blocking async setup checks (consistent with WebFlux/Reactor).
 */
@SpringBootTest
@ActiveProfiles("test")
class GraphQLIntegrationTest {

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private NoteController noteController;

    @Autowired
    private TeamController teamController;

    @Autowired
    private UserRepository userRepository;

    @Test
    void noteControllerCreatesAndReturnsPersonalNote() {
        // Non-blocking save + Awaitility wait for consistency with Reactor
        userRepository.save(new User(ALICE, "Alice", "a@test", Instant.now())).subscribe();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            StepVerifier.create(userRepository.findById(ALICE))
                .expectNextCount(1)
                .verifyComplete()
        );

        StepVerifier.create(noteController.createNote(new CreateNoteInput(
                        "Integration Personal", "Created via controller test", null)))
                .assertNext(note -> {
                    assertThat(note.title()).isEqualTo("Integration Personal");
                    assertThat(note.teamId()).isNull();
                })
                .verifyComplete();

        Flux<com.notetaking.notes.domain.Note> notes = noteController.myNotes(null, 10, 0);
        StepVerifier.create(notes)
                .expectNextMatches(note -> note.title().equals("Integration Personal"))
                .thenCancel()
                .verify();
    }

    @Test
    void teamControllerCreatesTeam() {
        // Non-blocking save + Awaitility wait
        userRepository.save(new User(ALICE, "Alice", "a@test", Instant.now())).subscribe();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            StepVerifier.create(userRepository.findById(ALICE))
                .expectNextCount(1)
                .verifyComplete()
        );

        StepVerifier.create(teamController.createTeam("GraphQL Test Team"))
                .assertNext(team -> assertThat(team.name()).isEqualTo("GraphQL Test Team"))
                .verifyComplete();
    }

}
