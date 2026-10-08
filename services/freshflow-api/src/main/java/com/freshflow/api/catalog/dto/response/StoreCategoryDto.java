package com.freshflow.api.catalog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Public category assignment under which a product is sold by a store. */
@Schema(description = "Active category assignment for a store catalog product")
public record StoreCategoryDto(
    @Schema(description = "Store-category assignment identifier", example = "12") Long id,
    @Schema(description = "Canonical category identifier", example = "3") Long categoryId,
    @Schema(description = "Category name", example = "Milk tea") String name,
    @Schema(description = "Display position in the store catalog", example = "1") int displayOrder) {}
