package com.notetaking.notes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GraphQLIntegrationTest {

    @Test
    void reactiveStackBootsSuccessfully() {
        // This test verifies that the full reactive + GraphQL + R2DBC context loads.
        // Real GraphQL calls are exercised manually via GraphiQL or curl (see README).
        // BlockHound (when enabled in test scope) would catch any blocking calls here.
    }
}
