package com.freshflow.api.catalog.application.exception;

public class CatalogNotFoundException extends RuntimeException {
  private final CatalogErrorCode errorCode;

  public CatalogNotFoundException(CatalogErrorCode errorCode, String resourceName, Long id) {
    super(resourceName + " was not found: " + id);
    this.errorCode = errorCode;
  }

  public String getCode() {
    return errorCode.code();
  }

  public CatalogErrorCode getErrorCode() {
    return errorCode;
  }
}
