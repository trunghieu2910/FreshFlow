package com.freshflow.api.catalog.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotNull @Positive Long storeCategoryId,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 2000) String description,
    @Size(max = 500) String imageUrl,
    Boolean active) {}
