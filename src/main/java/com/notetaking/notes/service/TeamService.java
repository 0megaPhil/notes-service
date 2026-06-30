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

/**
 * Service handling team lifecycle and membership management.
 * Permission checks (owner/admin) are enforced for mutating operations.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;

    /**
     * @param teamRepository       team data access
     * @param teamMemberRepository membership data access
     * @param currentUserService   reactive current user lookup
     */
    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       CurrentUserService currentUserService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns all teams the current user is a member of.
     *
     * @return teams visible to caller
     */
    public Flux<Team> getMyTeams() {
        return currentUserService.getCurrentUserId()
            .flatMapMany(userId ->
                teamMemberRepository.findByUserId(userId)
                    .map(TeamMember::teamId)
                    .flatMap(teamRepository::findById)
            );
    }

    /**
     * Creates a new team and automatically makes the caller the OWNER.
     *
     * @param name team name
     * @return the persisted team
     */
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

    /**
     * Adds (or updates) a member to a team. Caller must be OWNER or ADMIN.
     *
     * @param teamId       target team
     * @param userIdToAdd  user being added
     * @param role         desired role (defaults to MEMBER)
     * @return the saved membership
     */
    public Mono<TeamMember> addMember(UUID teamId, UUID userIdToAdd, Role role) {
        return currentUserService.getCurrentUserId()
            .flatMap(currentUserId -> canManageTeam(currentUserId, teamId)
                .filter(canManage -> canManage)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User {} attempted to add member without permission to team {}", currentUserId, teamId);
                    return Mono.error(new IllegalStateException("Only team owners/admins can add members"));
                }))
                .map(ignored -> {
                    TeamMember membership = new TeamMember(
                        UUID.randomUUID(),
                        teamId,
                        userIdToAdd,
                        role != null ? role : Role.MEMBER,
                        Instant.now()
                    );
                    log.info("User {} adding member {} to team {}", currentUserId, userIdToAdd, teamId);
                    return membership;
                })
                .flatMap(teamMemberRepository::save));
    }

    /**
     * Removes a member from the team. Caller must have management rights.
     *
     * @param teamId        target team
     * @param userIdToRemove member to remove
     * @return true on success
     */
    public Mono<Boolean> removeMember(UUID teamId, UUID userIdToRemove) {
        return currentUserService.getCurrentUserId()
            .flatMap(currentUserId -> canManageTeam(currentUserId, teamId)
                .filter(canManage -> canManage)
                .switchIfEmpty(Mono.error(new IllegalStateException("Insufficient permissions")))
                .flatMap(ignored -> teamMemberRepository.deleteByTeamIdAndUserId(teamId, userIdToRemove)
                    .thenReturn(true)));
    }

    /**
     * Lists members of a team (caller must be a member of the team).
     *
     * @param teamId the team
     * @return members
     */
    public Flux<TeamMember> getTeamMembers(UUID teamId) {
        return currentUserService.getCurrentUserId()
            .flatMapMany(currentUserId ->
                teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                    .flatMapMany(ignored -> teamMemberRepository.findByTeamId(teamId))
            );
    }

    private Mono<Boolean> canManageTeam(UUID userId, UUID teamId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.role() == Role.OWNER || member.role() == Role.ADMIN)
            .defaultIfEmpty(false);
    }
}
