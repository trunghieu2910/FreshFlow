package com.freshflow.api.catalog.repository;

import com.freshflow.api.catalog.model.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
  List<ProductVariant> findAllByProduct_IdOrderByNameAsc(Long productId);

  boolean existsByProduct_IdAndNameIgnoreCase(Long productId, String name);

  boolean existsByProduct_IdAndNameIgnoreCaseAndIdNot(Long productId, String name, Long variantId);
}
