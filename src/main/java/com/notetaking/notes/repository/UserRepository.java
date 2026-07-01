package com.notetaking.notes.repository;

import com.notetaking.notes.domain.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/**
 * Reactive repository for {@link User}.
 * <p>
 * Primarily available for test setup. Main/demo users are inserted via schema.sql MERGE
 * and application code uses entityTemplate or CurrentUser context.
 */
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    // No custom query methods currently needed (users are pre-seeded via schema.sql MERGE;
    // lookups in main code go through CurrentUser context or direct inserts).
}
