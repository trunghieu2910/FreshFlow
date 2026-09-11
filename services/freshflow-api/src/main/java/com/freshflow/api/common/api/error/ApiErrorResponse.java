package com.freshflow.api.common.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Standard FreshFlow API error payload")
public record ApiErrorResponse(
    @Schema(description = "Application specific error code", example = "PRODUCT_NOT_FOUND")
        String code,
    @Schema(
            description = "Human readable error message",
            example = "Product with id 9999 was not found")
        String message,
    @Schema(
            description = "Request URI path where error occurred",
            example = "/api/v1/stores/1/products/9999")
        String path,
    @Schema(description = "Timestamp when error occurred", example = "2026-09-11T03:30:00Z")
        Instant timestamp,
    @Schema(description = "List of field-specific validation errors, if applicable")
        List<FieldError> fieldErrors) {

  public ApiErrorResponse {
    fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
  }

  public ApiErrorResponse(String code, String message, String path, Instant timestamp) {
    this(code, message, path, timestamp, List.of());
  }

  @Schema(description = "Field-level validation error detail")
  public record FieldError(
      @Schema(description = "Name of the invalid request field", example = "price") String field,
      @Schema(
              description = "Reason why the field value is invalid",
              example = "must be greater than or equal to 0.01")
          String message) {}
}
