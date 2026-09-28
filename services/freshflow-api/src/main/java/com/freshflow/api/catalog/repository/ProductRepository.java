package com.freshflow.api.catalog.repository;

import com.freshflow.api.catalog.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  List<Product> findAllByStore_IdOrderByNameAsc(Long storeId);

  Optional<Product> findByIdAndStore_Id(Long productId, Long storeId);

  long countByStore_IdAndIsActiveTrue(Long storeId);

  long countByStore_Id(Long storeId);
}
