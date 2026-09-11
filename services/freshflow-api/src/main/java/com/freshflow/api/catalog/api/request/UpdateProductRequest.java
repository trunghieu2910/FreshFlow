package com.freshflow.api.catalog.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating an existing product (all fields optional)")
public record UpdateProductRequest(
    @Size(max = 150)
        @Schema(description = "Updated product display name", example = "Trà Đào Cam Sả Đặc Biệt")
        String name,
    @Size(max = 2000)
        @Schema(
            description = "Updated product description",
            example = "Công thức mới thơm đậm vị trà và thanh ngọt vị đào")
        String description,
    @Size(max = 500)
        @Schema(
            description = "Updated product image URL",
            example = "https://images.freshflow.vn/products/tra-dao-special.jpg")
        String imageUrl,
    @Schema(description = "Updated active status of the product", example = "true")
        Boolean active) {}
