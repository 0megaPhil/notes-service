package com.notetaking.notes;

import com.notetaking.notes.service.NoteService;
import com.notetaking.notes.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.blockhound.BlockHound;

@SpringBootTest
@ActiveProfiles("test")
class GraphQLIntegrationTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private TeamService teamService;

    @Test
    void contextLoads() {
        // The full context loads with test profile (seed-demo = false)
    }

    @Test
    void coreServicesAreAvailable() {
        assert noteService != null;
        assert teamService != null;
    }
}
