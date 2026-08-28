package com.freshflow.api.catalog.api.request;

import com.freshflow.api.catalog.domain.InventoryMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductVariantRequest(
    @Size(max = 80) String name,
    @Size(max = 30) String size,
    @DecimalMin(value = "0.01") BigDecimal price,
    InventoryMode inventoryMode,
    Boolean autoAcceptOverride,
    @Positive Integer maxQuantityPerOrder,
    @PositiveOrZero Integer dailyCapacityDefault,
    Boolean available) {}
