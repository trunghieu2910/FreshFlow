package com.freshflow.api.catalog.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;

public record CapacitySnapshot(LocalDate capacityDate, int capacityLimit, int reservedQuantity) {
  public CapacitySnapshot {
    Objects.requireNonNull(capacityDate, "capacityDate must not be null");
    if (capacityLimit < 0) {
      throw new IllegalArgumentException("capacityLimit must not be negative");
    }
    if (reservedQuantity < 0) {
      throw new IllegalArgumentException("reservedQuantity must not be negative");
    }
    if (reservedQuantity > capacityLimit) {
      throw new IllegalArgumentException("reservedQuantity must not exceed capacityLimit");
    }
  }

  public int remaining() {
    return capacityLimit - reservedQuantity;
  }
}
