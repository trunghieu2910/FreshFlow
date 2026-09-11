package com.freshflow.api.catalog.api.dto;

import com.freshflow.api.catalog.domain.InventoryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "Product variant details including pricing, size, inventory mode, and real-time daily capacity")
public record ProductVariantDto(
    @Schema(description = "Unique identifier of the variant", example = "10") Long id,
    @Schema(description = "Variant name", example = "Trà Sữa Oolong - Size M") String name,
    @Schema(
            description = "Size of the variant (M, L, or null/empty for STANDARD)",
            example = "M",
            nullable = true)
        String size,
    @Schema(description = "Price of this variant in VND", example = "35000.00") BigDecimal price,
    @Schema(
            description =
                "Inventory mode: MADE_TO_ORDER (kitchen preparation) or LIMITED_STOCK (physical inventory)",
            example = "MADE_TO_ORDER")
        InventoryMode inventoryMode,
    @Schema(
            description = "Whether merchant auto-accepts orders containing this variant",
            example = "true",
            nullable = true)
        Boolean autoAcceptOverride,
    @Schema(
            description = "Maximum quantity a customer can order in a single order",
            example = "10",
            nullable = true)
        Integer maxQuantityPerOrder,
    @Schema(
            description = "Default daily capacity for kitchen preparation",
            example = "100",
            nullable = true)
        Integer dailyCapacityDefault,
    @Schema(
            description =
                "Calculated availability flag based on active status and remaining capacity",
            example = "true")
        boolean available,
    @Schema(description = "Active status flag of the variant", example = "true") boolean active,
    @Schema(
            description =
                "Detailed availability status (AVAILABLE, MARKED_UNAVAILABLE, CAPACITY_EXHAUSTED, CAPACITY_NOT_CONFIGURED)",
            example = "AVAILABLE")
        AvailabilityStatus availabilityStatus,
    @Schema(description = "Daily capacity snapshot information", nullable = true)
        CapacityDto capacity) {}
