package com.freshflow.api.catalog.service;

import com.freshflow.api.catalog.dto.CatalogDtoMapper;
import com.freshflow.api.catalog.dto.CreateProductVariantRequest;
import com.freshflow.api.catalog.dto.ProductVariantDto;
import com.freshflow.api.catalog.dto.UpdateProductVariantRequest;
import com.freshflow.api.catalog.model.InventoryMode;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.model.ProductVariant;
import com.freshflow.api.catalog.repository.ProductVariantRepository;
import com.freshflow.api.catalog.service.exception.CatalogErrorCode;
import com.freshflow.api.catalog.service.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.service.exception.CatalogRuleViolationException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogVariantService {
  private final CatalogAccessService accessService;
  private final ProductVariantRepository variantRepository;
  private final CatalogDtoMapper mapper;

  @Transactional
  public ProductVariantDto create(
      Long storeId, Long productId, Long actorUserId, CreateProductVariantRequest request) {
    Product product = accessService.requireOwnedProduct(storeId, productId, actorUserId);
    validateCreateRequest(request);
    validateVariantConvention(request.name(), request.size());
    ensureUnique(productId, request.name(), null);

    OffsetDateTime now = now();
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setName(request.name().trim());
    variant.setSize(normalizeNullable(request.size()));
    variant.setPrice(request.price());
    variant.setInventoryMode(request.inventoryMode());
    variant.setAutoAcceptOverride(request.autoAcceptOverride());
    variant.setMaxQuantityPerOrder(request.maxQuantityPerOrder());
    variant.setDailyCapacityDefault(request.dailyCapacityDefault());
    variant.setIsAvailable(request.available() == null ? Boolean.TRUE : request.available());
    variant.setIsActive(Boolean.TRUE);
    variant.setCreatedAt(now);
    variant.setUpdatedAt(now);
    return mapper.toProductVariantDto(variantRepository.save(variant), null);
  }

  public List<ProductVariantDto> list(Long storeId, Long productId) {
    accessService.requireProductInStore(storeId, productId);
    return variantRepository.findAllByProduct_IdOrderByNameAsc(productId).stream()
        .map(variant -> mapper.toProductVariantDto(variant, null))
        .toList();
  }

  public ProductVariantDto get(Long storeId, Long productId, Long variantId) {
    accessService.requireProductInStore(storeId, productId);
    return mapper.toProductVariantDto(requireVariant(productId, variantId), null);
  }

  @Transactional
  public ProductVariantDto update(
      Long storeId,
      Long productId,
      Long variantId,
      Long actorUserId,
      UpdateProductVariantRequest request) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    ProductVariant variant = requireVariant(productId, variantId);
    validateUpdateRequest(request);

    String name = request.name() == null ? variant.getName() : request.name().trim();
    String size = request.size() == null ? variant.getSize() : normalizeNullable(request.size());
    validateVariantConvention(name, size);
    if (request.name() != null) {
      ensureUnique(productId, name, variantId);
      variant.setName(name);
    }
    if (request.size() != null) {
      variant.setSize(size);
    }
    if (request.price() != null) {
      variant.setPrice(request.price());
    }
    if (request.inventoryMode() != null) {
      variant.setInventoryMode(request.inventoryMode());
    }
    if (request.autoAcceptOverride() != null) {
      variant.setAutoAcceptOverride(request.autoAcceptOverride());
    }
    if (request.maxQuantityPerOrder() != null) {
      variant.setMaxQuantityPerOrder(request.maxQuantityPerOrder());
    }
    if (request.dailyCapacityDefault() != null) {
      variant.setDailyCapacityDefault(request.dailyCapacityDefault());
    }
    if (request.available() != null) {
      variant.setIsAvailable(request.available());
    }
    variant.setUpdatedAt(now());
    return mapper.toProductVariantDto(variantRepository.save(variant), null);
  }

  @Transactional
  public void delete(Long storeId, Long productId, Long variantId, Long actorUserId) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    ProductVariant variant = requireVariant(productId, variantId);
    variant.setIsActive(Boolean.FALSE);
    variant.setUpdatedAt(now());
    variantRepository.save(variant);
  }

  private ProductVariant requireVariant(Long productId, Long variantId) {
    return variantRepository
        .findById(variantId)
        .filter(
            variant ->
                variant.getProduct() != null && productId.equals(variant.getProduct().getId()))
        .orElseThrow(
            () ->
                new CatalogNotFoundException(
                    CatalogErrorCode.VARIANT_NOT_FOUND, "ProductVariant", variantId));
  }

  private void ensureUnique(Long productId, String name, Long variantId) {
    boolean exists =
        variantId == null
            ? variantRepository.existsByProduct_IdAndNameIgnoreCase(productId, name)
            : variantRepository.existsByProduct_IdAndNameIgnoreCaseAndIdNot(
                productId, name, variantId);
    if (exists) {
      throw new CatalogRuleViolationException(
          CatalogErrorCode.VARIANT_DUPLICATE, "A variant with the same name already exists");
    }
  }

  private static void validateCreateRequest(CreateProductVariantRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request must not be null");
    }
    validateValues(
        request.price(),
        request.inventoryMode(),
        request.maxQuantityPerOrder(),
        request.dailyCapacityDefault(),
        true);
  }

  private static void validateUpdateRequest(UpdateProductVariantRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request must not be null");
    }
    validateValues(
        request.price(),
        request.inventoryMode(),
        request.maxQuantityPerOrder(),
        request.dailyCapacityDefault(),
        false);
  }

  private static void validateValues(
      BigDecimal price,
      InventoryMode inventoryMode,
      Integer maxQuantity,
      Integer dailyCapacityDefault,
      boolean requireInventoryMode) {
    if (price != null && price.signum() <= 0) {
      throw new IllegalArgumentException("price must be greater than zero");
    }
    if (requireInventoryMode && inventoryMode == null) {
      throw new IllegalArgumentException("inventoryMode must not be null");
    }
    if (maxQuantity != null && maxQuantity <= 0) {
      throw new IllegalArgumentException("maxQuantityPerOrder must be positive");
    }
    if (dailyCapacityDefault != null && dailyCapacityDefault < 0) {
      throw new IllegalArgumentException("dailyCapacityDefault must not be negative");
    }
  }

  private static void validateVariantConvention(String name, String size) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Variant name must not be blank");
    }
    boolean standard = "STANDARD".equalsIgnoreCase(name.trim());
    if ((standard && size != null && !size.isBlank())
        || (!standard && (size == null || size.isBlank()))) {
      throw new CatalogRuleViolationException(
          CatalogErrorCode.STANDARD_SIZE_INVALID,
          "STANDARD must have null size and sized variants must have a size");
    }
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
