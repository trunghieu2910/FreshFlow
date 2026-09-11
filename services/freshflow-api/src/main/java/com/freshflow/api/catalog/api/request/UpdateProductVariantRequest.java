package com.freshflow.api.catalog.api.request;

import com.freshflow.api.catalog.domain.InventoryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(
    description = "Request payload for updating an existing product variant (all fields optional)")
public record UpdateProductVariantRequest(
    @Size(max = 80)
        @Schema(description = "Updated variant name", example = "Trà Đào Cam Sả - Size L Plus")
        String name,
    @Size(max = 30)
        @Schema(
            description = "Updated size: M, L, or empty for STANDARD",
            example = "L",
            nullable = true)
        String size,
    @DecimalMin(value = "0.01") @Schema(description = "Updated price in VND", example = "48000.00")
        BigDecimal price,
    @Schema(
            description = "Updated inventory mode: MADE_TO_ORDER or LIMITED_STOCK",
            example = "MADE_TO_ORDER")
        InventoryMode inventoryMode,
    @Schema(
            description = "Whether merchant auto-accepts orders with this variant",
            example = "true")
        Boolean autoAcceptOverride,
    @Positive @Schema(description = "Updated maximum quantity per order", example = "10")
        Integer maxQuantityPerOrder,
    @PositiveOrZero
        @Schema(
            description = "Updated default daily capacity for kitchen preparation",
            example = "150",
            nullable = true)
        Integer dailyCapacityDefault,
    @Schema(description = "Updated availability flag", example = "true") Boolean available) {}
