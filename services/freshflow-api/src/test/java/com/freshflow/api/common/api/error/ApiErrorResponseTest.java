package com.freshflow.api.common.api.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiErrorResponseTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  @Test
  void should_serialize_all_error_fields() throws Exception {
    Instant timestamp = Instant.parse("2026-08-20T10:15:30Z");
    ApiErrorResponse response =
        new ApiErrorResponse(
            "ORDER_NOT_FOUND", "Order was not found", "/api/orders/123", timestamp);

    String jsonString = objectMapper.writeValueAsString(response);
    JsonNode json = objectMapper.readTree(jsonString);

    assertEquals(4, json.size());
    assertEquals("ORDER_NOT_FOUND", json.get("code").asText());
    assertEquals("Order was not found", json.get("message").asText());
    assertEquals("/api/orders/123", json.get("path").asText());
    assertNotNull(json.get("timestamp"));
    assertEquals(timestamp.toString(), json.get("timestamp").asText());
  }
}
