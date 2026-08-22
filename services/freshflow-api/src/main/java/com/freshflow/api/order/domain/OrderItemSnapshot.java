package com.freshflow.api.order.domain;

import com.freshflow.api.common.domain.Money;
import java.math.BigDecimal;
import java.util.Objects;

// OrderItemSnapshot stores one purchased variant exactly as it was at checkout. It prevents a later
// product rename, price change or variant change from modifying historical order data.
public final class OrderItemSnapshot {
  private final Long productVariantId;
  private final String productName;
  private final String variantName;
  private final Money unitPrice;
  private final int quantity;
  private final Money lineTotal;

  public OrderItemSnapshot(
      Long productVariantId,
      String productName,
      String variantName,
      Money unitPrice,
      int quantity) {
    if (productVariantId == null || productVariantId <= 0) {
      throw new IllegalArgumentException(
          "productVariantId must not be null and must be greater than 0");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("productName must not be null or blank");
    }
    if (variantName == null || variantName.trim().isEmpty()) {
      throw new IllegalArgumentException("variantName must not be null or blank");
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("unitPrice must not be null");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be greater than 0");
    }

    this.productVariantId = productVariantId;
    this.productName = productName.trim();
    this.variantName = variantName.trim();
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  public Long getProductVariantId() {
    return productVariantId;
  }

  public String getProductName() {
    return productName;
  }

  public String getVariantName() {
    return variantName;
  }

  public Money getUnitPrice() {
    return unitPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public Money getLineTotal() {
    return lineTotal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrderItemSnapshot that = (OrderItemSnapshot) o;
    return quantity == that.quantity
        && productVariantId.equals(that.productVariantId)
        && productName.equals(that.productName)
        && variantName.equals(that.variantName)
        && unitPrice.equals(that.unitPrice)
        && lineTotal.equals(that.lineTotal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productVariantId, productName, variantName, unitPrice, quantity, lineTotal);
  }

  @Override
  public String toString() {
    return "OrderItemSnapshot{"
        + "productVariantId="
        + productVariantId
        + ", productName='"
        + productName
        + '\''
        + ", variantName='"
        + variantName
        + '\''
        + ", unitPrice="
        + unitPrice
        + ", quantity="
        + quantity
        + ", lineTotal="
        + lineTotal
        + '}';
  }
}
