package com.notetaking.notes.repository;

import com.notetaking.notes.domain.Team;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TeamRepository extends ReactiveCrudRepository<Team, UUID> {
    Flux<Team> findByCreatedBy(UUID createdBy);
}
