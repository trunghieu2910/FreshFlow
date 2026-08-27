package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  java.util.List<Category> findAllByOrderByNameAsc();
}
