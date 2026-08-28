package com.freshflow.api.catalog.api.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogDtoMapperConfig {
  @Bean
  public CatalogDtoMapper catalogDtoMapper() {
    return new CatalogDtoMapper();
  }

  @Bean
  public CatalogRequestMapper catalogRequestMapper() {
    return new CatalogRequestMapper();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Register JavaTimeModule to handle Java 8 date/time types if it's on classpath
    try {
      mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    } catch (Throwable t) {
      // Ignore if module not found
    }
    return mapper;
  }
}
