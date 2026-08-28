package com.freshflow.api.catalog.api.mapper;

import com.freshflow.api.catalog.api.request.CreateProductRequest;
import com.freshflow.api.catalog.api.request.CreateProductVariantRequest;
import com.freshflow.api.catalog.api.request.UpdateProductRequest;
import com.freshflow.api.catalog.api.request.UpdateProductVariantRequest;
import com.freshflow.api.catalog.application.command.CreateProductCommand;
import com.freshflow.api.catalog.application.command.CreateProductVariantCommand;
import com.freshflow.api.catalog.application.command.UpdateProductCommand;
import com.freshflow.api.catalog.application.command.UpdateProductVariantCommand;

public class CatalogRequestMapper {
  public CreateProductCommand toCreateProductCommand(Long storeId, CreateProductRequest request) {
    return new CreateProductCommand(
        storeId,
        request.storeCategoryId(),
        request.name(),
        request.description(),
        request.imageUrl(),
        request.active());
  }

  public UpdateProductCommand toUpdateProductCommand(UpdateProductRequest request) {
    return new UpdateProductCommand(
        request.name(), request.description(), request.imageUrl(), request.active());
  }

  public CreateProductVariantCommand toCreateVariantCommand(CreateProductVariantRequest request) {
    return new CreateProductVariantCommand(
        request.name(),
        request.size(),
        request.price(),
        request.inventoryMode(),
        request.autoAcceptOverride(),
        request.maxQuantityPerOrder(),
        request.dailyCapacityDefault(),
        request.available());
  }

  public UpdateProductVariantCommand toUpdateVariantCommand(UpdateProductVariantRequest request) {
    return new UpdateProductVariantCommand(
        request.name(),
        request.size(),
        request.price(),
        request.inventoryMode(),
        request.autoAcceptOverride(),
        request.maxQuantityPerOrder(),
        request.dailyCapacityDefault(),
        request.available());
  }
}
