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
}
