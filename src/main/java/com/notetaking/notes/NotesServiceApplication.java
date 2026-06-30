package com.notetaking.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point for the Notes Service.
 * <p>
 * Provides a reactive (WebFlux) GraphQL API for collaborative note taking
 * with team support, backed by R2DBC.
 */
@SpringBootApplication
public class NotesServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(NotesServiceApplication.class, args);
    }
}
