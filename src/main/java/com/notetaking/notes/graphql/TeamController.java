package com.notetaking.notes.graphql;

import com.notetaking.notes.domain.Role;
import com.notetaking.notes.domain.Team;
import com.notetaking.notes.domain.TeamMember;
import com.notetaking.notes.service.TeamService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
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
    public Flux<TeamMember> teamMembers(@Argument UUID teamId) {
        return teamService.getTeamMembers(teamId);
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
    public Mono<TeamMember> addMemberToTeam(@Argument UUID teamId,
                                            @Argument UUID userId,
                                            @Argument Role role) {
        return teamService.addMember(teamId, userId, role);
    }

    /**
     * Removes a member. Requires management permission.
     */
    @MutationMapping
    public Mono<Boolean> removeMemberFromTeam(@Argument UUID teamId, @Argument UUID userId) {
        return teamService.removeMember(teamId, userId);
    }
}
