package com.notetaking.notes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration bean.
 * <p>
 * Provides rich metadata for the generated OpenAPI document served at /v3/api-docs.
 * This only covers the REST surface (primarily /auth and actuator).
 * <p>
 * The primary API is GraphQL — documented interactively via GraphiQL at /graphiql.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the top-level OpenAPI object with service info, contact, license, and servers.
     */
    @Bean
    public OpenAPI notesServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Notes Service REST API")
                .description("""
                    REST endpoints for authentication and Spring Boot Actuator.
                    
                    **Primary API is GraphQL** at `/graphql`.
                    - Interactive UI: http://localhost:8080/graphiql
                    - Use demo tokens from POST /auth/login as `Authorization: Bearer <token>`
                    - Or the simpler `X-User-Id: <uuid>` header during development
                    
                    See the project README for full details, seeded user IDs, and GraphQL schema.
                    """)
                .version("0.1.0")
                .contact(new Contact()
                    .name("Notes Service Team")
                    .email("dev@example.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development server")
            ));
    }
}
