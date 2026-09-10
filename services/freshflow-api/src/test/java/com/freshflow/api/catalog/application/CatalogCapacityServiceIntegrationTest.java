package com.freshflow.api.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.freshflow.api.catalog.application.readmodel.CapacitySnapshot;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.ProductVariant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CatalogCapacityServiceIntegrationTest {

  @Autowired private CatalogCapacityService capacityService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void returnsDefaultCapacityWhenNoDbRecordExists() {
    ProductVariant variant = new ProductVariant();
    variant.setId(9901L);
    variant.setInventoryMode(InventoryMode.MADE_TO_ORDER);
    variant.setDailyCapacityDefault(25);

    Map<Long, CapacitySnapshot> result =
        capacityService.getCapacitySnapshots(List.of(variant), LocalDate.of(2026, 9, 10));

    assertNotNull(result.get(9901L));
    assertEquals(25, result.get(9901L).capacityLimit());
    assertEquals(0, result.get(9901L).reservedQuantity());
    assertEquals(25, result.get(9901L).remaining());
  }

  @Test
  void returnsDbRecordCapacityWhenRecordExists() {
    LocalDate date = LocalDate.of(2026, 9, 10);

    // Get an existing store and location or insert demo location
    Long storeId = jdbcTemplate.queryForObject("SELECT id FROM stores LIMIT 1", Long.class);
    List<Long> locations =
        jdbcTemplate.queryForList(
            "SELECT id FROM inventory_locations WHERE store_id = ? LIMIT 1", Long.class, storeId);
    Long locationId;
    if (locations.isEmpty()) {
      jdbcTemplate.update(
          "INSERT INTO inventory_locations (store_id, name, type, is_default, is_active, created_at, updated_at) "
              + "VALUES (?, 'Main Kitchen', 'MAIN_KITCHEN', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
          storeId);
      locationId =
          jdbcTemplate.queryForObject(
              "SELECT id FROM inventory_locations WHERE store_id = ? LIMIT 1", Long.class, storeId);
    } else {
      locationId = locations.get(0);
    }

    Long variantId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM product_variants WHERE inventory_mode = 'MADE_TO_ORDER' LIMIT 1",
            Long.class);

    // Insert capacity record for today with reserved = limit (exhausted)
    jdbcTemplate.update(
        "INSERT INTO inventory_capacity_records (variant_id, location_id, capacity_date, capacity_limit, reserved_quantity, created_at, updated_at) "
            + "VALUES (?, ?, ?, 10, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (variant_id, location_id, capacity_date) DO UPDATE "
            + "SET capacity_limit = 10, reserved_quantity = 10",
        variantId,
        locationId,
        date);

    ProductVariant variant = new ProductVariant();
    variant.setId(variantId);
    variant.setInventoryMode(InventoryMode.MADE_TO_ORDER);
    variant.setDailyCapacityDefault(20);

    Map<Long, CapacitySnapshot> result =
        capacityService.getCapacitySnapshots(List.of(variant), date);

    assertNotNull(result.get(variantId));
    assertEquals(10, result.get(variantId).capacityLimit());
    assertEquals(10, result.get(variantId).reservedQuantity());
    assertEquals(0, result.get(variantId).remaining()); // Capacity exhausted
  }

  @Test
  void ignoresLimitedStockVariants() {
    ProductVariant limitedStock = new ProductVariant();
    limitedStock.setId(9902L);
    limitedStock.setInventoryMode(InventoryMode.LIMITED_STOCK);
    limitedStock.setDailyCapacityDefault(15);

    Map<Long, CapacitySnapshot> result =
        capacityService.getCapacitySnapshots(List.of(limitedStock), LocalDate.of(2026, 9, 10));

    assertNull(result.get(9902L));
  }
}
