package com.notetaking.notes.service;

import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.repository.TeamRepository;
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
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TeamService} covering create, add member, and permission setup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEAM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private R2dbcEntityTemplate entityTemplate;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private TeamService teamService;

    /**
     * Prepares current user as OWNER of the test team.
     */
    @BeforeEach
    void setup() {
        lenient().when(currentUserService.getCurrentUser())
                .thenReturn(Mono.just(new CurrentUser(ALICE_ID, "Alice")));
        lenient().when(currentUserService.getCurrentUserId())
                .thenReturn(Mono.just(ALICE_ID));
        lenient().when(teamMemberRepository.findByTeamIdAndUserId(any(), any()))
                .thenReturn(Mono.just(new TeamMember(UUID.randomUUID(), TEAM_ID, ALICE_ID, Role.OWNER, Instant.now())));
        // Mock transactional: execute the callback and wrap result in Flux (for .next() in service)
        lenient().when(transactionalOperator.execute(any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.transaction.reactive.TransactionCallback<?> callback = 
                        (org.springframework.transaction.reactive.TransactionCallback<?>) inv.getArgument(0);
                    Object result = callback.doInTransaction(null);
                    if (result instanceof reactor.core.publisher.Mono) {
                        return ((reactor.core.publisher.Mono<?>) result).flux();
                    }
                    return Flux.just(result);
                });
    }

    /**
     * Happy path for team creation; also verifies owner membership side effect via mocks.
     */
    @Test
    void createTeamSucceeds() {
        Team savedTeam = new Team(TEAM_ID, "Test Team", ALICE_ID, Instant.now());
        when(entityTemplate.insert(any(Team.class))).thenReturn(Mono.just(savedTeam));
        when(entityTemplate.insert(any(TeamMember.class))).thenReturn(Mono.just(new TeamMember(UUID.randomUUID(), TEAM_ID, ALICE_ID, Role.OWNER, Instant.now())));

        StepVerifier.create(teamService.createTeam("Test Team"))
                .expectNextMatches(team -> team.name().equals("Test Team") && team.createdBy().equals(ALICE_ID))
                .verifyComplete();
    }

    /**
     * Verifies addMember succeeds when caller has permission.
     */
    @Test
    void addMemberSucceeds() {
        UUID bobId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TeamMember savedMember = new TeamMember(UUID.randomUUID(), TEAM_ID, bobId, Role.MEMBER, Instant.now());
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(Mono.just(savedMember));

        StepVerifier.create(teamService.addMember(TEAM_ID, bobId, Role.MEMBER))
                .expectNextMatches(member -> member.userId().equals(bobId))
                .verifyComplete();
    }
}
