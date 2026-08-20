package com.freshflow.api.order.domain.domain;

import com.freshflow.api.common.domain.Money;
import com.freshflow.api.order.domain.OrderItemSnapshot;
import com.freshflow.api.order.domain.OrderPricingCalculator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderPricingCalculatorTest {

    private final OrderPricingCalculator calculator = new OrderPricingCalculator();
    private final Money deliveryFee = new Money(new BigDecimal("15000.00"));

    @Test
    void shouldRejectNullItems() {
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(null, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee));
    }

    @Test
    void shouldRejectEmptyItems() {
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(new ArrayList<>(), OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee));
    }

    @Test
    void shouldRejectNullItemInsideList() {
        List<OrderItemSnapshot> items = new ArrayList<>();
        items.add(null);
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee));
    }

    @Test
    void shouldCalculateSubtotalFromLineTotals() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2),
                new OrderItemSnapshot(2L, "P2", "V2", new Money(new BigDecimal("25000.00")), 1)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee);
        assertEquals(new Money(new BigDecimal("85000.00")), result.getSubtotal());
    }

    @Test
    void shouldCalculateGrandTotalWithoutDiscount() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee);
        assertEquals(new Money(new BigDecimal("60000.00")), result.getSubtotal());
        assertEquals(new Money(BigDecimal.ZERO), result.getDiscountAmount());
        assertEquals(deliveryFee, result.getDeliveryFee());
        assertEquals(new Money(new BigDecimal("75000.00")), result.getGrandTotal());
    }

    @Test
    void shouldCalculateFixedDiscount() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.FIXED_AMOUNT, new BigDecimal("10000.00"), deliveryFee);
        assertEquals(new Money(new BigDecimal("10000.00")), result.getDiscountAmount());
        assertEquals(new Money(new BigDecimal("65000.00")), result.getGrandTotal());
    }

    @Test
    void shouldCalculatePercentageDiscount() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.PERCENTAGE, new BigDecimal("10"), deliveryFee);
        assertEquals(new Money(new BigDecimal("6000.00")), result.getDiscountAmount());
        assertEquals(new Money(new BigDecimal("69000.00")), result.getGrandTotal());
    }

    @Test
    void shouldRoundPercentageDiscountUsingHalfUp() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("1.00")), 1)
        );
        // 1.00 * 0.005 = 0.005 -> half up -> 0.01
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.PERCENTAGE, new BigDecimal("0.5"), deliveryFee);
        assertEquals(new Money(new BigDecimal("0.01")), result.getDiscountAmount());
    }

    @Test
    void shouldCapDiscountAtSubtotal() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.FIXED_AMOUNT, new BigDecimal("100000.00"), deliveryFee);
        assertEquals(new Money(new BigDecimal("60000.00")), result.getDiscountAmount());
        assertEquals(new Money(new BigDecimal("15000.00")), result.getGrandTotal());
    }

    @Test
    void shouldCalculateDeliveryFee() {
        List<OrderItemSnapshot> items = List.of(
                new OrderItemSnapshot(1L, "P1", "V1", new Money(new BigDecimal("30000.00")), 2)
        );
        OrderPricingCalculator.PricingResult result = calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, deliveryFee);
        assertEquals(deliveryFee, result.getDeliveryFee());
    }

    @Test
    void shouldRejectNullDiscountType() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, null, BigDecimal.ZERO, deliveryFee));
    }

    @Test
    void shouldRejectNullDiscountValue() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, null, deliveryFee));
    }

    @Test
    void shouldRejectNegativeDiscountValue() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.FIXED_AMOUNT, new BigDecimal("-10"), deliveryFee));
    }

    @Test
    void shouldRejectPercentageGreaterThan100() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.PERCENTAGE, new BigDecimal("101"), deliveryFee));
    }

    @Test
    void shouldRejectNonZeroDiscountValueForNone() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, new BigDecimal("10"), deliveryFee));
    }

    @Test
    void shouldRejectNullDeliveryFee() {
        List<OrderItemSnapshot> items = List.of(new OrderItemSnapshot(1L, "P", "V", new Money(new BigDecimal("10")), 1));
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(items, OrderPricingCalculator.DiscountType.NONE, BigDecimal.ZERO, null));
    }
}
