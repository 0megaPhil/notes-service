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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TeamService teamService;

    private final UUID aliceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID teamId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setup() {
        lenient().when(currentUserService.getCurrentUser())
                .thenReturn(Mono.just(new CurrentUser(aliceId, "Alice")));
        lenient().when(currentUserService.getCurrentUserId())
                .thenReturn(Mono.just(aliceId));
        lenient().when(teamMemberRepository.findByTeamIdAndUserId(any(), any()))
                .thenReturn(Mono.just(new TeamMember(UUID.randomUUID(), teamId, aliceId, Role.OWNER, Instant.now())));
    }

    @Test
    void createTeamSucceeds() {
        Team savedTeam = new Team(teamId, "Test Team", aliceId, Instant.now());
        when(teamRepository.save(any(Team.class))).thenReturn(Mono.just(savedTeam));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(Mono.just(new TeamMember(UUID.randomUUID(), teamId, aliceId, Role.OWNER, Instant.now())));

        StepVerifier.create(teamService.createTeam("Test Team"))
                .expectNextMatches(team -> team.name().equals("Test Team") && team.createdBy().equals(aliceId))
                .verifyComplete();
    }

    @Test
    void addMemberSucceeds() {
        UUID bobId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TeamMember savedMember = new TeamMember(UUID.randomUUID(), teamId, bobId, Role.MEMBER, Instant.now());
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(Mono.just(savedMember));

        StepVerifier.create(teamService.addMember(teamId, bobId, Role.MEMBER))
                .expectNextMatches(member -> member.userId().equals(bobId))
                .verifyComplete();
    }
}
