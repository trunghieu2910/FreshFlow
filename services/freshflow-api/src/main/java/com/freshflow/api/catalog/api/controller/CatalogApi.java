package com.freshflow.api.catalog.api.controller;

import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.api.request.CreateProductRequest;
import com.freshflow.api.catalog.api.request.CreateProductVariantRequest;
import com.freshflow.api.catalog.api.request.UpdateProductRequest;
import com.freshflow.api.catalog.api.request.UpdateProductVariantRequest;
import com.freshflow.api.catalog.domain.InventoryMode;
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

/**
 * Contract-first API interface for the Catalog module.
 *
 * <p>Merchant endpoints are scoped under {@code /api/v1/merchant/stores/{storeId}} and require
 * {@code X-User-Id} header for ownership verification. Public catalog endpoints are scoped under
 * {@code /api/v1/stores}.
 */
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Endpoints for managing store product catalog and variants")
public interface CatalogApi {

  // -------------------------------------------------------------------------
  // Public -- store list / product catalog
  // -------------------------------------------------------------------------

  /**
   * Retrieves all active merchant stores available for ordering in FreshFlow.
   *
   * @return List of active stores
   */
  @Operation(
      summary = "List all active stores",
      description = "Retrieves all active merchant stores available for ordering in FreshFlow.")
  @ApiResponse(responseCode = "200", description = "List of active stores retrieved successfully")
  @GetMapping("/stores")
  ResponseEntity<List<?>> listStores();

  /**
   * Queries active products in the store catalog with pagination, sorting, keyword search,
   * category filter, variant size, inventory mode, and availability.
   *
   * @param storeId store identifier
   * @param search keyword matched against name, description, SKU
   * @param storeCategoryId category ID filter
   * @param size size filter
   * @param variantSize explicit variant size filter
   * @param inventoryMode inventory mode filter
   * @param availableOnly availability filter
   * @param pageable pagination and sorting parameters
   * @return page of catalog products
   */
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
  ResponseEntity<Page<ProductCatalogDto>> listProducts(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(
              description =
                  "Search keyword matched against product name, description, and variant SKU",
              example = "tra")
          @RequestParam(name = "search", required = false)
          String search,
      @Parameter(description = "Filter by specific store category ID", example = "1")
          @RequestParam(name = "storeCategoryId", required = false)
          Long storeCategoryId,
      @Parameter(
              description =
                  "Filter by variant size ('M', 'L', 'STANDARD'). When numeric (e.g. '10'), Spring Data binds it as page size.",
              example = "M")
          @RequestParam(name = "size", required = false)
          String size,
      @Parameter(
              description =
                  "Explicit variant size filter ('M', 'L', 'STANDARD') without size/page conflict",
              example = "M")
          @RequestParam(name = "variantSize", required = false)
          String variantSize,
      @Parameter(description = "Filter by inventory management mode", example = "MADE_TO_ORDER")
          @RequestParam(name = "inventoryMode", required = false)
          InventoryMode inventoryMode,
      @Parameter(
              description =
                  "When true, returns only products that currently have at least one available variant",
              example = "true")
          @RequestParam(name = "availableOnly", required = false)
          Boolean availableOnly,
      @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable);

  /**
   * Retrieves active product details and its variants by product ID within a store.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @return product details
   */
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
  ResponseEntity<ProductCatalogDto> getProduct(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId);

  // -------------------------------------------------------------------------
  // Merchant -- product management
  // -------------------------------------------------------------------------

  /**
   * Creates a new product within merchant's store.
   *
   * @param storeId store identifier
   * @param actorUserId user ID of merchant owner
   * @param request creation payload
   * @return created product details
   */
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
  ResponseEntity<ProductCatalogDto> createProduct(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
      @Valid @RequestBody CreateProductRequest request);

  /**
   * Updates editable fields of a product.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param actorUserId user ID of merchant owner
   * @param request update payload
   * @return updated product details
   */
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
  ResponseEntity<ProductCatalogDto> updateProduct(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
      @Valid @RequestBody UpdateProductRequest request);

  /**
   * Soft-deletes a product by marking it inactive.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param actorUserId user ID of merchant owner
   * @return empty response
   */
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
  ResponseEntity<Void> deleteProduct(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId);

  // -------------------------------------------------------------------------
  // Merchant -- product variant management
  // -------------------------------------------------------------------------

  /**
   * Lists all variants (including inactive) of a specific product.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @return list of product variants
   */
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
  ResponseEntity<List<ProductVariantDto>> listVariants(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId);

  /**
   * Retrieves variant information including pricing, mode, and default capacity.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param variantId variant identifier
   * @return variant details
   */
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
  ResponseEntity<ProductVariantDto> getVariant(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true)
          @PathVariable("variantId")
          Long variantId);

  /**
   * Creates a new variant (size, price, inventory mode) for a product.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param actorUserId user ID of merchant owner
   * @param request variant creation payload
   * @return created variant details
   */
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
  ResponseEntity<ProductVariantDto> createVariant(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
      @Valid @RequestBody CreateProductVariantRequest request);

  /**
   * Updates fields of an existing variant.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param variantId variant identifier
   * @param actorUserId user ID of merchant owner
   * @param request variant update payload
   * @return updated variant details
   */
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
  ResponseEntity<ProductVariantDto> updateVariant(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true)
          @PathVariable("variantId")
          Long variantId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId,
      @Valid @RequestBody UpdateProductVariantRequest request);

  /**
   * Deletes or deactivates a product variant.
   *
   * @param storeId store identifier
   * @param productId product identifier
   * @param variantId variant identifier
   * @param actorUserId user ID of merchant owner
   * @return empty response
   */
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
  ResponseEntity<Void> deleteVariant(
      @Parameter(description = "Store identifier", example = "1", required = true)
          @PathVariable("storeId")
          Long storeId,
      @Parameter(description = "Product identifier", example = "1", required = true)
          @PathVariable("productId")
          Long productId,
      @Parameter(description = "Variant identifier", example = "1", required = true)
          @PathVariable("variantId")
          Long variantId,
      @Parameter(description = "User ID of merchant owner", example = "2", required = true)
          @RequestHeader("X-User-Id")
          Long actorUserId);
}
