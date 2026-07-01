package com.notetaking.notes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.core.Ordered;

/**
 * Configuration to ensure that @RestController mappings (like our fallback /graphql)
 * take precedence over other router functions if necessary.
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    mapping.setOrder(Ordered.HIGHEST_PRECEDENCE); // High priority
    return mapping;
  }
}
