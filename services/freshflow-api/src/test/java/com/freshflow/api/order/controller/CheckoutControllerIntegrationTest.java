package com.freshflow.api.order.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CheckoutControllerIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;
  private Long customerId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    customerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'customer.demo@freshflow.vn'", Long.class);
  }

  @Test
  @Transactional
  void createsOrderWithServerPriceAddressPaymentAndIdempotentReplay() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    Long variantId = id(variant.get("variant_id"));
    Integer reservedBefore = reserved(variantId);
    String body = body(id(variant.get("store_id")), id(variant.get("product_id")), variantId, 2);
    String key = UUID.randomUUID().toString();

    MvcResult first =
        mockMvc
            .perform(
                post("/api/v1/orders")
                    .header("X-User-Id", customerId)
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", greaterThan(0)))
            .andExpect(jsonPath("$.address.recipientName", is("Nguyễn Văn An")))
            .andExpect(jsonPath("$.items[0].quantity", is(2)))
            .andReturn();
    JsonNode created = objectMapper.readTree(first.getResponse().getContentAsString());
    long orderId = created.get("id").asLong();
    BigDecimal expected = ((BigDecimal) variant.get("price")).multiply(BigDecimal.valueOf(2));
    org.junit.jupiter.api.Assertions.assertEquals(
        0, expected.compareTo(created.get("totalAmount").decimalValue()));
    org.junit.jupiter.api.Assertions.assertEquals(
        0, expected.compareTo(created.get("items").get(0).get("lineTotal").decimalValue()));

    jdbcTemplate.update("UPDATE orders SET status = 'COMPLETED' WHERE id = ?", orderId);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is((int) orderId)))
        .andExpect(jsonPath("$.status", is(created.get("status").asText())));
    org.junit.jupiter.api.Assertions.assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM payments WHERE order_id = ?", Integer.class, orderId));
    org.junit.jupiter.api.Assertions.assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM orders WHERE id = ?", Integer.class, orderId));
    org.junit.jupiter.api.Assertions.assertEquals(
        "Nguyễn Văn An",
        jdbcTemplate.queryForObject(
            "SELECT recipient_name_snapshot FROM orders WHERE id = ?", String.class, orderId));
    org.junit.jupiter.api.Assertions.assertEquals(
        reservedBefore + 2,
        jdbcTemplate.queryForObject(
            "SELECT SUM(reserved_quantity) FROM inventory_capacity_records "
                + "WHERE variant_id = ? AND capacity_date = ?",
            Integer.class,
            variantId,
            LocalDate.now(ZoneOffset.UTC)));
  }

  @Test
  @Transactional
  void startsAutoAcceptedOnlineOrderAwaitingPayment() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    Long variantId = id(variant.get("variant_id"));
    jdbcTemplate.update(
        "UPDATE product_variants SET auto_accept_override = true WHERE id = ?", variantId);
    String body =
        body(id(variant.get("store_id")), id(variant.get("product_id")), variantId, 1)
            .replace("CASH_ON_DELIVERY", "ONLINE_MOCK");

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")))
        .andExpect(jsonPath("$.merchantAcceptanceStatus", is("ACCEPTED")));
  }

  @Test
  @Transactional
  void rejectsInactiveProductWithoutCreatingOrder() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    Long productId = id(variant.get("product_id"));
    Long variantId = id(variant.get("variant_id"));
    jdbcTemplate.update("UPDATE products SET is_active = false WHERE id = ?", productId);
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(id(variant.get("store_id")), productId, variantId, 1)))
        .andExpect(status().isConflict());
  }

  @Test
  @Transactional
  void rejectsInvalidQuantityAndReusedKeyWithDifferentBody() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    Long storeId = id(variant.get("store_id"));
    Long productId = id(variant.get("product_id"));
    Long variantId = id(variant.get("variant_id"));
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(storeId, productId, variantId, 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(storeId, productId, variantId, 1)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(storeId, productId, variantId, 2)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code", is("ORDER_IDEMPOTENCY_CONFLICT")));
  }

  @Test
  void rollsBackFirstReservationWhenLaterItemBelongsToAnotherStore() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    Long storeId = id(variant.get("store_id"));
    Long variantId = id(variant.get("variant_id"));
    Map<String, Object> other =
        jdbcTemplate.queryForMap(
            "SELECT p.id AS product_id, pv.id AS variant_id "
                + "FROM products p JOIN product_variants pv ON pv.product_id = p.id "
                + "WHERE p.store_id <> ? ORDER BY pv.id LIMIT 1",
            storeId);
    Integer reservedBefore = reserved(variantId);
    String body =
        body(storeId, id(variant.get("product_id")), variantId, 1)
            .replace(
                "\"items\":[{\"productId\":"
                    + id(variant.get("product_id"))
                    + ",\"variantId\":"
                    + variantId
                    + ",\"quantity\":1}]",
                "\"items\":[{\"productId\":"
                    + id(variant.get("product_id"))
                    + ",\"variantId\":"
                    + variantId
                    + ",\"quantity\":1},"
                    + "{\"productId\":"
                    + id(other.get("product_id"))
                    + ",\"variantId\":"
                    + id(other.get("variant_id"))
                    + ",\"quantity\":1}]");

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
    org.junit.jupiter.api.Assertions.assertEquals(reservedBefore, reserved(variantId));
  }

  private Integer reserved(Long variantId) {
    return jdbcTemplate.queryForObject(
        "SELECT COALESCE(SUM(reserved_quantity), 0) FROM inventory_capacity_records "
            + "WHERE variant_id = ? AND capacity_date = ?",
        Integer.class,
        variantId,
        LocalDate.now(ZoneOffset.UTC));
  }

  @Test
  @Transactional
  void lockedCustomerCannotCheckout() throws Exception {
    Map<String, Object> variant = purchasableVariant();
    jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", customerId);
    mockMvc
        .perform(
            post("/api/v1/orders")
                .header("X-User-Id", customerId)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        id(variant.get("store_id")),
                        id(variant.get("product_id")),
                        id(variant.get("variant_id")),
                        1)))
        .andExpect(status().isForbidden());
  }

  private Map<String, Object> purchasableVariant() {
    return jdbcTemplate.queryForMap(
        "SELECT p.id AS product_id, pv.id AS variant_id, p.store_id, pv.price "
            + "FROM products p JOIN product_variants pv ON pv.product_id = p.id "
            + "JOIN stores s ON s.id = p.store_id "
            + "WHERE s.status = 'ACTIVE' AND p.is_active AND pv.is_active AND pv.is_available "
            + "AND pv.price > 0.01 "
            + "AND pv.inventory_mode = 'MADE_TO_ORDER' AND pv.daily_capacity_default >= 2 "
            + "AND (pv.max_quantity_per_order IS NULL OR pv.max_quantity_per_order >= 2) "
            + "ORDER BY pv.id LIMIT 1");
  }

  private static String body(Long storeId, Long productId, Long variantId, int quantity) {
    return "{\"storeId\":"
        + storeId
        + ",\"items\":[{\"productId\":"
        + productId
        + ",\"variantId\":"
        + variantId
        + ",\"quantity\":"
        + quantity
        + "}],\"paymentMethod\":\"CASH_ON_DELIVERY\",\"totalAmount\":0.01,"
        + "\"address\":{\"recipientName\":\"Nguyễn Văn An\","
        + "\"phone\":\"0901234567\",\"addressLine\":\"1 Main Street\","
        + "\"ward\":\"Ward 1\",\"district\":\"District 1\","
        + "\"province\":\"Hồ Chí Minh\"}}";
  }

  private static Long id(Object value) {
    return ((Number) value).longValue();
  }
}
