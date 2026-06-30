package com.notetaking.notes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test ensuring the full Spring Boot application context loads successfully
 * under the test profile (with demo seeding disabled).
 */
@SpringBootTest
@ActiveProfiles("test")
class NotesServiceApplicationTests {

    /**
     * Context load verification. Any misconfiguration or blocking call
     * (enforced by BlockHound in tests) will cause failure.
     */
    @Test
    void contextLoads() {
        // Verifies that the Spring context starts without errors
        // BlockHound is active and will fail the test if blocking calls are introduced
    }
}
