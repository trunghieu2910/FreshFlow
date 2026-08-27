package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findAllByStore_IdOrderByNameAsc(Long storeId);
}
