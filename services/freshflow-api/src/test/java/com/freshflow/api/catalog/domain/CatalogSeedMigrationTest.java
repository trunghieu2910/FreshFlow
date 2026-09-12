package com.freshflow.api.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CatalogSeedMigrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private Flyway flyway;

  @Test
  void v5_applies_expected_catalog_seed_and_indexes() {
    assertEquals(5, migrationVersion());
    assertEquals(1, count("users", "email = 'demo.owner@freshflow.local'"));
    assertEquals(1, count("stores", "name = 'FreshFlow Demo Kitchen'"));
    assertEquals(2, count("products", "name IN ('Classic Milk Tea', 'Butter Croissant')"));
    assertEquals(
        3,
        count(
            "product_variants",
            "product_id IN (SELECT id FROM products WHERE name IN ('Classic Milk Tea', 'Butter Croissant'))"));

    // Verify expanded realistic catalog dataset (>= 30 products)
    int totalProducts = count("products", "1=1");
    assertTrue(
        totalProducts >= 30, "Expected at least 30 catalog products, but found " + totalProducts);

    int totalVariants = count("product_variants", "1=1");
    assertTrue(
        totalVariants >= 40, "Expected at least 40 product variants, but found " + totalVariants);

    int totalCategories = count("categories", "is_active = true");
    assertTrue(
        totalCategories >= 6,
        "Expected at least 6 active categories, but found " + totalCategories);

    // Verify search functional index is created
    int lowerNameIndexCount =
        count("pg_indexes", "indexname = 'idx_products_store_active_lower_name'");
    assertEquals(1, lowerNameIndexCount, "Index idx_products_store_active_lower_name should exist");
  }

  @Test
  void rerunning_flyway_does_not_duplicate_seed_rows() {
    Map<String, Integer> before = catalogSeedCounts();

    flyway.migrate();

    Map<String, Integer> after = catalogSeedCounts();
    assertEquals(before, after);
  }

  private int migrationVersion() {
    return jdbcTemplate.queryForObject(
        "SELECT COALESCE(MAX(version::integer), 0) FROM flyway_schema_history", Integer.class);
  }

  private Map<String, Integer> catalogSeedCounts() {
    return Map.of(
        "users", count("users", "email = 'demo.owner@freshflow.local'"),
        "stores", count("stores", "name = 'FreshFlow Demo Kitchen'"),
        "categories", count("categories", "is_active = true"),
        "products", count("products", "1=1"),
        "variants", count("product_variants", "1=1"));
  }

  private int count(String table, String predicate) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Integer.class);
  }
}
