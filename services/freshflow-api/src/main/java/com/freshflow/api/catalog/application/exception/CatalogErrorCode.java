package com.freshflow.api.catalog.application.exception;

public enum CatalogErrorCode {
  USER_NOT_FOUND("CATALOG_USER_NOT_FOUND"),
  STORE_NOT_FOUND("CATALOG_STORE_NOT_FOUND"),
  CATEGORY_NOT_FOUND("CATALOG_CATEGORY_NOT_FOUND"),
  STORE_CATEGORY_NOT_FOUND("CATALOG_STORE_CATEGORY_NOT_FOUND"),
  PRODUCT_NOT_FOUND("CATALOG_PRODUCT_NOT_FOUND"),
  STORE_CATEGORY_OWNERSHIP_MISMATCH("CATALOG_STORE_CATEGORY_OWNERSHIP_MISMATCH");

  private final String code;

  CatalogErrorCode(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
