package com.freshflow.api.order.exception;

public class OrderNotFoundException extends RuntimeException {
  private final OrderErrorCode errorCode;

  public OrderNotFoundException(OrderErrorCode errorCode, String resourceName, Long id) {
    super(resourceName + " was not found: " + id);
    this.errorCode = errorCode;
  }

  public String getCode() {
    return errorCode.code();
  }

  public OrderErrorCode getErrorCode() {
    return errorCode;
  }
}
