package com.freshflow.api.catalog.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshflow.api.catalog.api.dto.AvailabilityStatus;
import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.application.readmodel.CapacitySnapshot;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogDtoMapperTest {
  private final CatalogDtoMapper mapper = new CatalogDtoMapper();

  @Test
  void mapsMadeToOrderSizedVariantWithRemainingCapacity() {
    ProductVariant variant = variant(11L, "M", "M", InventoryMode.MADE_TO_ORDER, true, true);

    ProductVariantDto result =
        mapper.toProductVariantDto(variant, new CapacitySnapshot(LocalDate.of(2026, 8, 27), 10, 3));

    assertEquals("M", result.size());
    assertEquals(new BigDecimal("35000.00"), result.price());
    assertEquals(InventoryMode.MADE_TO_ORDER, result.inventoryMode());
    assertTrue(result.available());
    assertEquals(AvailabilityStatus.AVAILABLE, result.availabilityStatus());
    assertEquals(LocalDate.of(2026, 8, 27), result.capacity().capacityDate());
    assertEquals(7, result.capacity().remaining());
    assertNotSame(variant, result);
  }

  @Test
  void mapsLimitedStockStandardVariantWithNullSize() {
    ProductVariant variant =
        variant(12L, "STANDARD", null, InventoryMode.LIMITED_STOCK, true, true);

    ProductVariantDto result = mapper.toProductVariantDto(variant, null);

    assertEquals("STANDARD", result.name());
    assertNull(result.size());
    assertEquals(InventoryMode.LIMITED_STOCK, result.inventoryMode());
    assertTrue(result.available());
    assertEquals(AvailabilityStatus.AVAILABLE, result.availabilityStatus());
    assertNull(result.capacity());
  }

  @Test
  void marksMadeToOrderVariantUnavailableWhenCapacityIsExhausted() {
    ProductVariant variant = variant(13L, "L", "L", InventoryMode.MADE_TO_ORDER, true, true);

    ProductVariantDto result =
        mapper.toProductVariantDto(variant, new CapacitySnapshot(LocalDate.of(2026, 8, 27), 4, 4));

    assertFalse(result.available());
    assertEquals(AvailabilityStatus.CAPACITY_EXHAUSTED, result.availabilityStatus());
    assertEquals(0, result.capacity().remaining());
  }

  @Test
  void marksMadeToOrderVariantUnavailableWhenCapacityIsNotConfigured() {
    ProductVariant variant = variant(14L, "M", "M", InventoryMode.MADE_TO_ORDER, true, true);

    ProductVariantDto result = mapper.toProductVariantDto(variant, null);

    assertFalse(result.available());
    assertEquals(AvailabilityStatus.CAPACITY_NOT_CONFIGURED, result.availabilityStatus());
    assertNull(result.capacity());
  }

  @Test
  void preservesManualUnavailableState() {
    ProductVariant variant = variant(15L, "L", "L", InventoryMode.MADE_TO_ORDER, false, true);

    ProductVariantDto result =
        mapper.toProductVariantDto(variant, new CapacitySnapshot(LocalDate.of(2026, 8, 27), 10, 1));

    assertFalse(result.available());
    assertTrue(result.active());
    assertEquals(AvailabilityStatus.MARKED_UNAVAILABLE, result.availabilityStatus());
    assertEquals(9, result.capacity().remaining());
  }

  @Test
  void preservesInactiveStateAsUnavailable() {
    ProductVariant variant = variant(16L, "M", "M", InventoryMode.LIMITED_STOCK, true, false);

    ProductVariantDto result = mapper.toProductVariantDto(variant, null);

    assertFalse(result.available());
    assertFalse(result.active());
    assertEquals(AvailabilityStatus.MARKED_UNAVAILABLE, result.availabilityStatus());
  }

  @Test
  void mapsProductWithNestedSizedAndStandardVariants() {
    Product product = new Product();
    product.setId(21L);
    product.setName("Classic Milk Tea");
    ProductVariant m = variant(21L, "M", "M", InventoryMode.MADE_TO_ORDER, true, true);
    ProductVariant l = variant(22L, "L", "L", InventoryMode.MADE_TO_ORDER, true, true);
    ProductVariant standard =
        variant(23L, "STANDARD", null, InventoryMode.LIMITED_STOCK, true, true);
    product.setVariants(List.of(m, l, standard));

    ProductCatalogDto result =
        mapper.toProductDto(
            product,
            Map.of(
                21L, new CapacitySnapshot(LocalDate.of(2026, 8, 27), 10, 2),
                22L, new CapacitySnapshot(LocalDate.of(2026, 8, 27), 5, 5)));

    assertEquals("Classic Milk Tea", result.name());
    assertEquals(3, result.variants().size());
    assertTrue(result.variants().get(0).available());
    assertEquals(
        AvailabilityStatus.CAPACITY_EXHAUSTED, result.variants().get(1).availabilityStatus());
    assertNull(result.variants().get(2).size());
    assertEquals(ProductVariantDto.class, result.variants().get(0).getClass());
  }

  @Test
  void rejectsInvalidCapacitySnapshot() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CapacitySnapshot(LocalDate.of(2026, 8, 27), 2, 3));
  }

  private ProductVariant variant(
      Long id,
      String name,
      String size,
      InventoryMode inventoryMode,
      boolean available,
      boolean active) {
    ProductVariant variant = new ProductVariant();
    variant.setId(id);
    variant.setName(name);
    variant.setSize(size);
    variant.setPrice(new BigDecimal("35000.00"));
    variant.setInventoryMode(inventoryMode);
    variant.setIsAvailable(available);
    variant.setIsActive(active);
    return variant;
  }
}
