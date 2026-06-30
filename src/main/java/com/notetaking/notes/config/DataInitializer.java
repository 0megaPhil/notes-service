package com.notetaking.notes.config;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.domain.User;
import com.notetaking.notes.repository.NoteRepository;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.repository.TeamRepository;
import com.notetaking.notes.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository,
                           TeamRepository teamRepository,
                           TeamMemberRepository teamMemberRepository,
                           NoteRepository noteRepository) {
        return args -> {
            User alice = new User(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Alice", "alice@example.com", Instant.now());
            User bob = new User(UUID.fromString("22222222-2222-2222-2222-222222222222"), "Bob", "bob@example.com", Instant.now());
            User carol = new User(UUID.fromString("33333333-3333-3333-3333-333333333333"), "Carol", "carol@example.com", Instant.now());

            // Properly sequence everything
            Flux.just(alice, bob, carol)
                .flatMap(userRepository::save)
                .thenMany(Flux.just(
                    new Note(UUID.randomUUID(), "Alice's personal note",
                        "This is private to Alice. Ideas for the Q3 planning.",
                        alice.id(), null, Instant.now(), Instant.now(), 0L),
                    new Note(UUID.randomUUID(), "Team meeting notes - Sprint 42",
                        "## Agenda\n- Release timeline\n- Tech debt items\n- On-call rotation",
                        alice.id(), null, Instant.now(), Instant.now(), 0L)
                ))
                .flatMap(noteRepository::save)
                .then(teamRepository.save(new Team(
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                    "Engineering",
                    alice.id(),
                    Instant.now()
                )))
                .flatMap(team -> {
                    TeamMember aliceOwner = new TeamMember(UUID.randomUUID(), team.id(), alice.id(), Role.OWNER, Instant.now());
                    TeamMember bobMember = new TeamMember(UUID.randomUUID(), team.id(), bob.id(), Role.MEMBER, Instant.now());
                    return Flux.just(aliceOwner, bobMember)
                        .flatMap(teamMemberRepository::save)
                        .then(Mono.just(team));
                })
                .flatMap(team -> noteRepository.save(new Note(
                    UUID.randomUUID(),
                    "Shared Engineering Roadmap",
                    "Q3 goals:\n* New GraphQL schema\n* Improve reactive performance\n* Onboard two new engineers",
                    alice.id(),
                    team.id(),
                    Instant.now(),
                    Instant.now(),
                    0L
                )))
                .then()
                .doOnError(e -> System.err.println("Data initialization failed: " + e.getMessage()))
                .subscribe();   // Fire-and-forget; keeps everything fully non-blocking
        };
    }
}
