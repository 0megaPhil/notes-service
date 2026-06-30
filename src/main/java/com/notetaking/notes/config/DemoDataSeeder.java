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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Dedicated component for seeding demo data.
 * This separates initialization concerns from core application logic.
 * 
 * The seeder can be invoked:
 * - Automatically via conditional CommandLineRunner (see DataInitializer)
 * - Manually from tests or admin endpoints
 * - Skipped entirely in production
 */
@Service
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NoteRepository noteRepository;

    /**
     * @param userRepository        users
     * @param teamRepository        teams
     * @param teamMemberRepository  memberships
     * @param noteRepository        notes
     */
    public DemoDataSeeder(UserRepository userRepository,
                          TeamRepository teamRepository,
                          TeamMemberRepository teamMemberRepository,
                          NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.noteRepository = noteRepository;
    }

    /**
     * Seeds a small set of demo users, teams, memberships and notes.
     * Idempotency is not guaranteed; intended for fresh dev DBs.
     *
     * @return Mono that completes when seeding finishes (or errors)
     */
    public Mono<Void> seed() {
        log.info("Starting demo data seeding...");

        Instant now = Instant.now();

        User alice = new User(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Alice", "alice@example.com", now);
        User bob = new User(UUID.fromString("22222222-2222-2222-2222-222222222222"), "Bob", "bob@example.com", now);
        User carol = new User(UUID.fromString("33333333-3333-3333-3333-333333333333"), "Carol", "carol@example.com", now);

        return Flux.just(alice, bob, carol)
                .flatMap(userRepository::save)
                .thenMany(Flux.just(
                        new Note(UUID.randomUUID(), "Alice's personal note",
                                "This is private to Alice. Ideas for the Q3 planning.",
                                alice.id(), null, now, now, 0L),
                        new Note(UUID.randomUUID(), "Team meeting notes - Sprint 42",
                                "## Agenda\n- Release timeline\n- Tech debt items\n- On-call rotation",
                                alice.id(), null, now, now, 0L)
                ))
                .flatMap(noteRepository::save)
                .then(teamRepository.save(new Team(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "Engineering",
                        alice.id(),
                        now
                )))
                .flatMap(team -> {
                    TeamMember aliceOwner = new TeamMember(UUID.randomUUID(), team.id(), alice.id(), Role.OWNER, now);
                    TeamMember bobMember = new TeamMember(UUID.randomUUID(), team.id(), bob.id(), Role.MEMBER, now);
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
                        now,
                        now,
                        0L
                )))
                .then()
                .doOnSuccess(v -> log.info("Demo data seeding completed successfully."))
                .doOnError(e -> log.error("Demo data seeding failed", e));
    }
}
