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
import com.freshflow.api.common.api.error.ApiErrorResponse;
import com.freshflow.api.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
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
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Endpoints for managing store product catalog and variants")
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

  @Operation(
      summary = "List all active stores",
      description = "Retrieves all active merchant stores available for ordering in FreshFlow.")
  @ApiResponse(responseCode = "200", description = "List of active stores retrieved successfully")
  @GetMapping("/stores")
  public ResponseEntity<List<?>> listStores() {
    return ResponseEntity.ok(catalogService.listStores());
  }

  @Operation(
      summary = "Browse store product catalog with pagination, search, and filters",
      description =
          "Queries active products in the store catalog with pagination, sorting, keyword search, "
              + "category filter, variant size (M, L, STANDARD), inventory mode, and availability. "
              + "Made-to-order variants with zero daily capacity return CAPACITY_EXHAUSTED (BR-08). "
              + "Inactive stores, products, or categories are never returned (BR-01).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page of catalog products returned successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid filter or pagination parameters",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Store not found or inactive",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/stores/{storeId}/products")
  public ResponseEntity<Page<ProductCatalogDto>> listProducts(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(
              description =
                  "Search keyword matched against product name, description, and variant SKU",
              example = "tra")
          @RequestParam(required = false)
          String search,
      @Parameter(description = "Filter by specific store category ID", example = "1")
          @RequestParam(required = false)
          Long storeCategoryId,
      @Parameter(
              description =
                  "Filter by variant size ('M', 'L', 'STANDARD'). When numeric (e.g. '10'), Spring Data binds it as page size.",
              example = "M")
          @RequestParam(required = false)
          String size,
      @Parameter(
              description =
                  "Explicit variant size filter ('M', 'L', 'STANDARD') without size/page conflict",
              example = "M")
          @RequestParam(required = false)
          String variantSize,
      @Parameter(description = "Filter by inventory management mode", example = "MADE_TO_ORDER")
          @RequestParam(required = false)
          InventoryMode inventoryMode,
      @Parameter(
              description =
                  "When true, returns only products that currently have at least one available variant",
              example = "true")
          @RequestParam(required = false)
          Boolean availableOnly,
      @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
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

  @Operation(
      summary = "Get product details by ID",
      description =
          "Retrieves active product details and its variants by product ID within a store.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Product or store not found or inactive",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/stores/{storeId}/products/{productId}")
  public ResponseEntity<ProductCatalogDto> getProduct(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId) {
    Product product = catalogService.getProduct(productId);
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  // -------------------------------------------------------------------------
  // Merchant -- product management
  // -------------------------------------------------------------------------

  @Operation(
      summary = "Create a new product (Merchant)",
      description =
          "Creates a new product within merchant's store. Requires store ownership verification via X-User-Id header.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Product created successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed for request payload",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Store or category not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @PostMapping("/merchant/stores/{storeId}/products")
  public ResponseEntity<ProductCatalogDto> createProduct(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
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

  @Operation(
      summary = "Update product details (Merchant)",
      description = "Updates editable fields of a product. Requires store ownership verification.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Product updated successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed for request payload",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Product not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @PatchMapping("/merchant/stores/{storeId}/products/{productId}")
  public ResponseEntity<ProductCatalogDto> updateProduct(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
      @Valid @RequestBody UpdateProductRequest request) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    Product product =
        catalogService.updateProduct(productId, requestMapper.toUpdateProductCommand(request));
    return ResponseEntity.ok(dtoMapper.toProductDto(product));
  }

  @Operation(
      summary = "Delete product (Merchant)",
      description =
          "Soft-deletes a product by marking it inactive. Requires store ownership verification.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Product not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @DeleteMapping("/merchant/stores/{storeId}/products/{productId}")
  public ResponseEntity<Void> deleteProduct(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    catalogService.deleteProduct(productId);
    return ResponseEntity.noContent().build();
  }

  // -------------------------------------------------------------------------
  // Merchant -- product variant management
  // -------------------------------------------------------------------------

  @Operation(
      summary = "List all variants of a product (Merchant)",
      description = "Lists all variants (including inactive) of a specific product.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "List of product variants returned successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Store or product not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/merchant/stores/{storeId}/products/{productId}/variants")
  public ResponseEntity<List<ProductVariantDto>> listVariants(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId) {
    return ResponseEntity.ok(variantService.list(storeId, productId));
  }

  @Operation(
      summary = "Get variant details by ID (Merchant)",
      description = "Retrieves variant information including pricing, mode, and default capacity.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Variant details retrieved successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Variant not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<ProductVariantDto> getVariant(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true) @PathVariable
          Long variantId) {
    return ResponseEntity.ok(variantService.get(storeId, productId, variantId));
  }

  @Operation(
      summary = "Create product variant (Merchant)",
      description =
          "Creates a new variant (size, price, inventory mode) for a product. Requires store ownership.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Variant created successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed for request payload",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Store or product not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Variant conflict (e.g. duplicate SKU)",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @PostMapping("/merchant/stores/{storeId}/products/{productId}/variants")
  public ResponseEntity<ProductVariantDto> createVariant(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
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

  @Operation(
      summary = "Update product variant (Merchant)",
      description = "Updates fields of an existing variant. Requires store ownership verification.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Variant updated successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed for request payload",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Variant not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @PatchMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<ProductVariantDto> updateVariant(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true) @PathVariable
          Long variantId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
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

  @Operation(
      summary = "Delete product variant (Merchant)",
      description =
          "Deletes or deactivates a product variant. Requires store ownership verification.",
      security = @SecurityRequirement(name = OpenApiConfig.MERCHANT_USER_ID_HEADER))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Variant deleted successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "Store access denied for actor user",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Variant not found",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @DeleteMapping("/merchant/stores/{storeId}/products/{productId}/variants/{variantId}")
  public ResponseEntity<Void> deleteVariant(
      @Parameter(description = "Store identifier", example = "1", required = true) @PathVariable
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true) @PathVariable
          Long variantId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId) {
    variantService.delete(storeId, productId, variantId, actorUserId);
    return ResponseEntity.noContent().build();
  }
}
