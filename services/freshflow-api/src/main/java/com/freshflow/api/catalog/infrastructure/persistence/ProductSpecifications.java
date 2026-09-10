package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.application.query.ProductFilterCriteria;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import com.freshflow.api.catalog.domain.Store;
import com.freshflow.api.catalog.domain.StoreCategory;
import com.freshflow.api.catalog.domain.StoreStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic catalog querying, search, and filtering.
 *
 * <p>Enforces business rules:
 *
 * <ul>
 *   <li>BR-01: Only active stores, store categories, products, and variants are returned for public
 *       catalog.
 *   <li>BR-03: Sized products filter by size (M, L, etc.); unsized products filter by STANDARD
 *       (size is null).
 *   <li>BR-06: Inventory mode filter (MADE_TO_ORDER / LIMITED_STOCK).
 * </ul>
 */
public final class ProductSpecifications {

  private ProductSpecifications() {}

  public static Specification<Product> withFilter(Long storeId, ProductFilterCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 1. Mandatory store boundary
      predicates.add(cb.equal(root.get("store").get("id"), storeId));

      // 2. Active status constraints
      boolean activeOnly =
          criteria == null
              || criteria.activeOnly() == null
              || Boolean.TRUE.equals(criteria.activeOnly());

      Join<Product, StoreCategory> scJoin = root.join("storeCategory", JoinType.INNER);

      if (activeOnly) {
        predicates.add(cb.isTrue(root.get("isActive")));
        predicates.add(cb.isTrue(scJoin.get("isActive")));

        Join<Product, Store> storeJoin = root.join("store", JoinType.INNER);
        predicates.add(cb.equal(storeJoin.get("status"), StoreStatus.ACTIVE.name()));
      }

      // 3. Store category filter
      if (criteria != null && criteria.storeCategoryId() != null) {
        predicates.add(cb.equal(scJoin.get("id"), criteria.storeCategoryId()));
      }

      // 4. Variant-level filters (search, size, inventoryMode, availableOnly)
      boolean hasSearch =
          criteria != null && criteria.search() != null && !criteria.search().isBlank();
      boolean hasSize = criteria != null && criteria.size() != null && !criteria.size().isBlank();
      boolean hasInventoryMode = criteria != null && criteria.inventoryMode() != null;
      boolean hasAvailableOnly = criteria != null && Boolean.TRUE.equals(criteria.availableOnly());

      if (hasSearch || hasSize || hasInventoryMode || hasAvailableOnly) {
        query.distinct(true);
        Join<Product, ProductVariant> pvJoin = root.join("variants", JoinType.INNER);

        if (activeOnly) {
          predicates.add(cb.isTrue(pvJoin.get("isActive")));
        }

        if (hasSearch) {
          String pattern = "%" + criteria.search().trim().toLowerCase() + "%";
          Predicate pName = cb.like(cb.lower(root.get("name")), pattern);
          Predicate pDesc = cb.like(cb.lower(root.get("description")), pattern);
          Predicate pvName = cb.like(cb.lower(pvJoin.get("name")), pattern);
          predicates.add(cb.or(pName, pDesc, pvName));
        }

        if (hasSize) {
          String size = criteria.size().trim();
          if ("STANDARD".equalsIgnoreCase(size)) {
            Predicate pStdName = cb.equal(cb.upper(pvJoin.get("name")), "STANDARD");
            Predicate pStdSize =
                cb.or(
                    cb.isNull(pvJoin.get("size")),
                    cb.equal(cb.upper(pvJoin.get("size")), "STANDARD"));
            predicates.add(cb.and(pStdName, pStdSize));
          } else {
            Predicate matchSize = cb.equal(cb.upper(pvJoin.get("size")), size.toUpperCase());
            Predicate matchName = cb.equal(cb.upper(pvJoin.get("name")), size.toUpperCase());
            predicates.add(cb.or(matchSize, matchName));
          }
        }

        if (hasInventoryMode) {
          predicates.add(cb.equal(pvJoin.get("inventoryMode"), criteria.inventoryMode()));
        }

        if (hasAvailableOnly) {
          predicates.add(cb.isTrue(pvJoin.get("isAvailable")));
        }
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
