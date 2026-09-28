package com.freshflow.api.catalog.repository;

import com.freshflow.api.catalog.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  java.util.List<Category> findAllByOrderByNameAsc();
}
