package com.freshflow.api.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// Money represents one valid monetary amount in FreshFlow. It prevents the rest of the domain from
// passing unvalidated BigDecimal values everywhere.
public final class Money implements Comparable<Money> {
  private final BigDecimal amount;

  public Money(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    try {
      this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Amount has too many decimal places: " + amount, e);
    }
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Money add(Money other) {
    if (other == null) {
      throw new IllegalArgumentException("Other amount cannot be null");
    }
    return new Money(this.amount.add(other.amount));
  }

  public Money subtract(Money other) {
    if (other == null) {
      throw new IllegalArgumentException("Other amount cannot be null");
    }
    BigDecimal result = this.amount.subtract(other.amount);
    if (result.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Result cannot be negative");
    }
    return new Money(result);
  }

  public Money multiply(BigDecimal multiplier) {
    if (multiplier == null) {
      throw new IllegalArgumentException("Multiplier cannot be null");
    }
    if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Multiplier cannot be negative");
    }
    if (multiplier.compareTo(BigDecimal.ZERO) == 0) {
      return new Money(BigDecimal.ZERO);
    }
    BigDecimal result = this.amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    return new Money(result);
  }

  @Override
  public int compareTo(Money other) {
    if (other == null) {
      throw new IllegalArgumentException("Other amount cannot be null");
    }
    return this.amount.compareTo(other.amount);
  }

  public boolean isZero() {
    return this.amount.signum() == 0;
  }

  public boolean isGreaterThan(Money other) {
    return compareTo(other) > 0;
  }

  public boolean isLessThan(Money other) {
    return compareTo(other) < 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return amount.equals(money.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount);
  }

  @Override
  public String toString() {
    return amount.toString();
  }
}
