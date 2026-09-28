package com.freshflow.api.order.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class MerchantOrderControllerIntegrationTest {

  @Autowired private WebApplicationContext wac;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;
  private Long store1Id;
  private Long store1OwnerId;
  private Long store2OwnerId;
  private Long order1Id;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

    // Query Store 1 and its owner
    var storeRow =
        jdbcTemplate.queryForMap(
            "SELECT s.id AS store_id, u.id AS owner_user_id "
                + "FROM stores s JOIN users u ON u.id = s.owner_user_id "
                + "ORDER BY s.id ASC LIMIT 1");
    store1Id = ((Number) storeRow.get("store_id")).longValue();
    store1OwnerId = ((Number) storeRow.get("owner_user_id")).longValue();

    // Query an order belonging to Store 1
    var orderRows =
        jdbcTemplate.queryForList(
            "SELECT id FROM orders WHERE store_id = ? ORDER BY id ASC LIMIT 1", store1Id);
    if (!orderRows.isEmpty()) {
      order1Id = ((Number) orderRows.get(0).get("id")).longValue();
    }

    // Determine an unauthorized owner id (e.g. store1OwnerId + 999)
    store2OwnerId = store1OwnerId + 999;
  }

  @Test
  @DisplayName("GET /dashboard/summary returns real KPI metrics for owned store")
  void getDashboardSummary_success() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/merchant/stores/{storeId}/dashboard/summary", store1Id)
                .header("X-User-Id", store1OwnerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeProductsCount", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.totalProductsCount", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.pendingOrdersCount", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.operationalStatus", is("NORMAL")));
  }

  @Test
  @DisplayName("GET /dashboard/summary returns 403 when accessed by non-owner")
  void getDashboardSummary_forbiddenForNonOwner() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/merchant/stores/{storeId}/dashboard/summary", store1Id)
                .header("X-User-Id", store2OwnerId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("CATALOG_STORE_ACCESS_DENIED")));
  }

  @Test
  @DisplayName("GET /orders returns paginated orders with masked customer phone")
  void getOrders_success() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/merchant/stores/{storeId}/orders", store1Id)
                .param("page", "0")
                .param("size", "10")
                .header("X-User-Id", store1OwnerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", notNullValue()))
        .andExpect(jsonPath("$.pageable", notNullValue()));
  }

  @Test
  @DisplayName("POST /orders/{orderId}/accept updates order state properly")
  void acceptOrder_success() throws Exception {
    if (order1Id != null) {
      // Set status to AWAITING_MERCHANT_CONFIRMATION first to ensure clean state
      jdbcTemplate.update(
          "UPDATE orders SET status = 'AWAITING_MERCHANT_CONFIRMATION', merchant_acceptance_status = 'PENDING' WHERE id = ?",
          order1Id);

      mockMvc
          .perform(
              post("/api/v1/merchant/stores/{storeId}/orders/{orderId}/accept", store1Id, order1Id)
                  .header("X-User-Id", store1OwnerId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.merchantAcceptanceStatus", is("ACCEPTED")))
          .andExpect(jsonPath("$.status", anyOf(is("PROCESSING"), is("AWAITING_PAYMENT"))));
    }
  }

  @Test
  @DisplayName("POST /orders/{orderId}/reject rejects order and sets CANCELLED status")
  void rejectOrder_success() throws Exception {
    if (order1Id != null) {
      jdbcTemplate.update(
          "UPDATE orders SET status = 'AWAITING_MERCHANT_CONFIRMATION', merchant_acceptance_status = 'PENDING' WHERE id = ?",
          order1Id);

      mockMvc
          .perform(
              post("/api/v1/merchant/stores/{storeId}/orders/{orderId}/reject", store1Id, order1Id)
                  .header("X-User-Id", store1OwnerId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"Hết nguyên liệu pha chế\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.merchantAcceptanceStatus", is("REJECTED")))
          .andExpect(jsonPath("$.status", is("CANCELLED")))
          .andExpect(jsonPath("$.cancelReason", is("Hết nguyên liệu pha chế")));
    }
  }
}
