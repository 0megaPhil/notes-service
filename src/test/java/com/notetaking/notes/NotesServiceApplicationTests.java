package com.notetaking.notes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotesServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context starts without errors
        // BlockHound is active and will fail the test if blocking calls are introduced
    }
}
