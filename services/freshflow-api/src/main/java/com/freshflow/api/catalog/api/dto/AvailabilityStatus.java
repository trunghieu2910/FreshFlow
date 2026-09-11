package com.freshflow.api.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Availability status for variant: AVAILABLE, MARKED_UNAVAILABLE, CAPACITY_EXHAUSTED, or CAPACITY_NOT_CONFIGURED")
public enum AvailabilityStatus {
  @Schema(description = "Item is active, configured and has available capacity/stock")
  AVAILABLE,

  @Schema(description = "Item was explicitly marked unavailable by merchant")
  MARKED_UNAVAILABLE,

  @Schema(description = "Daily capacity has reached 0 for today (BR-08)")
  CAPACITY_EXHAUSTED,

  @Schema(description = "No capacity records or default capacity configured for made-to-order item")
  CAPACITY_NOT_CONFIGURED
}
