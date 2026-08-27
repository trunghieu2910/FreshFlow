package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
  java.util.List<Store> findAllByOrderByNameAsc();
}
