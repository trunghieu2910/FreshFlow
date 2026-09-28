package com.freshflow.api.catalog.service.exception;

public class CatalogRuleViolationException extends RuntimeException {
  private final CatalogErrorCode errorCode;

  public CatalogRuleViolationException(CatalogErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getCode() {
    return errorCode.code();
  }

  public CatalogErrorCode getErrorCode() {
    return errorCode;
  }
}
