package com.freshflow.api.catalog.application.command;

public record CreateProductCommand(
    Long storeId,
    Long storeCategoryId,
    String name,
    String description,
    String imageUrl,
    Boolean active) {}
