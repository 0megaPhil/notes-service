package com.notetaking.notes.repository;

import com.notetaking.notes.domain.TeamMember;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TeamMemberRepository extends ReactiveCrudRepository<TeamMember, UUID> {
    Flux<TeamMember> findByTeamId(UUID teamId);
    Flux<TeamMember> findByUserId(UUID userId);
    Mono<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);
    Mono<Void> deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
