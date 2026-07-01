package com.notetaking.notes.service;

import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.repository.TeamMemberRepository;
import com.notetaking.notes.repository.TeamRepository;
import com.notetaking.notes.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final R2dbcEntityTemplate entityTemplate;
    private final TransactionalOperator transactionalOperator;

    /**
     * @param teamRepository       team data access
     * @param teamMemberRepository membership data access
     * @param currentUserService   reactive current user lookup
     * @param entityTemplate       explicit inserts (pre-assigned UUIDs would otherwise trigger UPDATE)
     */
    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       CurrentUserService currentUserService,
                       R2dbcEntityTemplate entityTemplate,
                       TransactionalOperator transactionalOperator) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
        this.entityTemplate = entityTemplate;
        this.transactionalOperator = transactionalOperator;
    }

    /**
     * Returns all teams the current user is a member of.
     *
     * @return teams visible to caller
     */
    public Flux<Team> getMyTeams() {
        return currentUserService.getCurrentUserId()
            .flatMapMany(this::getMyTeams);
    }

    /**
     * Returns all teams the given user is a member of (or created).
     * Used by GraphQL resolvers that extract the user from context explicitly.
     */
    public Flux<Team> getMyTeams(UUID userId) {
        log.debug("getMyTeams called for userId: {}", userId);
        return Flux.concat(
            // teams via explicit membership
            teamRepository.findByMemberUserId(userId)
                .doOnNext(t -> log.debug("Found team {} via membership for user {}", t.id(), userId)),
            // also teams created by user (creator is implicitly a member/owner)
            teamRepository.findByCreatedBy(userId)
                .doOnNext(t -> log.debug("Found created team {} for user {}", t.id(), userId))
        ).distinct(Team::id)
            .doOnNext(t -> log.debug("myTeams for {} includes team {}", userId, t.id()));
    }

    /**
     * Creates a new team and automatically makes the caller the OWNER.
     *
     * @param name team name
     * @return the persisted team
     */
    public Mono<Team> createTeam(String name) {
        return transactionalOperator.execute(txStatus ->
            currentUserService.getCurrentUser()
                .flatMap(currentUser -> {
                    Team team = new Team(
                        UUID.randomUUID(),
                        name,
                        currentUser.id(),
                        Instant.now()
                    );
                    log.info("User {} creating new team '{}'", currentUser.id(), name);
                    return entityTemplate.insert(team)
                        .flatMap(savedTeam -> {
                            TeamMember ownerMembership = new TeamMember(
                                UUID.randomUUID(),
                                savedTeam.id(),
                                currentUser.id(),
                                Role.OWNER,
                                Instant.now()
                            );
                            return entityTemplate.insert(ownerMembership)
                                .thenReturn(savedTeam);
                        });
                })
        ).next()
        .delayElement(Duration.ofMillis(150));  // small delay to mitigate cross-connection visibility in H2 for immediate follow-up queries in tests/repros
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
        final Role targetRole = Optional.ofNullable(role).orElse(Role.MEMBER);
        return currentUserService.getCurrentUserId()
            .flatMap(currentUserId -> canManageTeam(currentUserId, teamId)
                .filter(canManage -> canManage)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User {} attempted to add member without permission to team {}", currentUserId, teamId);
                    return Mono.error(new IllegalStateException("Only team owners/admins can add members"));
                }))
                .then(
                    transactionalOperator.transactional(
                        teamMemberRepository.findByTeamIdAndUserId(teamId, userIdToAdd)
                            .flatMap(existing -> {
                                if (existing.role() == targetRole) {
                                    log.info("User {} already has role {} in team {}", userIdToAdd, targetRole, teamId);
                                    return Mono.just(existing);
                                }
                                TeamMember updated = new TeamMember(
                                    existing.id(),
                                    existing.teamId(),
                                    existing.userId(),
                                    targetRole,
                                    existing.joinedAt()
                                );
                                log.info("User {} promoting {} to {} in team {}", currentUserId, userIdToAdd, targetRole, teamId);
                                return teamMemberRepository.save(updated);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                TeamMember membership = new TeamMember(
                                    UUID.randomUUID(),
                                    teamId,
                                    userIdToAdd,
                                    targetRole,
                                    Instant.now()
                                );
                                log.info("User {} adding {} as {} to team {}", currentUserId, userIdToAdd, targetRole, teamId);
                                return entityTemplate.insert(membership);
                            }))
                    )
                )
                .delayElement(Duration.ofMillis(150))
            );
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
     * Lists members of a team for the given user (the caller must be a member or the creator).
     * <p>
     * This overload accepts an explicit userId (sourced from GraphQL context or headers in the controller)
     * for reliable reactive context propagation.
     *
     * @param teamId        the team
     * @param currentUserId the user performing the lookup (used for permission check + virtual OWNER fallback)
     * @return members (may include a virtual OWNER entry for the creator if not explicitly in membership table)
     */
    public Flux<TeamMember> getTeamMembers(UUID teamId, UUID currentUserId) {
        log.debug("getTeamMembers for team {} as user {}", teamId, currentUserId);
        // Load members first. Then load team to decide on creator.
        // This is resilient to one or the other not being visible yet after create.
        Mono<List<TeamMember>> membersMono = teamMemberRepository.findByTeamId(teamId).collectList();
        return membersMono.flatMapMany(existing -> {
            boolean hasSelf = existing.stream().anyMatch(m -> m.userId().equals(currentUserId));
            return teamRepository.findById(teamId)
                .map(t -> currentUserId.equals(t.createdBy()))
                .defaultIfEmpty(false)
                .flatMapMany(isCreator -> {
                    if (!isCreator && !hasSelf) {
                        return Flux.empty();
                    }
                    List<TeamMember> result = existing;
                    if (isCreator && !hasSelf) {
                        result = new java.util.ArrayList<>(existing);
                        result.add(new TeamMember(UUID.randomUUID(), teamId, currentUserId, Role.OWNER, Instant.now()));
                    }
                    return Flux.fromIterable(result);
                });
        });
    }

    private Mono<Boolean> canManageTeam(UUID userId, UUID teamId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.role() == Role.OWNER || member.role() == Role.ADMIN)
            .defaultIfEmpty(false);
    }
}
