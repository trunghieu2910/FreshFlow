package com.freshflow.api.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.freshflow.api.common.model.Money;
import com.freshflow.api.order.enums.MerchantAcceptanceStatus;
import com.freshflow.api.order.enums.OrderStatus;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.model.OrderItem;
import com.freshflow.api.order.service.OrderPersistenceService;
import com.freshflow.api.order.service.OrderPersistenceService.LineSelection;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderPersistenceIntegrationTest {
  @Autowired private OrderPersistenceService orderPersistenceService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void persistsSnapshotAndServerCalculatedTotalAfterCatalogChanges() {
    var catalogRow =
        jdbcTemplate.queryForMap(
            "SELECT p.id AS product_id, pv.id AS variant_id, p.store_id, "
                + "p.name AS product_name, pv.name AS variant_name, pv.price "
                + "FROM products p JOIN product_variants pv ON pv.product_id = p.id "
                + "WHERE p.is_active AND pv.is_active AND pv.is_available "
                + "ORDER BY p.id, pv.id LIMIT 1");
    Long productId = id(catalogRow.get("product_id"));
    Long variantId = id(catalogRow.get("variant_id"));
    Long storeId = id(catalogRow.get("store_id"));
    Long customerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'customer.demo@freshflow.vn'", Long.class);
    BigDecimal catalogPrice = (BigDecimal) catalogRow.get("price");

    Order created =
        orderPersistenceService.create(
            "TEST-" + UUID.randomUUID().toString().substring(0, 20),
            customerId,
            storeId,
            OrderStatus.AWAITING_MERCHANT_CONFIRMATION,
            "CASH_ON_DELIVERY",
            MerchantAcceptanceStatus.PENDING,
            List.of(new LineSelection(productId, variantId, 2)),
            new Money(new BigDecimal("12.25")),
            new Money(new BigDecimal("3.10")));
    entityManager.flush();
    Long orderId = created.getId();
    assertNotNull(orderId);
    entityManager.clear();

    Order saved = orderRepository.findById(orderId).orElseThrow();
    OrderItem item = saved.getItems().getFirst();
    assertNotNull(item.getId());
    assertEquals(catalogRow.get("product_name"), item.getProductNameSnapshot());
    assertEquals(catalogRow.get("variant_name"), item.getVariantNameSnapshot());
    assertEquals(0, catalogPrice.compareTo(item.getUnitPriceSnapshot()));
    assertEquals(0, catalogPrice.multiply(BigDecimal.valueOf(2)).compareTo(item.getLineTotal()));
    assertEquals(0, item.getLineTotal().compareTo(saved.getSubtotal()));
    assertEquals(
        0,
        item.getLineTotal()
            .add(new BigDecimal("12.25"))
            .subtract(new BigDecimal("3.10"))
            .compareTo(saved.getTotalAmount()));

    jdbcTemplate.update("UPDATE products SET name = 'Renamed product' WHERE id = ?", productId);
    jdbcTemplate.update(
        "UPDATE product_variants SET name = 'Renamed variant', price = 999.99 WHERE id = ?",
        variantId);
    entityManager.clear();

    Order historical = orderRepository.findById(orderId).orElseThrow();
    OrderItem historicalItem = historical.getItems().getFirst();
    assertEquals(catalogRow.get("product_name"), historicalItem.getProductNameSnapshot());
    assertEquals(catalogRow.get("variant_name"), historicalItem.getVariantNameSnapshot());
    assertEquals(0, catalogPrice.compareTo(historicalItem.getUnitPriceSnapshot()));
    assertEquals(0, saved.getTotalAmount().compareTo(historical.getTotalAmount()));
  }

  @Test
  void rejectsVariantFromAnotherStoreBeforePersistingOrder() {
    var row =
        jdbcTemplate.queryForMap(
            "SELECT p.id AS product_id, pv.id AS variant_id, p.store_id "
                + "FROM products p JOIN product_variants pv ON pv.product_id = p.id "
                + "ORDER BY p.id LIMIT 1");
    Long otherStoreId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM stores WHERE id <> ? ORDER BY id LIMIT 1",
            Long.class,
            id(row.get("store_id")));
    Long customerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'customer.demo@freshflow.vn'", Long.class);
    String orderNumber = "TEST-" + UUID.randomUUID().toString().substring(0, 20);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            orderPersistenceService.create(
                orderNumber,
                customerId,
                otherStoreId,
                OrderStatus.AWAITING_MERCHANT_CONFIRMATION,
                "CASH_ON_DELIVERY",
                MerchantAcceptanceStatus.PENDING,
                List.of(new LineSelection(id(row.get("product_id")), id(row.get("variant_id")), 1)),
                new Money(BigDecimal.ZERO),
                new Money(BigDecimal.ZERO)));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM orders WHERE order_number = ?", Integer.class, orderNumber));
  }

  private static Long id(Object value) {
    return ((Number) value).longValue();
  }
}
