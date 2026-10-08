package com.freshflow.api.order.exception;

public enum OrderErrorCode {
  ORDER_NOT_FOUND("ORDER_NOT_FOUND"),
  ORDER_STORE_MISMATCH("ORDER_STORE_MISMATCH"),
  ORDER_INVALID_TRANSITION("ORDER_INVALID_TRANSITION"),
  ORDER_ACCESS_DENIED("ORDER_ACCESS_DENIED"),
  ORDER_IDEMPOTENCY_CONFLICT("ORDER_IDEMPOTENCY_CONFLICT"),
  ACTOR_REQUIRED("ORDER_ACTOR_REQUIRED");

  private final String code;

  OrderErrorCode(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
