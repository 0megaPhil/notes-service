package com.notetaking.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class NotesServiceApplication {

    static {
        // Install BlockHound to detect ANY blocking calls on non-blocking threads.
        // This guarantees that the WebFlux + Project Reactor stack stays non-blocking.
        // If a blocking call is ever introduced, the app will fail fast with a clear error.
        BlockHound.install();
    }

    public static void main(String[] args) {
        SpringApplication.run(NotesServiceApplication.class, args);
    }
}
