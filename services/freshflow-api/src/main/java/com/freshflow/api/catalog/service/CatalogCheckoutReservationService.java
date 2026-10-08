package com.freshflow.api.catalog.service;

import com.freshflow.api.catalog.exception.CatalogErrorCode;
import com.freshflow.api.catalog.exception.CatalogRuleViolationException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Validates catalog state and reserves each variant inside the caller's checkout transaction. */
@Service
public class CatalogCheckoutReservationService {
  private final JdbcTemplate jdbcTemplate;

  public CatalogCheckoutReservationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record Selection(Long productId, Long variantId, int quantity) {}

  /** Returns true when at least one item needs merchant confirmation. */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean reserve(Long storeId, List<Selection> selections) {
    boolean manualAcceptance = false;
    for (Selection selection :
        selections.stream().sorted(Comparator.comparing(Selection::variantId)).toList()) {
      Map<String, Object> variant;
      try {
        variant =
            jdbcTemplate.queryForMap(
                "SELECT pv.product_id, p.store_id, pv.inventory_mode, pv.daily_capacity_default, "
                    + "pv.auto_accept_override, s.auto_accept_default, sc.store_id AS category_store_id, "
                    + "pv.is_active AS variant_active, pv.is_available, p.is_active AS product_active, "
                    + "s.status, sc.is_active AS assignment_active, c.is_active AS category_active "
                    + "FROM product_variants pv "
                    + "JOIN products p ON p.id = pv.product_id "
                    + "JOIN stores s ON s.id = p.store_id "
                    + "JOIN store_categories sc ON sc.id = p.store_category_id "
                    + "JOIN categories c ON c.id = sc.category_id "
                    + "WHERE pv.id = ? FOR SHARE OF pv, p, s, sc, c",
                selection.variantId());
      } catch (EmptyResultDataAccessException exception) {
        throw unavailable("Product or variant is unavailable in this store");
      }
      if (!selection.productId().equals(asLong(variant.get("product_id")))
          || !storeId.equals(asLong(variant.get("store_id")))
          || !storeId.equals(asLong(variant.get("category_store_id")))
          || !"ACTIVE".equals(variant.get("status"))
          || !Boolean.TRUE.equals(variant.get("variant_active"))
          || !Boolean.TRUE.equals(variant.get("is_available"))
          || !Boolean.TRUE.equals(variant.get("product_active"))
          || !Boolean.TRUE.equals(variant.get("assignment_active"))
          || !Boolean.TRUE.equals(variant.get("category_active"))) {
        throw unavailable("Product or variant is unavailable in this store");
      }
      Boolean override = (Boolean) variant.get("auto_accept_override");
      boolean autoAccept =
          override != null ? override : Boolean.TRUE.equals(variant.get("auto_accept_default"));
      manualAcceptance |= !autoAccept;

      Long locationId = defaultLocation(storeId);
      String inventoryMode = (String) variant.get("inventory_mode");
      if ("MADE_TO_ORDER".equals(inventoryMode)) {
        reserveCapacity(selection, locationId, (Integer) variant.get("daily_capacity_default"));
      } else if ("LIMITED_STOCK".equals(inventoryMode)) {
        reserveStock(selection, locationId);
      } else {
        throw unavailable("Unsupported inventory mode");
      }
    }
    return manualAcceptance;
  }

  private Long defaultLocation(Long storeId) {
    jdbcTemplate.update(
        "INSERT INTO inventory_locations "
            + "(store_id, name, type, is_default, is_active, created_at, updated_at) "
            + "VALUES (?, 'MAIN_KITCHEN', 'MAIN_KITCHEN', true, true, now(), now()) "
            + "ON CONFLICT DO NOTHING",
        storeId);
    List<Long> locations =
        jdbcTemplate.queryForList(
            "SELECT id FROM inventory_locations WHERE store_id = ? "
                + "AND is_default = true AND is_active = true",
            Long.class,
            storeId);
    if (locations.isEmpty()) {
      throw unavailable("No active inventory location for store");
    }
    return locations.getFirst();
  }

  private void reserveCapacity(Selection selection, Long locationId, Integer defaultLimit) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (defaultLimit != null) {
      jdbcTemplate.update(
          "INSERT INTO inventory_capacity_records "
              + "(variant_id, location_id, capacity_date, capacity_limit, reserved_quantity, "
              + "version, created_at, updated_at) VALUES (?, ?, ?, ?, 0, 0, now(), now()) "
              + "ON CONFLICT (variant_id, location_id, capacity_date) DO NOTHING",
          selection.variantId(),
          locationId,
          today,
          defaultLimit);
    }
    int updated =
        jdbcTemplate.update(
            "UPDATE inventory_capacity_records SET reserved_quantity = reserved_quantity + ?, "
                + "version = version + 1, updated_at = now() "
                + "WHERE variant_id = ? AND location_id = ? AND capacity_date = ? "
                + "AND capacity_limit - reserved_quantity >= ?",
            selection.quantity(),
            selection.variantId(),
            locationId,
            today,
            selection.quantity());
    if (updated != 1) {
      throw unavailable("Daily capacity is unavailable");
    }
  }

  private void reserveStock(Selection selection, Long locationId) {
    int updated =
        jdbcTemplate.update(
            "UPDATE inventory_stock_records SET reserved_quantity = reserved_quantity + ?, "
                + "version = version + 1, updated_at = now() "
                + "WHERE variant_id = ? AND location_id = ? "
                + "AND stock_quantity - reserved_quantity >= ?",
            selection.quantity(),
            selection.variantId(),
            locationId,
            selection.quantity());
    if (updated != 1) {
      throw unavailable("Stock is unavailable");
    }
  }

  private static Long asLong(Object value) {
    return ((Number) value).longValue();
  }

  private static CatalogRuleViolationException unavailable(String message) {
    return new CatalogRuleViolationException(CatalogErrorCode.CONFLICT, message);
  }
}
