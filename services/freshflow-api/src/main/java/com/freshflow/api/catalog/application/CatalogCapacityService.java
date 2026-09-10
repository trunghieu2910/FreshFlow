package com.freshflow.api.catalog.application;

import com.freshflow.api.catalog.application.readmodel.CapacitySnapshot;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.ProductVariant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads daily capacity records and generates capacity snapshots for MADE_TO_ORDER variants.
 *
 * <p>Enforces business rules:
 *
 * <ul>
 *   <li>BR-06: MADE_TO_ORDER uses daily capacity limits and reservations.
 *   <li>BR-08: If remaining capacity is zero, variant becomes CAPACITY_EXHAUSTED.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CatalogCapacityService {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public CatalogCapacityService(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Builds capacity snapshots for a collection of product variants on a specific date.
   *
   * @param variants the variants to evaluate
   * @param date the date of capacity evaluation (defaults to today if null)
   * @return a map of variantId -> CapacitySnapshot
   */
  public Map<Long, CapacitySnapshot> getCapacitySnapshots(
      List<ProductVariant> variants, LocalDate date) {
    if (variants == null || variants.isEmpty()) {
      return Map.of();
    }

    LocalDate targetDate = date != null ? date : LocalDate.now();

    // Only MADE_TO_ORDER variants need capacity tracking
    List<ProductVariant> madeToOrderVariants =
        variants.stream()
            .filter(v -> InventoryMode.MADE_TO_ORDER.equals(v.getInventoryMode()))
            .filter(v -> v.getId() != null)
            .toList();

    if (madeToOrderVariants.isEmpty()) {
      return Map.of();
    }

    List<Long> variantIds = madeToOrderVariants.stream().map(ProductVariant::getId).toList();

    String sql =
        "SELECT variant_id, capacity_limit, reserved_quantity "
            + "FROM inventory_capacity_records "
            + "WHERE variant_id IN (:variantIds) AND capacity_date = :capacityDate";

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("variantIds", variantIds)
            .addValue("capacityDate", targetDate);

    Map<Long, int[]> dbRecords = new HashMap<>();
    jdbcTemplate.query(
        sql,
        params,
        rs -> {
          long variantId = rs.getLong("variant_id");
          int limit = rs.getInt("capacity_limit");
          int reserved = rs.getInt("reserved_quantity");
          dbRecords.put(variantId, new int[] {limit, reserved});
        });

    Map<Long, CapacitySnapshot> result = new HashMap<>();

    for (ProductVariant variant : madeToOrderVariants) {
      Long id = variant.getId();
      if (dbRecords.containsKey(id)) {
        int[] rec = dbRecords.get(id);
        int limit = rec[0];
        int reserved = Math.min(rec[1], limit);
        result.put(id, new CapacitySnapshot(targetDate, limit, reserved));
      } else if (variant.getDailyCapacityDefault() != null) {
        int defaultLimit = variant.getDailyCapacityDefault();
        result.put(id, new CapacitySnapshot(targetDate, defaultLimit, 0));
      }
      // If neither db record nor dailyCapacityDefault exists, no snapshot is put.
      // CatalogDtoMapper will mark it as CAPACITY_NOT_CONFIGURED.
    }

    return Map.copyOf(result);
  }
}
