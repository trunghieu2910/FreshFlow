package com.freshflow.api.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Daily capacity snapshot for made-to-order kitchen items")
public record CapacityDto(
    @Schema(description = "Record date for the capacity snapshot", example = "2026-09-11")
        LocalDate capacityDate,
    @Schema(description = "Remaining available kitchen capacity units for today", example = "45")
        int remaining) {}
