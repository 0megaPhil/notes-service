package com.notetaking.notes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the full Spring Boot application context loads successfully
 * under the test profile (with demo seeding disabled).
 */
@SpringBootTest
@ActiveProfiles("test")
class NotesServiceApplicationTests {

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    void contextLoadsWithCoreBeans() {
        // Real verification instead of empty method
        assertThat(context.containsBean("noteService")).isTrue();
        assertThat(context.containsBean("teamService")).isTrue();
        assertThat(context.containsBean("noteController")).isTrue();
    }
}
