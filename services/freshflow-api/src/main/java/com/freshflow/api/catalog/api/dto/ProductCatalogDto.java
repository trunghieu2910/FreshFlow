package com.freshflow.api.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description = "Product catalog item representation with available variants and daily capacity")
public record ProductCatalogDto(
    @Schema(description = "Unique identifier of the product", example = "1") Long id,
    @Schema(description = "Name of the product", example = "Trà Sữa Oolong") String name,
    @Schema(
            description = "Detailed product description",
            example = "Trà sữa pha từ lá trà oolong nguyên chất kết hợp sữa tươi")
        String description,
    @Schema(
            description = "Product image URL",
            example = "https://images.freshflow.vn/products/oolong-tea.jpg")
        String imageUrl,
    @Schema(
            description = "Whether the product is currently active in the catalog",
            example = "true")
        Boolean active,
    @Schema(description = "List of product variants (sizes, prices, capacity status)")
        List<ProductVariantDto> variants) {
  public ProductCatalogDto {
    variants = variants == null ? List.of() : List.copyOf(variants);
  }
}
