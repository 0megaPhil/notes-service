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

@Controller
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @QueryMapping
    public Flux<Team> myTeams() {
        return teamService.getMyTeams();
    }

    @QueryMapping
    public Flux<TeamMember> teamMembers(@Argument UUID teamId) {
        return teamService.getTeamMembers(teamId);
    }

    @MutationMapping
    public Mono<Team> createTeam(@Argument String name) {
        return teamService.createTeam(name);
    }

    @MutationMapping
    public Mono<TeamMember> addMemberToTeam(@Argument UUID teamId,
                                            @Argument UUID userId,
                                            @Argument Role role) {
        return teamService.addMember(teamId, userId, role);
    }

    @MutationMapping
    public Mono<Boolean> removeMemberFromTeam(@Argument UUID teamId, @Argument UUID userId) {
        return teamService.removeMember(teamId, userId);
    }
}
