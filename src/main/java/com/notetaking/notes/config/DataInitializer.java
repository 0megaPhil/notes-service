package com.notetaking.notes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Conditionally registers a CommandLineRunner that seeds demo data on startup.
 * Disabled by default and in test profile.
 */
@Configuration
@ConditionalOnProperty(name = "app.data.seed-demo", havingValue = "true")
@Profile("!test")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DemoDataSeeder demoDataSeeder;

    public DataInitializer(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    /**
     * Returns the runner. Executes seeding in a non-blocking fire-and-forget fashion.
     */
    @Bean
    CommandLineRunner init() {
        return args -> demoDataSeeder.seed()
                .doOnError(e -> log.error("Demo data seeding failed", e))
                .subscribe();  // Fire-and-forget to keep startup non-blocking
    }
}
