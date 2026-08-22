package com.freshflow.api.order.domain;

import com.freshflow.api.common.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// OrderPricingCalculator calculates the final price of a cart after the item snapshots, discount
// and delivery fee are known. It does not access the database, payment gateway, inventory or
// product service.
public class OrderPricingCalculator {

  public enum DiscountType {
    NONE,
    FIXED_AMOUNT,
    PERCENTAGE
  }

  public static final class PricingResult {
    private final Money subtotal;
    private final Money discountAmount;
    private final Money deliveryFee;
    private final Money grandTotal;

    private PricingResult(
        Money subtotal, Money discountAmount, Money deliveryFee, Money grandTotal) {
      this.subtotal = subtotal;
      this.discountAmount = discountAmount;
      this.deliveryFee = deliveryFee;
      this.grandTotal = grandTotal;
    }

    public Money getSubtotal() {
      return subtotal;
    }

    public Money getDiscountAmount() {
      return discountAmount;
    }

    public Money getDeliveryFee() {
      return deliveryFee;
    }

    public Money getGrandTotal() {
      return grandTotal;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PricingResult that = (PricingResult) o;
      return subtotal.equals(that.subtotal)
          && discountAmount.equals(that.discountAmount)
          && deliveryFee.equals(that.deliveryFee)
          && grandTotal.equals(that.grandTotal);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subtotal, discountAmount, deliveryFee, grandTotal);
    }

    @Override
    public String toString() {
      return "PricingResult{"
          + "subtotal="
          + subtotal
          + ", discountAmount="
          + discountAmount
          + ", deliveryFee="
          + deliveryFee
          + ", grandTotal="
          + grandTotal
          + '}';
    }
  }

  public PricingResult calculate(
      List<OrderItemSnapshot> items,
      DiscountType discountType,
      BigDecimal discountValue,
      Money deliveryFee) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("items must not be null or empty");
    }
    for (OrderItemSnapshot item : items) {
      if (item == null) {
        throw new IllegalArgumentException("item inside list must not be null");
      }
    }
    if (discountType == null) {
      throw new IllegalArgumentException("discountType must not be null");
    }
    if (discountValue == null) {
      throw new IllegalArgumentException("discountValue must not be null");
    }
    if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("discountValue must not be negative");
    }
    if (deliveryFee == null) {
      throw new IllegalArgumentException("deliveryFee must not be null");
    }
    if (discountType == DiscountType.PERCENTAGE) {
      if (discountValue.compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalArgumentException("Percentage value must be between 0 and 100");
      }
    }
    if (discountType == DiscountType.NONE && discountValue.compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalArgumentException("discountValue must be zero when discountType is NONE");
    }

    Money subtotal = new Money(BigDecimal.ZERO);
    for (OrderItemSnapshot item : items) {
      subtotal = subtotal.add(item.getLineTotal());
    }

    Money discountAmount;
    if (discountType == DiscountType.FIXED_AMOUNT) {
      discountAmount = new Money(discountValue);
    } else if (discountType == DiscountType.PERCENTAGE) {
      BigDecimal percentageFactor = discountValue.divide(new BigDecimal("100"));
      discountAmount = subtotal.multiply(percentageFactor);
    } else {
      discountAmount = new Money(BigDecimal.ZERO);
    }

    if (discountAmount.isGreaterThan(subtotal)) {
      discountAmount = subtotal;
    }

    Money grandTotal = subtotal.subtract(discountAmount).add(deliveryFee);

    return new PricingResult(subtotal, discountAmount, deliveryFee, grandTotal);
  }
}
