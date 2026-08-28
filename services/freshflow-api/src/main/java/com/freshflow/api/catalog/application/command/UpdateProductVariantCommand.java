package com.freshflow.api.catalog.application.command;

import com.freshflow.api.catalog.domain.InventoryMode;
import java.math.BigDecimal;

public record UpdateProductVariantCommand(
    String name,
    String size,
    BigDecimal price,
    InventoryMode inventoryMode,
    Boolean autoAcceptOverride,
    Integer maxQuantityPerOrder,
    Integer dailyCapacityDefault,
    Boolean available) {}
