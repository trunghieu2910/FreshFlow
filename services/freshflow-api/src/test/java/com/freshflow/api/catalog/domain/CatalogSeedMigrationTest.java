package com.freshflow.api.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void v2_applies_expected_catalog_seed() {
    assertEquals(2, migrationVersion());
    assertEquals(1, count("users", "email = 'demo.owner@freshflow.local'"));
    assertEquals(1, count("stores", "name = 'FreshFlow Demo Kitchen'"));
    assertEquals(2, count("categories", "name IN ('Beverages', 'Bakery')"));
    assertEquals(2, count("products", "name IN ('Classic Milk Tea', 'Butter Croissant')"));
    assertEquals(3, count("product_variants", "name IN ('M', 'L', 'STANDARD')"));
    assertEquals(
        1,
        count(
            "product_variants",
            "name = 'STANDARD' AND size IS NULL AND inventory_mode = 'LIMITED_STOCK'"));
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
        "categories", count("categories", "name IN ('Beverages', 'Bakery')"),
        "products", count("products", "name IN ('Classic Milk Tea', 'Butter Croissant')"),
        "variants", count("product_variants", "name IN ('M', 'L', 'STANDARD')"));
  }

  private int count(String table, String predicate) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Integer.class);
  }
}
