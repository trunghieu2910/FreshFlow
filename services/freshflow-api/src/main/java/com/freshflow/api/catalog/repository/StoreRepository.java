package com.freshflow.api.catalog.repository;

import com.freshflow.api.catalog.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
  java.util.List<Store> findAllByOrderByNameAsc();

  java.util.List<Store> findAllByStatusOrderByNameAsc(String status);
}
