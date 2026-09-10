package com.freshflow.api.catalog.api.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class ProductControllerIntegrationTest {

  @Autowired private WebApplicationContext wac;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;
  private Long ownerUserId;
  private Long storeId;
  private Long productId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    var row =
        jdbcTemplate.queryForMap(
            "SELECT s.id AS store_id, u.id AS owner_user_id, p.id AS product_id "
                + "FROM stores s JOIN users u ON u.id = s.owner_user_id "
                + "JOIN products p ON p.store_id = s.id "
                + "WHERE s.name = 'FreshFlow Demo Kitchen' "
                + "AND p.name = 'Classic Milk Tea' "
                + "ORDER BY p.id LIMIT 1");
    storeId = ((Number) row.get("store_id")).longValue();
    ownerUserId = ((Number) row.get("owner_user_id")).longValue();
    productId = ((Number) row.get("product_id")).longValue();
  }

  @Test
  void createsAndListsStandardVariantWithNullSize() throws Exception {
    String request =
        "{\"name\":\"STANDARD\",\"size\":null,\"price\":25000.00,\"inventoryMode\":\"LIMITED_STOCK\",\"available\":true}";

    mockMvc
        .perform(
            post(
                    "/api/v1/merchant/stores/{storeId}/products/{productId}/variants",
                    storeId,
                    productId)
                .header("X-User-Id", ownerUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("STANDARD"))
        .andExpect(jsonPath("$.size").value(nullValue()))
        .andExpect(jsonPath("$.inventoryMode").value("LIMITED_STOCK"));
  }

  @Test
  void rejectsInvalidPrice() throws Exception {
    String request =
        "{\"name\":\"M-INVALID\",\"size\":\"M\",\"price\":0,\"inventoryMode\":\"MADE_TO_ORDER\"}";

    mockMvc
        .perform(
            post(
                    "/api/v1/merchant/stores/{storeId}/products/{productId}/variants",
                    storeId,
                    productId)
                .header("X-User-Id", ownerUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsDuplicateVariantName() throws Exception {
    String request =
        "{\"name\":\"M\",\"size\":\"M\",\"price\":35000.00,\"inventoryMode\":\"MADE_TO_ORDER\"}";

    mockMvc
        .perform(
            post(
                    "/api/v1/merchant/stores/{storeId}/products/{productId}/variants",
                    storeId,
                    productId)
                .header("X-User-Id", ownerUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATALOG_VARIANT_DUPLICATE"));
  }

  @Test
  void rejectsUnauthorizedStoreAccess() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/merchant/stores/{storeId}/products/{productId}", storeId, productId)
                .header("X-User-Id", 999999L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CATALOG_STORE_ACCESS_DENIED"));
  }

  @Test
  void softDeletesVariantInsteadOfHardDeleting() throws Exception {
    String request =
        "{\"name\":\"XL-TEMP\",\"size\":\"XL\",\"price\":55000.00,\"inventoryMode\":\"MADE_TO_ORDER\"}";
    String body =
        mockMvc
            .perform(
                post(
                        "/api/v1/merchant/stores/{storeId}/products/{productId}/variants",
                        storeId,
                        productId)
                    .header("X-User-Id", ownerUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long variantId = objectMapper.readTree(body).get("id").asLong();

    mockMvc
        .perform(
            delete(
                    "/api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}",
                    storeId,
                    productId,
                    variantId)
                .header("X-User-Id", ownerUserId))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(
                "/api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}",
                storeId,
                productId,
                variantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void listsProductsWithPaginationAndSorting() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/stores/{storeId}/products", storeId)
                .param("page", "0")
                .param("size", "1")
                .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.totalPages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.first").value(true));
  }

  @Test
  void searchesProductsByKeyword() throws Exception {
    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId).param("search", "Croissant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Butter Croissant"));
  }

  @Test
  void filtersProductsByVariantSize_M_and_STANDARD() throws Exception {
    // Sized M filter
    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId).param("size", "M"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Classic Milk Tea"));

    // STANDARD (unsized) filter
    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId).param("size", "STANDARD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Butter Croissant"));
  }

  @Test
  void doesNotLeakInactiveProductsOrInactiveCategories() throws Exception {
    // Soft-deactivate Classic Milk Tea
    jdbcTemplate.update("UPDATE products SET is_active = false WHERE id = ?", productId);

    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[*].name")
                .value(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Classic Milk Tea"))));

    // Reactivate product, but deactivate its StoreCategory
    jdbcTemplate.update("UPDATE products SET is_active = true WHERE id = ?", productId);
    Long storeCatId =
        jdbcTemplate.queryForObject(
            "SELECT store_category_id FROM products WHERE id = ?", Long.class, productId);
    jdbcTemplate.update("UPDATE store_categories SET is_active = false WHERE id = ?", storeCatId);

    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[*].name")
                .value(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Classic Milk Tea"))));
  }

  @Test
  void returnsCapacityExhaustedWhenDailyCapacityIsFull() throws Exception {
    // Get beverage location or create one
    var locations =
        jdbcTemplate.queryForList(
            "SELECT id FROM inventory_locations WHERE store_id = ? LIMIT 1", Long.class, storeId);
    Long locationId;
    if (locations.isEmpty()) {
      jdbcTemplate.update(
          "INSERT INTO inventory_locations (store_id, name, type, is_default, is_active, created_at, updated_at) "
              + "VALUES (?, 'Main Kitchen', 'MAIN_KITCHEN', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
          storeId);
      locationId =
          jdbcTemplate.queryForObject(
              "SELECT id FROM inventory_locations WHERE store_id = ? LIMIT 1", Long.class, storeId);
    } else {
      locationId = locations.get(0);
    }

    Long variantId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM product_variants WHERE product_id = ? AND name = 'M' LIMIT 1",
            Long.class,
            productId);

    // Insert capacity record for today with capacity_limit = 10 and reserved_quantity = 10
    // (remaining = 0)
    jdbcTemplate.update(
        "INSERT INTO inventory_capacity_records (variant_id, location_id, capacity_date, capacity_limit, reserved_quantity, created_at, updated_at) "
            + "VALUES (?, ?, CURRENT_DATE, 10, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (variant_id, location_id, capacity_date) DO UPDATE "
            + "SET capacity_limit = 10, reserved_quantity = 10",
        variantId,
        locationId);

    mockMvc
        .perform(get("/api/v1/stores/{storeId}/products", storeId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.content[?(@.name == 'Classic Milk Tea')].variants[?(@.name == 'M')].available")
                .value(false))
        .andExpect(
            jsonPath(
                    "$.content[?(@.name == 'Classic Milk Tea')].variants[?(@.name == 'M')].availabilityStatus")
                .value("CAPACITY_EXHAUSTED"))
        .andExpect(
            jsonPath(
                    "$.content[?(@.name == 'Classic Milk Tea')].variants[?(@.name == 'M')].capacity.remaining")
                .value(0));
  }
}
