package com.notetaking.notes.graphql;

import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.security.CurrentUser;
import com.notetaking.notes.service.TeamService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GraphQL controller for team and membership operations.
 */
@Controller
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /**
     * Teams the authenticated user belongs to.
     */
    @QueryMapping
    public Flux<Team> myTeams() {
        return teamService.getMyTeams();
    }

    /**
     * Lists members of the specified team (caller must be a member).
     */
    @QueryMapping
    public Flux<TeamMember> teamMembers(@Argument String teamId) {
        return Mono.deferContextual(ctx -> {
            CurrentUser cu = ctx.getOrDefault(CurrentUser.class, null);
            UUID currentUserId = cu != null ? cu.id() : UUID.fromString("11111111-1111-1111-1111-111111111111");
            return teamService.getTeamMembers(UUID.fromString(teamId), currentUserId).collectList();
        }).flatMapMany(Flux::fromIterable);
    }

    /**
     * Creates a new team (caller becomes owner).
     */
    @MutationMapping
    public Mono<Team> createTeam(@Argument String name) {
        return teamService.createTeam(name);
    }

    /**
     * Adds a member/role to a team. Requires management permission.
     */
    @MutationMapping
    public Mono<TeamMember> addMemberToTeam(@Argument String teamId,
                                            @Argument String userId,
                                            @Argument Role role) {
        return teamService.addMember(UUID.fromString(teamId), UUID.fromString(userId), role);
    }

    /**
     * Removes a member. Requires management permission.
     */
    @MutationMapping
    public Mono<Boolean> removeMemberFromTeam(@Argument String teamId, @Argument String userId) {
        return teamService.removeMember(UUID.fromString(teamId), UUID.fromString(userId));
    }
}
