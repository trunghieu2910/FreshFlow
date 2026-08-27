package com.freshflow.api.catalog.api.dto;

import com.freshflow.api.catalog.domain.InventoryMode;
import java.math.BigDecimal;

public record ProductVariantDto(
    Long id,
    String name,
    String size,
    BigDecimal price,
    InventoryMode inventoryMode,
    Boolean autoAcceptOverride,
    Integer maxQuantityPerOrder,
    boolean available,
    boolean active,
    AvailabilityStatus availabilityStatus,
    CapacityDto capacity) {}
