package com.freshflow.api.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.application.query.ProductFilterCriteria;
import com.freshflow.api.catalog.domain.Store;
import com.freshflow.api.catalog.infrastructure.persistence.StoreRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@Transactional
class CatalogQueryPerformanceIntegrationTest {

  @Autowired private CatalogService catalogService;
  @Autowired private StoreRepository storeRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private Long storeId;
  private Statistics statistics;

  @BeforeEach
  void setUp() {
    Store demoStore =
        storeRepository.findAll().stream()
            .filter(s -> "FreshFlow Demo Kitchen".equals(s.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("FreshFlow Demo Kitchen store not found"));
    storeId = demoStore.getId();

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
  }

  @Test
  void listProductsByStore_doesNotProduceNPlusOneQueries() {
    statistics.clear();

    // Request first page with size 20
    Page<ProductCatalogDto> page =
        catalogService.listProductsByStore(storeId, null, PageRequest.of(0, 20));

    // Verify page content
    assertNotNull(page);
    assertEquals(20, page.getContent().size(), "Page 0 should contain exactly 20 products");
    assertTrue(page.getTotalElements() >= 30, "Total seed products must be at least 30");

    // Verify variants are eagerly populated in memory via batch fetch
    for (ProductCatalogDto dto : page.getContent()) {
      assertNotNull(dto.variants());
      assertFalse(dto.variants().isEmpty(), "Every product in demo store should have variants");
    }

    // In an N+1 scenario with 20 products, there would be:
    // 1 (store check) + 1 (count) + 1 (products page) + 20 (variants query per product) = 23+
    // queries.
    // With @BatchSize / batch fetching, Hibernate fetches variants for all 20 products in ONE batch
    // query.
    // Plus 1 batch query for capacity snapshots.
    // Total prepare statements should be <= 6 (specifically: 1 store + 1 count + 1 product + 1
    // variant batch + 1 capacity batch).
    long statementCount = statistics.getPrepareStatementCount();
    assertTrue(
        statementCount <= 6,
        "Expected at most 6 SQL statements with batch fetching, but executed: " + statementCount);
  }

  @Test
  void catalogSearch_withDifferentCases_returnsMatchingProducts() {
    // Test case-insensitive search (lower and upper)
    ProductFilterCriteria searchLower =
        ProductFilterCriteria.publicCatalog("oolong", null, null, null, null);
    Page<ProductCatalogDto> resultsLower =
        catalogService.listProductsByStore(storeId, searchLower, PageRequest.of(0, 10));
    assertFalse(resultsLower.getContent().isEmpty());
    assertTrue(resultsLower.getContent().stream().anyMatch(p -> p.name().contains("Oolong")));

    ProductFilterCriteria searchUpper =
        ProductFilterCriteria.publicCatalog("OOLONG", null, null, null, null);
    Page<ProductCatalogDto> resultsUpper =
        catalogService.listProductsByStore(storeId, searchUpper, PageRequest.of(0, 10));
    assertEquals(resultsLower.getTotalElements(), resultsUpper.getTotalElements());
  }

  @Test
  void catalogPagination_navigatesMultiplePagesConsistently() {
    Page<ProductCatalogDto> page0 =
        catalogService.listProductsByStore(storeId, null, PageRequest.of(0, 15));
    Page<ProductCatalogDto> page1 =
        catalogService.listProductsByStore(storeId, null, PageRequest.of(1, 15));

    assertEquals(15, page0.getContent().size());
    assertTrue(page1.getContent().size() >= 15);
    assertTrue(page0.hasNext());

    // Ensure distinct products between page 0 and page 1
    var idsPage0 = page0.getContent().stream().map(ProductCatalogDto::id).toList();
    var idsPage1 = page1.getContent().stream().map(ProductCatalogDto::id).toList();
    assertTrue(idsPage0.stream().noneMatch(idsPage1::contains), "Pages must not overlap");
  }
}
