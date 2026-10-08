package com.freshflow.api.catalog.mapper;

import com.freshflow.api.catalog.dto.response.*;
import com.freshflow.api.catalog.enums.*;
import com.freshflow.api.catalog.enums.InventoryMode;
import com.freshflow.api.catalog.model.*;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.model.ProductVariant;
import com.freshflow.api.catalog.model.StoreCategory;
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

  /**
   * Maps a purchasable public-catalog item. Inactive variants are deliberately omitted so a
   * customer can never mistake a soft-deleted variant for a purchasable one.
   */
  public ProductCatalogDto toPublicProductDto(
      Product product, Map<Long, CapacitySnapshot> capacityByVariantId) {
    Objects.requireNonNull(product, "product must not be null");
    Map<Long, CapacitySnapshot> capacities =
        capacityByVariantId == null ? Map.of() : capacityByVariantId;
    List<ProductVariantDto> variants =
        product.getVariants() == null
            ? List.of()
            : product.getVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
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
        true,
        toStoreCategoryDto(product.getStoreCategory()),
        variants);
  }

  private static StoreCategoryDto toStoreCategoryDto(StoreCategory storeCategory) {
    if (storeCategory == null || storeCategory.getCategory() == null) {
      return null;
    }
    return new StoreCategoryDto(
        storeCategory.getId(),
        storeCategory.getCategory().getId(),
        storeCategory.getCategory().getName(),
        storeCategory.getDisplayOrder());
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
        variant.getDailyCapacityDefault(),
        available,
        active,
        status,
        capacity);
  }
}
