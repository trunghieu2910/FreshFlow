package com.freshflow.api.catalog.controller;

import com.freshflow.api.catalog.dto.CatalogDtoMapper;
import com.freshflow.api.catalog.dto.CreateProductRequest;
import com.freshflow.api.catalog.dto.CreateProductVariantRequest;
import com.freshflow.api.catalog.dto.ProductCatalogDto;
import com.freshflow.api.catalog.dto.ProductFilterCriteria;
import com.freshflow.api.catalog.dto.ProductVariantDto;
import com.freshflow.api.catalog.dto.UpdateProductRequest;
import com.freshflow.api.catalog.dto.UpdateProductVariantRequest;
import com.freshflow.api.catalog.model.InventoryMode;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.service.CatalogAccessService;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.catalog.service.CatalogVariantService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller implementation for {@link CatalogApi}.
 *
 * <p>Implements contract-first OpenAPI interface with business logic, authorization, and service
 * delegation.
 */
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CatalogController implements CatalogApi {

  private final CatalogService catalogService;
  private final CatalogVariantService variantService;
  private final CatalogAccessService accessService;
  private final CatalogDtoMapper dtoMapper;

  // -------------------------------------------------------------------------
  // Public -- store list / product catalog
  // -------------------------------------------------------------------------

  @Override
  public ResponseEntity<List<?>> listStores() {
    return ResponseEntity.ok(catalogService.listStores());
  }

  @Override
  public ResponseEntity<Page<ProductCatalogDto>> listProducts(
      Long storeId,
      String search,
      Long storeCategoryId,
      String size,
      String variantSize,
      InventoryMode inventoryMode,
      Boolean availableOnly,
      Pageable pageable) {
    String effectiveSize = resolveSizeFilter(size, variantSize);
    ProductFilterCriteria criteria =
        ProductFilterCriteria.publicCatalog(
            search, storeCategoryId, effectiveSize, inventoryMode, availableOnly);
    Page<ProductCatalogDto> page = catalogService.listProductsByStore(storeId, criteria, pageable);
    return ResponseEntity.ok(page);
  }

  private static String resolveSizeFilter(String sizeParam, String variantSizeParam) {
    if (variantSizeParam != null && !variantSizeParam.isBlank()) {
      return variantSizeParam.trim();
    }
    if (sizeParam != null && !sizeParam.isBlank() && !sizeParam.trim().matches("\\d+")) {
      return sizeParam.trim();
    }
    return null;
  }

  @Override
  public ResponseEntity<ProductCatalogDto> getProduct(Long storeId, Long productId) {
    Product product = catalogService.getProduct(productId);
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  // -------------------------------------------------------------------------
  // Merchant -- product management
  // -------------------------------------------------------------------------

  @Override
  public ResponseEntity<ProductCatalogDto> createProduct(
      Long storeId, Long actorUserId, CreateProductRequest request) {
    accessService.requireOwnedStore(storeId, actorUserId);
    Product product = catalogService.createProduct(storeId, request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(product.getId())
            .toUri();
    return ResponseEntity.created(location).body(dtoMapper.toProductDto(product));
  }

  @Override
  public ResponseEntity<ProductCatalogDto> updateProduct(
      Long storeId, Long productId, Long actorUserId, UpdateProductRequest request) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    Product product = catalogService.updateProduct(productId, request);
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  @Override
  public ResponseEntity<Void> deleteProduct(Long storeId, Long productId, Long actorUserId) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    catalogService.deleteProduct(productId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Merchant -- product variant management
  // -------------------------------------------------------------------------

  @Override
  public ResponseEntity<List<ProductVariantDto>> listVariants(Long storeId, Long productId) {
    return ResponseEntity.ok(variantService.list(storeId, productId));
  }

  @Override
  public ResponseEntity<ProductVariantDto> getVariant(
      Long storeId, Long productId, Long variantId) {
    return ResponseEntity.ok(variantService.get(storeId, productId, variantId));
  }

  @Override
  public ResponseEntity<ProductVariantDto> createVariant(
      Long storeId, Long productId, Long actorUserId, CreateProductVariantRequest request) {
    ProductVariantDto dto = variantService.create(storeId, productId, actorUserId, request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(dto.id())
            .toUri();
    return ResponseEntity.created(location).body(dto);
  }

  @Override
  public ResponseEntity<ProductVariantDto> updateVariant(
      Long storeId,
      Long productId,
      Long variantId,
      Long actorUserId,
      UpdateProductVariantRequest request) {
    ProductVariantDto dto =
        variantService.update(storeId, productId, variantId, actorUserId, request);
    return ResponseEntity.ok(dto);
  }

  @Override
  public ResponseEntity<Void> deleteVariant(
      Long storeId, Long productId, Long variantId, Long actorUserId) {
    variantService.delete(storeId, productId, variantId, actorUserId);
    return ResponseEntity.noContent().build();
  }
}
