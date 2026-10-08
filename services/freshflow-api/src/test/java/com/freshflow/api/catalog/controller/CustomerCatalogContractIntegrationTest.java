package com.freshflow.api.catalog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Contract tests for the customer-facing catalog. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class CustomerCatalogContractIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;
  private Long storeId;
  private Long productId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var product =
        jdbcTemplate.queryForMap(
            "SELECT s.id AS store_id, p.id AS product_id "
                + "FROM stores s JOIN products p ON p.store_id = s.id "
                + "WHERE s.name = 'FreshFlow Demo Kitchen' AND p.name = 'Classic Milk Tea' "
                + "ORDER BY p.id LIMIT 1");
    storeId = ((Number) product.get("store_id")).longValue();
    productId = ((Number) product.get("product_id")).longValue();
  }

  @Test
  void productDetail_exposesPurchasableVariantCategoryAndLiveCapacity() throws Exception {
    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products/{productId}", storeId, productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(productId))
        .andExpect(jsonPath("$.storeCategory.id").isNumber())
        .andExpect(jsonPath("$.storeCategory.categoryId").isNumber())
        .andExpect(jsonPath("$.storeCategory.name").isNotEmpty())
        .andExpect(jsonPath("$.variants[0].id").isNumber())
        .andExpect(jsonPath("$.variants[0].price").isNumber())
        .andExpect(jsonPath("$.variants[0].inventoryMode").isNotEmpty())
        .andExpect(jsonPath("$.variants[0].available").isBoolean())
        .andExpect(jsonPath("$.variants[0].availabilityStatus").isNotEmpty());
  }

  @Test
  void catalog_doesNotExposeAnInactiveVariantAsPurchasable() throws Exception {
    Long variantId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM product_variants WHERE product_id = ? ORDER BY id LIMIT 1",
            Long.class,
            productId);
    jdbcTemplate.update("UPDATE product_variants SET is_active = false WHERE id = ?", variantId);

    String response =
        mockMvc
            .perform(get("/api/v1/stores/{storeId}/products/{productId}", storeId, productId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode variants = objectMapper.readTree(response).path("variants");
    for (JsonNode variant : variants) {
      org.junit.jupiter.api.Assertions.assertNotEquals(variantId.longValue(), variant.path("id").asLong());
      org.junit.jupiter.api.Assertions.assertTrue(variant.path("active").asBoolean());
    }
  }

  @Test
  void productDetail_isNotReachableThroughAnotherStore() throws Exception {
    Long anotherStoreId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM stores WHERE id <> ? ORDER BY id LIMIT 1", Long.class, storeId);

    mockMvc
        .perform(
            get(
                "/api/v1/stores/{storeId}/products/{productId}", anotherStoreId, productId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CATALOG_PRODUCT_NOT_FOUND"));
  }
}
