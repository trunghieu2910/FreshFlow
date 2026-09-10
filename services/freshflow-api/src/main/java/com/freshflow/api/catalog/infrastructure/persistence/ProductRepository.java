package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  List<Product> findAllByStore_IdOrderByNameAsc(Long storeId);

  Optional<Product> findByIdAndStore_Id(Long productId, Long storeId);
}
