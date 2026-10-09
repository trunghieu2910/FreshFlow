package com.freshflow.api.order.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class OrderReadControllerIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private com.freshflow.api.identity.service.RoleGrantService grants;
  private MockMvc mvc;
  private long orderId;
  private long storeId;
  private long customerId;
  private long merchantId;
  private long otherMerchantId;
  private long otherStoreId;
  private long strangerId;
  private long driverId;
  private long otherDriverId;
  private long assignmentId;
  private String snapshotName;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    Map<String, Object> order =
        jdbc.queryForMap(
            "SELECT o.id, o.store_id, o.customer_user_id, s.owner_user_id "
                + "FROM orders o JOIN stores s ON s.id = o.store_id "
                + "WHERE EXISTS (SELECT 1 FROM order_items i WHERE i.order_id = o.id) "
                + "ORDER BY o.id LIMIT 1");
    orderId = id(order.get("id"));
    storeId = id(order.get("store_id"));
    customerId = id(order.get("customer_user_id"));
    merchantId = id(order.get("owner_user_id"));
    Map<String, Object> otherStore =
        jdbc.queryForMap(
            "SELECT id, owner_user_id FROM stores WHERE id <> ? ORDER BY id LIMIT 1", storeId);
    otherStoreId = id(otherStore.get("id"));
    otherMerchantId = id(otherStore.get("owner_user_id"));
    strangerId = user("stranger");
    driverId = user("driver");
    otherDriverId = user("other-driver");
    long profileId = driverProfile(driverId, storeId);
    driverProfile(otherDriverId, storeId);
    jdbc.update(
        "UPDATE orders SET status = 'SHIPPING', merchant_acceptance_status = 'ACCEPTED', "
            + "payment_method = 'CASH_ON_DELIVERY', current_driver_id = ?, "
            + "recipient_name_snapshot = 'Snapshot Recipient', "
            + "recipient_phone_snapshot = '0901234567', address_line_snapshot = 'Snapshot Street', "
            + "ward_snapshot = 'Ward 1', district_snapshot = 'District 1', "
            + "province_snapshot = 'Province 1' "
            + "WHERE id = ?",
        profileId,
        orderId);
    jdbc.update("DELETE FROM payments WHERE order_id = ?", orderId);
    jdbc.update(
        "INSERT INTO payments (order_id, attempt_number, method, status, amount, "
            + "created_at, updated_at) VALUES (?, 1, 'CASH_ON_DELIVERY', 'CASH_COLLECTED', "
            + "(SELECT total_amount FROM orders WHERE id = ?), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        orderId,
        orderId);
    assignmentId =
        jdbc.queryForObject(
            "INSERT INTO delivery_assignments (order_id, driver_profile_id, status, "
                + "attempt_number, assigned_at, created_at, updated_at) "
                + "VALUES (?, ?, 'DELIVERING', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, "
                + "CURRENT_TIMESTAMP) RETURNING id",
            Long.class,
            orderId,
            profileId);
    jdbc.update(
        "INSERT INTO delivery_credentials (delivery_assignment_id, otp_hash, expires_at, "
            + "attempt_count, max_attempts, created_at) VALUES (?, 'secret-otp-hash', ?, 0, 3, CURRENT_TIMESTAMP)",
        assignmentId,
        OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
    jdbc.update(
        "INSERT INTO disputes (order_id, customer_user_id, reason, customer_message, "
            + "status, created_at, updated_at) VALUES (?, ?, 'LATE', 'Private dispute note', "
            + "'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        orderId,
        customerId);
    jdbc.update(
        "INSERT INTO order_audits (order_id, actor_user_id, actor_role, event_type, "
            + "from_status, to_status, reason, created_at) "
            + "VALUES (?, ?, 'MERCHANT', 'ORDER_DISPATCHED', 'PROCESSING', 'SHIPPING', "
            + "'Private audit reason', CURRENT_TIMESTAMP)",
        orderId,
        merchantId);
    Map<String, Object> item =
        jdbc.queryForMap(
            "SELECT product_id, product_name_snapshot FROM order_items WHERE order_id = ? "
                + "AND product_id IS NOT NULL ORDER BY id LIMIT 1",
            orderId);
    snapshotName = (String) item.get("product_name_snapshot");
    jdbc.update(
        "UPDATE products SET name = ? WHERE id = ?",
        "Changed catalog name",
        item.get("product_id"));
  }

  @Test
  void customerGetsOwnSnapshotAndFullProjectionButNotOtpHash() throws Exception {
    mvc.perform(get("/api/v1/orders/{id}", orderId).header("X-User-Id", customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].productName", is(snapshotName)))
        .andExpect(jsonPath("$.items[0].variantName", notNullValue()))
        .andExpect(jsonPath("$.items[0].unitPrice", notNullValue()))
        .andExpect(jsonPath("$.merchantAcceptanceStatus", is("ACCEPTED")))
        .andExpect(jsonPath("$.paymentMethod", is("CASH_ON_DELIVERY")))
        .andExpect(jsonPath("$.paymentStatus", is("CASH_COLLECTED")))
        .andExpect(jsonPath("$.delivery.driverUserId", is((int) driverId)))
        .andExpect(jsonPath("$.address.addressLine", is("Snapshot Street")))
        .andExpect(jsonPath("$.otp.required", is(true)))
        .andExpect(jsonPath("$.otp.verificationReady", is(true)))
        .andExpect(jsonPath("$.otp.codeVisible", is(false)))
        .andExpect(jsonPath("$.disputes[0].customerMessage", is("Private dispute note")))
        .andExpect(jsonPath("$.events[0].eventType", is("ORDER_DISPATCHED")))
        .andExpect(jsonPath("$..otpHash").doesNotExist());
    mvc.perform(get("/api/v1/orders").header("X-User-Id", customerId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.content[?(@.id == " + orderId + ")].paymentStatus", hasItem("CASH_COLLECTED")))
        .andExpect(
            jsonPath("$.content[?(@.id == " + orderId + ")].deliveryStatus", hasItem("DELIVERING")))
        .andExpect(
            jsonPath("$.content[?(@.id == " + orderId + ")].disputeStatus", hasItem("OPEN")));
    jdbc.update(
        "UPDATE delivery_credentials SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' "
            + "WHERE delivery_assignment_id = ?",
        assignmentId);
    mvc.perform(get("/api/v1/orders/{id}", orderId).header("X-User-Id", customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.otp.verificationReady", is(false)));
  }

  @Test
  void customerCannotReadAnotherCustomersOrder() throws Exception {
    mvc.perform(get("/api/v1/orders/{id}", orderId).header("X-User-Id", strangerId))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/orders").header("X-User-Id", strangerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(0)));
    mvc.perform(get("/api/v1/orders/{id}", orderId)).andExpect(status().isBadRequest());
  }

  @Test
  void merchantReadsOnlyOwnedStore() throws Exception {
    mvc.perform(
            get("/api/v1/merchant/stores/{store}/orders/{id}", storeId, orderId)
                .header("X-User-Id", merchantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.delivery.status", is("DELIVERING")))
        .andExpect(jsonPath("$.disputes[0].status", is("OPEN")))
        .andExpect(jsonPath("$.otp.verificationReady", is(false)));
    mvc.perform(
            get("/api/v1/merchant/stores/{store}/orders/history", storeId)
                .header("X-User-Id", merchantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", greaterThan(0)));
    mvc.perform(
            get("/api/v1/merchant/stores/{store}/orders/{id}", storeId, orderId)
                .header("X-User-Id", otherMerchantId))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/merchant/stores/{store}/orders/{id}", otherStoreId, orderId)
                .header("X-User-Id", otherMerchantId))
        .andExpect(status().isNotFound());
  }

  @Test
  void driverOnlyReadsLatestAssignmentInOwnStoreAndPrivateNotesAreHidden() throws Exception {
    mvc.perform(get("/api/v1/driver/orders/{id}", orderId).header("X-User-Id", driverId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.delivery.assignmentId", is((int) assignmentId)))
        .andExpect(jsonPath("$.disputes[0].status", is("OPEN")))
        .andExpect(jsonPath("$.disputes[0].customerMessage").value(nullValue()))
        .andExpect(jsonPath("$.events[0].reason").value(nullValue()))
        .andExpect(jsonPath("$.otp.verificationReady", is(false)));
    mvc.perform(get("/api/v1/driver/orders/{id}", orderId).header("X-User-Id", otherDriverId))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/driver/orders").header("X-User-Id", otherDriverId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(0)));
    long wrongStoreDriver = user("wrong-store-driver");
    long wrongProfile = driverProfile(wrongStoreDriver, otherStoreId);
    jdbc.update(
        "UPDATE delivery_assignments SET status = 'ENDED', ended_at = CURRENT_TIMESTAMP "
            + "WHERE id = ?",
        assignmentId);
    jdbc.update(
        "INSERT INTO delivery_assignments (order_id, driver_profile_id, status, attempt_number, "
            + "assigned_at, created_at, updated_at) VALUES (?, ?, 'ASSIGNED', 2, "
            + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        orderId,
        wrongProfile);
    mvc.perform(get("/api/v1/driver/orders/{id}", orderId).header("X-User-Id", driverId))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/driver/orders/{id}", orderId).header("X-User-Id", wrongStoreDriver))
        .andExpect(status().isNotFound());
  }

  private long user(String prefix) {
    long userId =
        jdbc.queryForObject(
            "INSERT INTO users (email, password_hash, full_name, status, created_at, updated_at) "
                + "VALUES (?, 'test', ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id",
            Long.class,
            prefix + UUID.randomUUID() + "@test.local",
            prefix);
    grants.grant(userId, null, "CUSTOMER");
    return userId;
  }

  @Test
  void inactiveDriverIdentityIsDeniedInHistoryAndDetail() throws Exception {
    jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", driverId);
    assertDriverDenied();
    jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", driverId);
    for (String state : java.util.List.of("SUSPENDED", "INACTIVE")) {
      jdbc.update("UPDATE driver_profiles SET status = ? WHERE user_id = ?", state, driverId);
      assertDriverDenied();
    }
    jdbc.update("UPDATE driver_profiles SET status = 'ACTIVE' WHERE user_id = ?", driverId);
    jdbc.update("UPDATE user_store_roles SET status = 'INACTIVE' WHERE user_id = ?", driverId);
    assertDriverDenied();
  }

  private void assertDriverDenied() throws Exception {
    mvc.perform(get("/api/v1/driver/orders").header("X-User-Id", driverId))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/driver/orders/{id}", orderId).header("X-User-Id", driverId))
        .andExpect(status().isForbidden());
  }

  private long driverProfile(long userId, long profileStoreId) {
    grants.grant(userId, profileStoreId, "DRIVER");
    return jdbc.queryForObject(
        "INSERT INTO driver_profiles (user_id, store_id, status, created_at, updated_at) "
            + "VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id",
        Long.class,
        userId,
        profileStoreId);
  }

  private static long id(Object value) {
    return ((Number) value).longValue();
  }
}
