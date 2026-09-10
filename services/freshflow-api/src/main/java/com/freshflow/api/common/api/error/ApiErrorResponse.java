package com.freshflow.api.common.api.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String code, String message, String path, Instant timestamp, List<FieldError> fieldErrors) {

  public ApiErrorResponse {
    fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
  }

  public ApiErrorResponse(String code, String message, String path, Instant timestamp) {
    this(code, message, path, timestamp, List.of());
  }

  public record FieldError(String field, String message) {}
}
