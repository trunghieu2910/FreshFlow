package com.freshflow.api.catalog.api;

import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.api.mapper.CatalogDtoMapper;
import com.freshflow.api.catalog.api.mapper.CatalogRequestMapper;
import com.freshflow.api.catalog.api.request.CreateProductRequest;
import com.freshflow.api.catalog.api.request.CreateProductVariantRequest;
import com.freshflow.api.catalog.api.request.UpdateProductRequest;
import com.freshflow.api.catalog.api.request.UpdateProductVariantRequest;
import com.freshflow.api.catalog.application.CatalogService;
import com.freshflow.api.catalog.application.CatalogVariantService;
import com.freshflow.api.catalog.application.exception.CatalogAccessService;
import com.freshflow.api.catalog.application.query.ProductFilterCriteria;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for the Catalog module.
 *
 * <p>Merchant endpoints are scoped under {@code /api/v1/merchant/stores/{storeId}} and require
 * {@code X-User-Id} header for ownership verification. Public catalog endpoints are scoped under
 * {@code /api/v1/stores}.
 *
 * <p>Authentication and full RBAC will be enforced by Spring Security once the auth module is
 * added. Until then, {@code X-User-Id} is used as a lightweight ownership check.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Endpoints for managing product catalog and variants")
public class CatalogController {

  private final CatalogService catalogService;
  private final CatalogVariantService variantService;
  private final CatalogAccessService accessService;
  private final CatalogDtoMapper dtoMapper;
  private final CatalogRequestMapper requestMapper;

  public CatalogController(
      CatalogService catalogService,
      CatalogVariantService variantService,
      CatalogAccessService accessService,
      CatalogDtoMapper dtoMapper,
      CatalogRequestMapper requestMapper) {
    this.catalogService = catalogService;
    this.variantService = variantService;
    this.accessService = accessService;
    this.dtoMapper = dtoMapper;
    this.requestMapper = requestMapper;
  }

  // -------------------------------------------------------------------------
  // Public -- store list / product catalog
  // -------------------------------------------------------------------------
  // get list of all stores
  @GetMapping("/stores")
  public ResponseEntity<List<?>> listStores() {
    return ResponseEntity.ok(catalogService.listStores());
  }

  // get list of all Prodducts in menu of store
  @GetMapping("/stores/{storeId}/products")
  public ResponseEntity<Page<ProductCatalogDto>> listProducts(
      @PathVariable Long storeId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long storeCategoryId,
      @RequestParam(required = false) String size,
      @RequestParam(required = false) String variantSize,
      @RequestParam(required = false) InventoryMode inventoryMode,
      @RequestParam(required = false) Boolean availableOnly,
      @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
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

  // get Product details
  @GetMapping("/stores/{storeId}/products/{productId}")
  public ResponseEntity<ProductCatalogDto> getProduct(
      @PathVariable Long storeId, @PathVariable Long productId) {
    Product product = catalogService.getProduct(productId);
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  // -------------------------------------------------------------------------
  // Merchant -- product management
  // -------------------------------------------------------------------------

  @PostMapping("/merchant/stores/{storeId}/products")
  public ResponseEntity<ProductCatalogDto> createProduct(
      @PathVariable Long storeId,
      @RequestHeader("X-User-Id") Long actorUserId,
      @Valid @RequestBody CreateProductRequest request) {
    Product product =
        catalogService.createProduct(requestMapper.toCreateProductCommand(storeId, request));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(product.getId())
            .toUri();
    return ResponseEntity.created(location).body(dtoMapper.toProductDto(product));
  }

  @PatchMapping("/merchant/stores/{storeId}/products/{productId}")
  public ResponseEntity<ProductCatalogDto> updateProduct(
      @PathVariable Long storeId,
      @PathVariable Long productId,
      @RequestHeader("X-User-Id") Long actorUserId,
      @Valid @RequestBody UpdateProductRequest request) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    Product product =
        catalogService.updateProduct(productId, requestMapper.toUpdateProductCommand(request));
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  @DeleteMapping("/merchant/stores/{storeId}/products/{productId}")
  public ResponseEntity<Void> deleteProduct(
      @PathVariable Long storeId,
      @PathVariable Long productId,
      @RequestHeader("X-User-Id") Long actorUserId) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    catalogService.deleteProduct(productId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Merchant -- product variant management
  // -------------------------------------------------------------------------

  @GetMapping("/merchant/stores/{storeId}/products/{productId}/variants")
  public ResponseEntity<List<ProductVariantDto>> listVariants(
      @PathVariable Long storeId, @PathVariable Long productId) {
    return ResponseEntity.ok(variantService.list(storeId, productId));
  }

  @GetMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<ProductVariantDto> getVariant(
      @PathVariable Long storeId, @PathVariable Long productId, @PathVariable Long variantId) {
    return ResponseEntity.ok(variantService.get(storeId, productId, variantId));
  }

  @PostMapping("/merchant/stores/{storeId}/products/{productId}/variants")
  public ResponseEntity<ProductVariantDto> createVariant(
      @PathVariable Long storeId,
      @PathVariable Long productId,
      @RequestHeader("X-User-Id") Long actorUserId,
      @Valid @RequestBody CreateProductVariantRequest request) {
    ProductVariantDto dto =
        variantService.create(
            storeId, productId, actorUserId, requestMapper.toCreateVariantCommand(request));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(dto.id())
            .toUri();
    return ResponseEntity.created(location).body(dto);
  }

  @PatchMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<ProductVariantDto> updateVariant(
      @PathVariable Long storeId,
      @PathVariable Long productId,
      @PathVariable Long variantId,
      @RequestHeader("X-User-Id") Long actorUserId,
      @Valid @RequestBody UpdateProductVariantRequest request) {
    ProductVariantDto dto =
        variantService.update(
            storeId,
            productId,
            variantId,
            actorUserId,
            requestMapper.toUpdateVariantCommand(request));
    return ResponseEntity.ok(dto);
  }

  @DeleteMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<Void> deleteVariant(
      @PathVariable Long storeId,
      @PathVariable Long productId,
      @PathVariable Long variantId,
      @RequestHeader("X-User-Id") Long actorUserId) {
    variantService.delete(storeId, productId, variantId, actorUserId);
    return ResponseEntity.noContent().build();
  }
}
