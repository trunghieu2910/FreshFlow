package com.freshflow.api.catalog.api.dto;

import java.util.List;

public record ProductCatalogDto(
    Long id,
    String name,
    String description,
    String imageUrl,
    Boolean active,
    List<ProductVariantDto> variants) {
  public ProductCatalogDto {
    variants = variants == null ? List.of() : List.copyOf(variants);
  }
}
