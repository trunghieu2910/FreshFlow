package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Long> {}
