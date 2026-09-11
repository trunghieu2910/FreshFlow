package com.freshflow.api.catalog.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a new catalog product")
public record CreateProductRequest(
    @NotNull @Positive @Schema(description = "ID of the store category to assign this product to", example = "1")
        Long storeCategoryId,
    @NotBlank
        @Size(max = 150)
        @Schema(description = "Product display name", example = "Trà Đào Cam Sả")
        String name,
    @Size(max = 2000)
        @Schema(
            description = "Detailed product description",
            example = "Trà đào thơm ngát kết hợp hương cam sả thanh mát sảng khoái")
        String description,
    @Size(max = 500)
        @Schema(
            description = "Product image URL",
            example = "https://images.freshflow.vn/products/tra-dao.jpg")
        String imageUrl,
    @Schema(
            description = "Whether the product is immediately active (defaults to true if omitted)",
            example = "true",
            defaultValue = "true")
        Boolean active) {}
