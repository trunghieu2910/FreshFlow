package com.freshflow.api.catalog.application;

import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.api.mapper.CatalogDtoMapper;
import com.freshflow.api.catalog.application.command.CreateProductVariantCommand;
import com.freshflow.api.catalog.application.command.UpdateProductVariantCommand;
import com.freshflow.api.catalog.application.exception.CatalogAccessService;
import com.freshflow.api.catalog.application.exception.CatalogErrorCode;
import com.freshflow.api.catalog.application.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.application.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import com.freshflow.api.catalog.infrastructure.persistence.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogVariantService {
  private final CatalogAccessService accessService;
  private final ProductVariantRepository variantRepository;
  private final CatalogDtoMapper mapper;

  public CatalogVariantService(
      CatalogAccessService accessService,
      ProductVariantRepository variantRepository,
      CatalogDtoMapper mapper) {
    this.accessService = accessService;
    this.variantRepository = variantRepository;
    this.mapper = mapper;
  }

  @Transactional
  public ProductVariantDto create(
      Long storeId, Long productId, Long actorUserId, CreateProductVariantCommand command) {
    Product product = accessService.requireOwnedProduct(storeId, productId, actorUserId);
    validateCreateCommand(command);
    validateVariantConvention(command.name(), command.size());
    ensureUnique(productId, command.name(), null);

    OffsetDateTime now = now();
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setName(command.name().trim());
    variant.setSize(normalizeNullable(command.size()));
    variant.setPrice(command.price());
    variant.setInventoryMode(command.inventoryMode());
    variant.setAutoAcceptOverride(command.autoAcceptOverride());
    variant.setMaxQuantityPerOrder(command.maxQuantityPerOrder());
    variant.setDailyCapacityDefault(command.dailyCapacityDefault());
    variant.setIsAvailable(command.available() == null ? Boolean.TRUE : command.available());
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
      UpdateProductVariantCommand command) {
    accessService.requireOwnedProduct(storeId, productId, actorUserId);
    ProductVariant variant = requireVariant(productId, variantId);
    validateUpdateCommand(command);

    String name = command.name() == null ? variant.getName() : command.name().trim();
    String size = command.size() == null ? variant.getSize() : normalizeNullable(command.size());
    validateVariantConvention(name, size);
    if (command.name() != null) {
      ensureUnique(productId, name, variantId);
      variant.setName(name);
    }
    if (command.size() != null) {
      variant.setSize(size);
    }
    if (command.price() != null) {
      variant.setPrice(command.price());
    }
    if (command.inventoryMode() != null) {
      variant.setInventoryMode(command.inventoryMode());
    }
    if (command.autoAcceptOverride() != null) {
      variant.setAutoAcceptOverride(command.autoAcceptOverride());
    }
    if (command.maxQuantityPerOrder() != null) {
      variant.setMaxQuantityPerOrder(command.maxQuantityPerOrder());
    }
    if (command.dailyCapacityDefault() != null) {
      variant.setDailyCapacityDefault(command.dailyCapacityDefault());
    }
    if (command.available() != null) {
      variant.setIsAvailable(command.available());
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

  private static void validateCreateCommand(CreateProductVariantCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("Command must not be null");
    }
    validateValues(
        command.price(),
        command.inventoryMode(),
        command.maxQuantityPerOrder(),
        command.dailyCapacityDefault(),
        true);
  }

  private static void validateUpdateCommand(UpdateProductVariantCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("Command must not be null");
    }
    validateValues(
        command.price(),
        command.inventoryMode(),
        command.maxQuantityPerOrder(),
        command.dailyCapacityDefault(),
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
