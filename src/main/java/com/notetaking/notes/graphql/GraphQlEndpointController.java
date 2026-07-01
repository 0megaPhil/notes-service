package com.notetaking.notes.graphql;

import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fallback /graphql endpoint controller.
 * Ensures that the GraphQL HTTP endpoint always has a handler registered
 * via a standard @RestController + @PostMapping, bypassing any issues with
 * the reactive GraphQL router auto-configuration.
 */
@RestController
public class GraphQlEndpointController {

  private final ExecutionGraphQlService executionGraphQlService;

  public GraphQlEndpointController(@Autowired(required = false) ExecutionGraphQlService executionGraphQlService) {
    this.executionGraphQlService = executionGraphQlService;
  }

  @PostMapping("/graphql")
  public Mono<Map<String, Object>> graphql(@RequestBody Map<String, Object> body) {
    if (executionGraphQlService == null) {
      return Mono.just(Map.of(
        "errors", List.of(Map.of("message", "GraphQL execution service not available"))
      ));
    }
    String query = (String) body.get("query");
    @SuppressWarnings("unchecked")
    Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Map.of());
    String operationName = (String) body.get("operationName");

    // Use the execution request
    DefaultExecutionGraphQlRequest request = new DefaultExecutionGraphQlRequest(
      query,
      operationName,
      variables,
      Map.of(),
      "1",
      Locale.getDefault()
    );

    return executionGraphQlService.execute(request)
      .map(ExecutionGraphQlResponse::toMap);
  }
}
