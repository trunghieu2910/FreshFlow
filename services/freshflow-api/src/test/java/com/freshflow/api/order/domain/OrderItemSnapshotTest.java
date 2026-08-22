package com.freshflow.api.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.freshflow.api.common.domain.Money;
import com.freshflow.api.support.AbstractUnitTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemSnapshotTest extends AbstractUnitTest {

  @Test
  void shouldCalculateLineTotal() {
    Money price = new Money(new BigDecimal("30000.00"));
    OrderItemSnapshot item = new OrderItemSnapshot(1L, "Product", "Variant", price, 1);
    assertEquals(new Money(new BigDecimal("30000.00")), item.getLineTotal());
  }

  @Test
  void shouldCalculateLineTotalForMultipleQuantity() {
    Money price = new Money(new BigDecimal("30000.00"));
    OrderItemSnapshot item = new OrderItemSnapshot(101L, "Burger", "LARGE", price, 2);
    assertEquals(new Money(new BigDecimal("60000.00")), item.getLineTotal());
  }

  @Test
  void shouldRejectNullProductVariantId() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(null, "Product", "Variant", price, 1));
  }

  @Test
  void shouldRejectNonPositiveProductVariantId() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(0L, "Product", "Variant", price, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(-5L, "Product", "Variant", price, 1));
  }

  @Test
  void shouldRejectBlankProductName() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(1L, "   ", "Variant", price, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItemSnapshot(1L, null, "Variant", price, 1));
  }

  @Test
  void shouldRejectBlankVariantName() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(1L, "Product", "   ", price, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItemSnapshot(1L, "Product", null, price, 1));
  }

  @Test
  void shouldRejectNullUnitPrice() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(1L, "Product", "Variant", null, 1));
  }

  @Test
  void shouldRejectZeroQuantity() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(1L, "Product", "Variant", price, 0));
  }

  @Test
  void shouldRejectNegativeQuantity() {
    Money price = new Money(new BigDecimal("30000.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new OrderItemSnapshot(1L, "Product", "Variant", price, -1));
  }

  @Test
  void shouldKeepSnapshotValues() {
    Money price = new Money(new BigDecimal("30000.00"));
    OrderItemSnapshot item = new OrderItemSnapshot(101L, "  Burger  ", "  LARGE  ", price, 2);
    assertEquals(101L, item.getProductVariantId());
    assertEquals("Burger", item.getProductName());
    assertEquals("LARGE", item.getVariantName());
    assertEquals(price, item.getUnitPrice());
    assertEquals(2, item.getQuantity());
    assertEquals(new Money(new BigDecimal("60000.00")), item.getLineTotal());
  }
}
