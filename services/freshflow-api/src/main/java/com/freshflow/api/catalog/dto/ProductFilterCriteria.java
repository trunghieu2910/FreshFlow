package com.freshflow.api.catalog.dto;

import com.freshflow.api.catalog.model.InventoryMode;

/**
 * Filter and search criteria for querying the product catalog.
 *
 * @param search keyword for search across product name, description, or variant name/size
 * @param storeCategoryId optional filter by store category ID
 * @param size optional filter by variant size (e.g. M, L, STANDARD)
 * @param inventoryMode optional filter by inventory mode (MADE_TO_ORDER, LIMITED_STOCK)
 * @param availableOnly if true, only products with available variants are matched
 * @param activeOnly if true, only active products in active store categories are matched
 */
public record ProductFilterCriteria(
    String search,
    Long storeCategoryId,
    String size,
    InventoryMode inventoryMode,
    Boolean availableOnly,
    Boolean activeOnly) {

  public static ProductFilterCriteria publicCatalog(
      String search,
      Long storeCategoryId,
      String size,
      InventoryMode inventoryMode,
      Boolean availableOnly) {
    return new ProductFilterCriteria(
        search, storeCategoryId, size, inventoryMode, availableOnly, Boolean.TRUE);
  }
}
