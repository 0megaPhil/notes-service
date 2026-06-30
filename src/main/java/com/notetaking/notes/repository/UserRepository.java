package com.notetaking.notes.repository;

import com.notetaking.notes.domain.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for {@link User} (primarily used by demo seeder).
 */
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    /**
     * Lookup user by email (demo / future use).
     */
    Mono<User> findByEmail(String email);
}
