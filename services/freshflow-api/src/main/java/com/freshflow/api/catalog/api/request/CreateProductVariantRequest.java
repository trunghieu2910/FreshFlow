package com.freshflow.api.catalog.api.request;

import com.freshflow.api.catalog.domain.InventoryMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new product variant")
public record CreateProductVariantRequest(
    @NotBlank
        @Size(max = 80)
        @Schema(description = "Variant name", example = "Trà Đào Cam Sả - Size L")
        String name,
    @Size(max = 30)
        @Schema(
            description = "Size of variant: M, L, or omitted/null for STANDARD",
            example = "L",
            nullable = true)
        String size,
    @NotNull @DecimalMin(value = "0.01")
        @Schema(description = "Price of this variant in VND", example = "45000.00")
        BigDecimal price,
    @NotNull @Schema(
            description = "Inventory management mode: MADE_TO_ORDER or LIMITED_STOCK",
            example = "MADE_TO_ORDER")
        InventoryMode inventoryMode,
    @Schema(
            description = "Whether merchant auto-accepts orders with this variant",
            example = "true",
            defaultValue = "false")
        Boolean autoAcceptOverride,
    @Positive @Schema(
            description = "Maximum quantity allowed per single order",
            example = "5",
            defaultValue = "10")
        Integer maxQuantityPerOrder,
    @PositiveOrZero
        @Schema(
            description = "Default daily capacity for made-to-order kitchen items",
            example = "120",
            nullable = true)
        Integer dailyCapacityDefault,
    @Schema(
            description = "Initial availability flag (defaults to true)",
            example = "true",
            defaultValue = "true")
        Boolean available) {}
