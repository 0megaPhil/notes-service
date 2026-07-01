package com.notetaking.notes.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Provides explicit GraphQlSource + ExecutionGraphQlService beans.
 * <p>
 * This ensures the GraphQL execution layer is always available regardless of
 * web exposure auto-config conditions or schema detection edge cases.
 * The @Controller classes (NoteController / TeamController) are picked up via
 * the RuntimeWiringConfigurer beans (AnnotatedControllerConfigurer) supplied
 * by spring-graphql.
 */
@Configuration
public class GraphQlSourceConfig {

    @Bean
    public GraphQlSource graphQlSource(List<RuntimeWiringConfigurer> configurers) {
        // Use schemaResourceBuilder() per Spring GraphQL 1.3.x API (Spring Boot 4)
        GraphQlSource.SchemaResourceBuilder builder = GraphQlSource.schemaResourceBuilder()
                .schemaResources(new ClassPathResource("graphql/schema.graphqls"));
        for (RuntimeWiringConfigurer c : configurers) {
            builder = builder.configureRuntimeWiring(c);
        }
        return builder.build();
    }

    @Bean
    public ExecutionGraphQlService executionGraphQlService(GraphQlSource graphQlSource) {
        return new DefaultExecutionGraphQlService(graphQlSource);
    }

    /**
     * Custom exception resolver so that permission errors (IllegalState/Argument from
     * our canModify checks) surface their message to the client instead of becoming
     * opaque "INTERNAL_ERROR".
     */
    @Bean
    public DataFetcherExceptionResolver permissionExceptionResolver() {
        return (ex, env) -> {
            if (ex instanceof IllegalStateException || ex instanceof IllegalArgumentException) {
                GraphQLError err = GraphqlErrorBuilder.newError()
                    .message(ex.getMessage())
                    .build();
                return Mono.just(List.of(err));
            }
            return Mono.empty();
        };
    }
}
