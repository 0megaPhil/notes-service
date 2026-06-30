package com.notetaking.notes.service;

import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.repository.TeamRepository;
import com.notetaking.notes.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       CurrentUserService currentUserService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
    }

    public Flux<Team> getMyTeams() {
        return currentUserService.getCurrentUserId()
            .flatMapMany(userId ->
                teamMemberRepository.findByUserId(userId)
                    .map(TeamMember::teamId)
                    .flatMap(teamRepository::findById)
            );
    }

    public Mono<Team> createTeam(String name) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                Team team = new Team(
                    UUID.randomUUID(),
                    name,
                    currentUser.id(),
                    Instant.now()
                );
                log.info("User {} creating new team '{}'", currentUser.id(), name);
                return teamRepository.save(team)
                    .flatMap(savedTeam -> {
                        TeamMember ownerMembership = new TeamMember(
                            UUID.randomUUID(),
                            savedTeam.id(),
                            currentUser.id(),
                            Role.OWNER,
                            Instant.now()
                        );
                        return teamMemberRepository.save(ownerMembership)
                            .thenReturn(savedTeam);
                    });
            });
    }

    public Mono<TeamMember> addMember(UUID teamId, UUID userIdToAdd, Role role) {
        return currentUserService.getCurrentUserId()
            .flatMap(currentUserId -> canManageTeam(currentUserId, teamId)
                .flatMap(canManage -> {
                    if (!canManage) {
                        log.warn("User {} attempted to add member without permission to team {}", currentUserId, teamId);
                        return Mono.error(new IllegalStateException("Only team owners/admins can add members"));
                    }
                    TeamMember membership = new TeamMember(
                        UUID.randomUUID(),
                        teamId,
                        userIdToAdd,
                        role != null ? role : Role.MEMBER,
                        Instant.now()
                    );
                    log.info("User {} adding member {} to team {}", currentUserId, userIdToAdd, teamId);
                    return teamMemberRepository.save(membership);
                }));
    }

    public Mono<Boolean> removeMember(UUID teamId, UUID userIdToRemove) {
        return currentUserService.getCurrentUserId()
            .flatMap(currentUserId -> canManageTeam(currentUserId, teamId)
                .flatMap(canManage -> {
                    if (!canManage) {
                        return Mono.error(new IllegalStateException("Insufficient permissions"));
                    }
                    return teamMemberRepository.deleteByTeamIdAndUserId(teamId, userIdToRemove)
                        .thenReturn(true);
                }));
    }

    public Flux<TeamMember> getTeamMembers(UUID teamId) {
        return currentUserService.getCurrentUserId()
            .flatMapMany(currentUserId ->
                teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                    .flatMapMany(m -> teamMemberRepository.findByTeamId(teamId))
            );
    }

    private Mono<Boolean> canManageTeam(UUID userId, UUID teamId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.role() == Role.OWNER || member.role() == Role.ADMIN)
            .defaultIfEmpty(false);
    }
}
