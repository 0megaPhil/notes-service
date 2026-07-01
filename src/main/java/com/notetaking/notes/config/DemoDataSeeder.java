package com.notetaking.notes.config;

import com.notetaking.notes.domain.Note;
import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.domain.User;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
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

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final R2dbcEntityTemplate entityTemplate;

    /**
     * @param teamRepository        teams
     * @param teamMemberRepository  memberships
     * @param entityTemplate        for explicit INSERTs (prevents .save treating fixed-ID entities as updates)
     */
    public DemoDataSeeder(TeamRepository teamRepository,
                          TeamMemberRepository teamMemberRepository,
                          R2dbcEntityTemplate entityTemplate) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.entityTemplate = entityTemplate;
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

        UUID engineeringTeamId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // Clean any prior demo data for the fixed team so restarts with file-based H2 work cleanly
        Mono<Void> cleanup = teamMemberRepository.findByTeamId(engineeringTeamId)
                .flatMap(teamMemberRepository::delete)
                .then(teamRepository.findById(engineeringTeamId).flatMap(teamRepository::delete).then());

        // Users are pre-created via MERGE in schema.sql (avoids duplicate key on insert).
        // Use explicit inserts for team + members + notes (fixed/random IDs would be turned into no-op UPDATEs by .save).
        return cleanup
                .then(entityTemplate.insert(new Note(UUID.randomUUID(), "Alice's personal note",
                        "This is private to Alice. Ideas for the Q3 planning.",
                        alice.id(), null, now, now, null)))
                .then(entityTemplate.insert(new Note(UUID.randomUUID(), "Team meeting notes - Sprint 42",
                        "## Agenda\n- Release timeline\n- Tech debt items\n- On-call rotation",
                        alice.id(), null, now, now, null)))
                .then(entityTemplate.insert(new Note(UUID.randomUUID(), "Carol's personal note",
                        "Reminder for Carol: prepare the Q4 budget draft.",
                        carol.id(), null, now, now, null)))
                .then(entityTemplate.insert(new Team(
                        engineeringTeamId,
                        "Engineering",
                        alice.id(),
                        now
                )))
                .flatMap(team -> {
                    TeamMember aliceOwner = new TeamMember(UUID.randomUUID(), team.id(), alice.id(), Role.OWNER, now);
                    TeamMember bobMember = new TeamMember(UUID.randomUUID(), team.id(), bob.id(), Role.MEMBER, now);
                    TeamMember carolMember = new TeamMember(UUID.randomUUID(), team.id(), carol.id(), Role.MEMBER, now);
                    return entityTemplate.insert(aliceOwner)
                            .then(entityTemplate.insert(bobMember))
                            .then(entityTemplate.insert(carolMember))
                            .thenReturn(team);
                })
                .flatMap(team -> entityTemplate.insert(new Note(UUID.randomUUID(), "Carol's team note",
                        "Action items from last retro - shared with the whole Engineering team.",
                        carol.id(), team.id(), now, now, null))
                        .thenReturn(team))
                .then()
                .doOnSuccess(v -> log.info("Demo data seeding completed successfully."))
                .doOnError(e -> log.error("Demo data seeding failed", e));
    }
}
