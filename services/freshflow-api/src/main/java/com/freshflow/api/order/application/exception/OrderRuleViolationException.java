package com.freshflow.api.order.application.exception;

public class OrderRuleViolationException extends RuntimeException {
  private final OrderErrorCode errorCode;

  public OrderRuleViolationException(OrderErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getCode() {
    return errorCode.code();
  }

  public OrderErrorCode getErrorCode() {
    return errorCode;
  }
}
