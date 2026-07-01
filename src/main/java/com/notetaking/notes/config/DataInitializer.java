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
     * Returns the runner.
     * <p>
     * For the demo/dev seed we deliberately block here so that "application started"
     * means demo data is present. This makes automated checks, curl tests, and
     * the first browser requests reliable without extra sleeps/races.
     * (Production deploys keep seed-demo=false and do not use this path.)
     */
    @Bean
    CommandLineRunner init() {
        return args -> demoDataSeeder.seed()
                .doOnError(e -> log.error("Demo data seeding failed", e))
                .block();   // Await completion so startup guarantees seeded data is visible
    }
}
