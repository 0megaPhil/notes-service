package com.notetaking.notes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration-level context test for the GraphQL-enabled application.
 * More comprehensive GraphQL tests can be added using WebGraphQlTester.
 */
@SpringBootTest
@ActiveProfiles("test")
class GraphQLIntegrationTest {

    /**
     * Ensures the application boots cleanly for GraphQL usage.
     */
    @Test
    void contextLoads() {
        // The full context loads with test profile (seed-demo = false)
    }
}
