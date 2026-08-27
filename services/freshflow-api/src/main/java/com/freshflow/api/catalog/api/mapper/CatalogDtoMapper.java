package com.freshflow.api.catalog.api.mapper;

import com.freshflow.api.catalog.api.dto.AvailabilityStatus;
import com.freshflow.api.catalog.api.dto.CapacityDto;
import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.application.readmodel.CapacitySnapshot;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CatalogDtoMapper {
  public ProductCatalogDto toProductDto(Product product) {
    return toProductDto(product, Map.of());
  }

  public ProductCatalogDto toProductDto(
      Product product, Map<Long, CapacitySnapshot> capacityByVariantId) {
    Objects.requireNonNull(product, "product must not be null");
    Map<Long, CapacitySnapshot> capacities =
        capacityByVariantId == null ? Map.of() : capacityByVariantId;

    List<ProductVariantDto> variants =
        product.getVariants() == null
            ? List.of()
            : product.getVariants().stream()
                .map(
                    variant ->
                        toProductVariantDto(
                            variant,
                            variant.getId() == null ? null : capacities.get(variant.getId())))
                .toList();

    return new ProductCatalogDto(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getImageUrl(),
        product.getIsActive(),
        variants);
  }

  public ProductVariantDto toProductVariantDto(
      ProductVariant variant, CapacitySnapshot capacitySnapshot) {
    Objects.requireNonNull(variant, "variant must not be null");

    boolean active = Boolean.TRUE.equals(variant.getIsActive());
    boolean entityAvailable = Boolean.TRUE.equals(variant.getIsAvailable());
    boolean baseAvailable = active && entityAvailable;
    AvailabilityStatus status;
    boolean available;

    if (!baseAvailable) {
      status = AvailabilityStatus.MARKED_UNAVAILABLE;
      available = false;
    } else if (InventoryMode.MADE_TO_ORDER.equals(variant.getInventoryMode())) {
      if (capacitySnapshot == null) {
        status = AvailabilityStatus.CAPACITY_NOT_CONFIGURED;
        available = false;
      } else if (capacitySnapshot.remaining() == 0) {
        status = AvailabilityStatus.CAPACITY_EXHAUSTED;
        available = false;
      } else {
        status = AvailabilityStatus.AVAILABLE;
        available = true;
      }
    } else {
      status = AvailabilityStatus.AVAILABLE;
      available = true;
    }

    CapacityDto capacity =
        InventoryMode.MADE_TO_ORDER.equals(variant.getInventoryMode()) && capacitySnapshot != null
            ? new CapacityDto(capacitySnapshot.capacityDate(), capacitySnapshot.remaining())
            : null;

    return new ProductVariantDto(
        variant.getId(),
        variant.getName(),
        variant.getSize(),
        variant.getPrice(),
        variant.getInventoryMode(),
        variant.getAutoAcceptOverride(),
        variant.getMaxQuantityPerOrder(),
        available,
        active,
        status,
        capacity);
  }
}
