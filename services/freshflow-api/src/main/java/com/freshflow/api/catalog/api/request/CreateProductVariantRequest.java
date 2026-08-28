package com.freshflow.api.catalog.api.request;

import com.freshflow.api.catalog.domain.InventoryMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductVariantRequest(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 30) String size,
    @NotNull @DecimalMin(value = "0.01") BigDecimal price,
    @NotNull InventoryMode inventoryMode,
    Boolean autoAcceptOverride,
    @Positive Integer maxQuantityPerOrder,
    @PositiveOrZero Integer dailyCapacityDefault,
    Boolean available) {}
