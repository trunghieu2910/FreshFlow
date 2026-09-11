package com.freshflow.api.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshflow.api.catalog.api.dto.AvailabilityStatus;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogVariantServiceTest {

  @Mock private CatalogAccessService accessService;
  @Mock private ProductVariantRepository variantRepository;
  @Mock private CatalogDtoMapper mapper;

  private CatalogVariantService variantService;
  private Product product;

  @BeforeEach
  void setUp() {
    variantService = new CatalogVariantService(accessService, variantRepository, mapper);
    product = new Product();
    product.setId(10L);
    product.setName("Trà Sữa Oolong");
  }

  @Test
  @DisplayName("create should successfully save and return DTO for valid STANDARD variant")
  void create_whenValidStandardVariant_shouldSaveAndReturnDto() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "STANDARD",
            null,
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            100,
            true);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);
    when(variantRepository.existsByProduct_IdAndNameIgnoreCase(10L, "STANDARD")).thenReturn(false);

    ProductVariant savedVariant = new ProductVariant();
    savedVariant.setId(101L);
    savedVariant.setName("STANDARD");
    savedVariant.setSize(null);
    savedVariant.setPrice(new BigDecimal("35000"));
    savedVariant.setInventoryMode(InventoryMode.MADE_TO_ORDER);

    when(variantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

    ProductVariantDto expectedDto =
        new ProductVariantDto(
            101L,
            "STANDARD",
            null,
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            100,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);
    when(mapper.toProductVariantDto(eq(savedVariant), isNull())).thenReturn(expectedDto);

    ProductVariantDto result = variantService.create(1L, 10L, 2L, command);

    assertNotNull(result);
    assertEquals("STANDARD", result.name());
    assertNull(result.size());
    verify(variantRepository).save(any(ProductVariant.class));
  }

  @Test
  @DisplayName("create should successfully save and return DTO for valid sized variant (e.g. M)")
  void create_whenValidSizedVariant_shouldSaveAndReturnDto() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "Size M",
            "M",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            false,
            5,
            50,
            true);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);
    when(variantRepository.existsByProduct_IdAndNameIgnoreCase(10L, "Size M")).thenReturn(false);

    ProductVariant savedVariant = new ProductVariant();
    savedVariant.setId(102L);
    savedVariant.setName("Size M");
    savedVariant.setSize("M");

    when(variantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

    ProductVariantDto expectedDto =
        new ProductVariantDto(
            102L,
            "Size M",
            "M",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            false,
            5,
            50,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);
    when(mapper.toProductVariantDto(eq(savedVariant), isNull())).thenReturn(expectedDto);

    ProductVariantDto result = variantService.create(1L, 10L, 2L, command);

    assertNotNull(result);
    assertEquals("M", result.size());
  }

  @Test
  @DisplayName(
      "create should throw STANDARD_SIZE_INVALID when STANDARD variant has a non-null size")
  void create_whenStandardVariantHasSize_shouldThrowStandardSizeInvalid() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "STANDARD",
            "L",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> variantService.create(1L, 10L, 2L, command));

    assertEquals(CatalogErrorCode.STANDARD_SIZE_INVALID, exception.getErrorCode());
  }

  @Test
  @DisplayName(
      "create should throw STANDARD_SIZE_INVALID when non-STANDARD variant has null/blank size")
  void create_whenNonStandardVariantHasNullSize_shouldThrowStandardSizeInvalid() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "Size M",
            null,
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> variantService.create(1L, 10L, 2L, command));

    assertEquals(CatalogErrorCode.STANDARD_SIZE_INVALID, exception.getErrorCode());
  }

  @Test
  @DisplayName("create should throw IllegalArgumentException when price is zero or negative")
  void create_whenNegativePrice_shouldThrowIllegalArgumentException() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "STANDARD",
            null,
            new BigDecimal("-1000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);

    assertThrows(IllegalArgumentException.class, () -> variantService.create(1L, 10L, 2L, command));
  }

  @Test
  @DisplayName("create should throw VARIANT_DUPLICATE when a variant with same name exists")
  void create_whenDuplicateVariantName_shouldThrowVariantDuplicateException() {
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "STANDARD",
            null,
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);
    when(variantRepository.existsByProduct_IdAndNameIgnoreCase(10L, "STANDARD")).thenReturn(true);

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> variantService.create(1L, 10L, 2L, command));

    assertEquals(CatalogErrorCode.VARIANT_DUPLICATE, exception.getErrorCode());
  }

  @Test
  @DisplayName("list should return mapped variant DTOs for product in store")
  void list_whenValidStoreAndProduct_shouldReturnMappedVariantDtos() {
    ProductVariant v1 = new ProductVariant();
    v1.setId(1L);
    v1.setName("Size M");

    ProductVariant v2 = new ProductVariant();
    v2.setId(2L);
    v2.setName("Size L");

    when(variantRepository.findAllByProduct_IdOrderByNameAsc(10L)).thenReturn(List.of(v1, v2));

    ProductVariantDto dto1 =
        new ProductVariantDto(
            1L,
            "Size M",
            "M",
            new BigDecimal("30000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            50,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);
    ProductVariantDto dto2 =
        new ProductVariantDto(
            2L,
            "Size L",
            "L",
            new BigDecimal("40000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            50,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);

    when(mapper.toProductVariantDto(eq(v1), isNull())).thenReturn(dto1);
    when(mapper.toProductVariantDto(eq(v2), isNull())).thenReturn(dto2);

    List<ProductVariantDto> results = variantService.list(1L, 10L);

    assertEquals(2, results.size());
    verify(accessService).requireProductInStore(1L, 10L);
  }

  @Test
  @DisplayName("get should return mapped variant DTO when variant exists")
  void get_whenVariantExists_shouldReturnMappedVariantDto() {
    ProductVariant variant = new ProductVariant();
    variant.setId(101L);
    variant.setProduct(product);

    when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));

    ProductVariantDto expectedDto =
        new ProductVariantDto(
            101L,
            "Size M",
            "M",
            new BigDecimal("30000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            50,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);
    when(mapper.toProductVariantDto(eq(variant), isNull())).thenReturn(expectedDto);

    ProductVariantDto result = variantService.get(1L, 10L, 101L);

    assertNotNull(result);
    assertEquals(101L, result.id());
    verify(accessService).requireProductInStore(1L, 10L);
  }

  @Test
  @DisplayName("get should throw VARIANT_NOT_FOUND when variant belongs to a different product")
  void get_whenVariantBelongsToDifferentProduct_shouldThrowVariantNotFoundException() {
    Product otherProduct = new Product();
    otherProduct.setId(999L);

    ProductVariant variant = new ProductVariant();
    variant.setId(101L);
    variant.setProduct(otherProduct);

    when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));

    CatalogNotFoundException exception =
        assertThrows(CatalogNotFoundException.class, () -> variantService.get(1L, 10L, 101L));

    assertEquals(CatalogErrorCode.VARIANT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("update should update fields and return updated DTO")
  void update_whenValidCommand_shouldUpdateFieldsAndSave() {
    ProductVariant variant = new ProductVariant();
    variant.setId(101L);
    variant.setProduct(product);
    variant.setName("Size M");
    variant.setSize("M");
    variant.setPrice(new BigDecimal("30000"));

    when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));
    when(variantRepository.save(any(ProductVariant.class))).thenReturn(variant);

    UpdateProductVariantCommand command =
        new UpdateProductVariantCommand(
            null, null, new BigDecimal("35000"), null, null, null, null, null);

    ProductVariantDto updatedDto =
        new ProductVariantDto(
            101L,
            "Size M",
            "M",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            true,
            10,
            50,
            true,
            true,
            AvailabilityStatus.AVAILABLE,
            null);
    when(mapper.toProductVariantDto(eq(variant), isNull())).thenReturn(updatedDto);

    ProductVariantDto result = variantService.update(1L, 10L, 101L, 2L, command);

    assertNotNull(result);
    assertEquals(new BigDecimal("35000"), result.price());
    verify(variantRepository).save(variant);
  }

  @Test
  @DisplayName("update should throw VARIANT_DUPLICATE when changing name to existing variant name")
  void update_whenRenamingToExistingName_shouldThrowVariantDuplicateException() {
    ProductVariant variant = new ProductVariant();
    variant.setId(101L);
    variant.setProduct(product);
    variant.setName("Size M");
    variant.setSize("M");

    when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));
    when(variantRepository.existsByProduct_IdAndNameIgnoreCaseAndIdNot(10L, "Size L", 101L))
        .thenReturn(true);

    UpdateProductVariantCommand command =
        new UpdateProductVariantCommand("Size L", "L", null, null, null, null, null, null);

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class,
            () -> variantService.update(1L, 10L, 101L, 2L, command));

    assertEquals(CatalogErrorCode.VARIANT_DUPLICATE, exception.getErrorCode());
  }

  @Test
  @DisplayName("delete should set isActive to false and save")
  void delete_whenCalled_shouldDeactivateVariantAndSave() {
    ProductVariant variant = new ProductVariant();
    variant.setId(101L);
    variant.setProduct(product);
    variant.setIsActive(Boolean.TRUE);

    when(variantRepository.findById(101L)).thenReturn(Optional.of(variant));

    variantService.delete(1L, 10L, 101L, 2L);

    ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
    verify(variantRepository).save(captor.capture());
    assertFalse(captor.getValue().getIsActive());
  }
}
