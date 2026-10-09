package com.freshflow.api.catalog.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshflow.api.catalog.dto.request.CreateProductRequest;
import com.freshflow.api.catalog.dto.request.CreateProductVariantRequest;
import com.freshflow.api.catalog.dto.request.ProductFilterCriteria;
import com.freshflow.api.catalog.dto.request.UpdateProductRequest;
import com.freshflow.api.catalog.dto.response.ProductCatalogDto;
import com.freshflow.api.catalog.dto.response.ProductVariantDto;
import com.freshflow.api.catalog.enums.AvailabilityStatus;
import com.freshflow.api.catalog.enums.InventoryMode;
import com.freshflow.api.catalog.exception.CatalogErrorCode;
import com.freshflow.api.catalog.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.mapper.CatalogDtoMapper;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.service.CatalogAccessService;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.catalog.service.CatalogVariantService;
import com.freshflow.api.common.api.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CatalogControllerUnitTest {

  @Mock private CatalogService catalogService;
  @Mock private CatalogVariantService variantService;
  @Mock private CatalogAccessService accessService;
  @Mock private CatalogDtoMapper dtoMapper;

  @InjectMocks private CatalogController catalogController;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(catalogController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/stores should return 200 OK with list of stores")
  void listStores_shouldReturn200OkWithStoreList() throws Exception {
    when(catalogService.listStoreDtos()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/stores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName(
      "GET /api/v1/stores/{storeId}/products should return 200 OK with paginated ProductCatalogDto list")
  void listProducts_shouldReturn200OkWithPagedProducts() throws Exception {
    ProductCatalogDto dto =
        new ProductCatalogDto(
            1L,
            "Trà Sữa Oolong",
            "Trà sữa oolong đậm vị trà tự nhiên",
            "https://images.freshflow.vn/products/oolong-milk-tea.jpg",
            true,
            List.of());

    when(catalogService.listProductsByStore(
            eq(1L), any(ProductFilterCriteria.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

    mockMvc
        .perform(get("/api/v1/stores/1/products?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Trà Sữa Oolong"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/stores/{storeId}/products/{productId} should return 200 OK with ProductCatalogDto")
  void getProduct_shouldReturn200OkWithProductDto() throws Exception {
    Product product = new Product();
    product.setId(10L);
    product.setName("Trà Sữa Oolong");

    ProductCatalogDto dto =
        new ProductCatalogDto(10L, "Trà Sữa Oolong", "Mô tả", "img.jpg", true, List.of());

    when(catalogService.getPublicProduct(1L, 10L)).thenReturn(dto);

    mockMvc
        .perform(get("/api/v1/stores/1/products/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.name").value("Trà Sữa Oolong"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/stores/{storeId}/products/{productId} should return 404 NOT_FOUND when missing")
  void getProduct_whenNotFound_shouldReturn404() throws Exception {
    when(catalogService.getPublicProduct(1L, 999L))
        .thenThrow(
            new CatalogNotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", 999L));

    mockMvc
        .perform(get("/api/v1/stores/1/products/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(CatalogErrorCode.PRODUCT_NOT_FOUND.code()));
  }

  @Test
  @DisplayName(
      "POST /api/v1/merchant/stores/{storeId}/products should return 201 CREATED with Location")
  void createProduct_whenValid_shouldReturn201WithLocation() throws Exception {
    CreateProductRequest request =
        new CreateProductRequest(1L, "Trà Sen Vàng", "Mô tả", "img.jpg", true);

    Product savedProduct = new Product();
    savedProduct.setId(50L);
    savedProduct.setName("Trà Sen Vàng");

    ProductCatalogDto dto =
        new ProductCatalogDto(50L, "Trà Sen Vàng", "Mô tả", "img.jpg", true, List.of());

    when(catalogService.createProduct(eq(1L), any(CreateProductRequest.class)))
        .thenReturn(savedProduct);
    when(dtoMapper.toProductDto(savedProduct)).thenReturn(dto);

    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/50")))
        .andExpect(jsonPath("$.id").value(50));
  }

  @Test
  @DisplayName(
      "POST /api/v1/merchant/stores/{storeId}/products should return 403 FORBIDDEN when access denied")
  void createProduct_whenAccessDenied_shouldReturn403() throws Exception {
    CreateProductRequest request =
        new CreateProductRequest(1L, "Trà Sen Vàng", "Mô tả", "img.jpg", true);

    when(catalogService.createProduct(eq(1L), any(CreateProductRequest.class)))
        .thenThrow(
            new CatalogRuleViolationException(
                CatalogErrorCode.STORE_ACCESS_DENIED, "Access denied"));

    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products")
                .header("X-User-Id", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(CatalogErrorCode.STORE_ACCESS_DENIED.code()));
  }

  @Test
  @DisplayName("PATCH /api/v1/merchant/stores/{storeId}/products/{productId} should return 200 OK")
  void updateProduct_whenValid_shouldReturn200WithDto() throws Exception {
    UpdateProductRequest request = new UpdateProductRequest("Tên mới", null, null, null);

    Product updatedProduct = new Product();
    updatedProduct.setId(10L);
    updatedProduct.setName("Tên mới");

    ProductCatalogDto dto =
        new ProductCatalogDto(10L, "Tên mới", "Mô tả", "img.jpg", true, List.of());

    when(catalogService.updateProduct(eq(10L), any(UpdateProductRequest.class)))
        .thenReturn(updatedProduct);
    when(dtoMapper.toProductDto(updatedProduct)).thenReturn(dto);

    mockMvc
        .perform(
            patch("/api/v1/merchant/stores/1/products/10")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tên mới"));

    verify(accessService).requireOwnedProduct(1L, 10L, 2L);
  }

  @Test
  @DisplayName(
      "DELETE /api/v1/merchant/stores/{storeId}/products/{productId} should return 204 NO_CONTENT")
  void deleteProduct_whenValid_shouldReturn204NoContent() throws Exception {
    Product product = new Product();
    product.setId(10L);
    when(accessService.requireOwnedProduct(1L, 10L, 2L)).thenReturn(product);
    doNothing().when(catalogService).deleteProduct(10L);

    mockMvc
        .perform(delete("/api/v1/merchant/stores/1/products/10").header("X-User-Id", 2L))
        .andExpect(status().isNoContent());

    verify(catalogService).deleteProduct(10L);
  }

  @Test
  @DisplayName(
      "GET /api/v1/merchant/stores/{storeId}/products/{productId}/variants should return 200 OK")
  void listVariants_whenValid_shouldReturn200WithList() throws Exception {
    ProductVariantDto dto =
        new ProductVariantDto(
            1L,
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

    when(variantService.list(1L, 10L)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/v1/merchant/stores/1/products/10/variants"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Size M"));
  }

  @Test
  @DisplayName(
      "POST /api/v1/merchant/stores/{storeId}/products/{productId}/variants should return 409 CONFLICT on duplicate")
  void createVariant_whenDuplicateSku_shouldReturn409Conflict() throws Exception {
    CreateProductVariantRequest request =
        new CreateProductVariantRequest(
            "Size M",
            "M",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(variantService.create(eq(1L), eq(10L), eq(2L), any(CreateProductVariantRequest.class)))
        .thenThrow(
            new CatalogRuleViolationException(
                CatalogErrorCode.VARIANT_DUPLICATE, "Variant duplicate"));

    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products/10/variants")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(CatalogErrorCode.VARIANT_DUPLICATE.code()));
  }

  @Test
  @DisplayName(
      "DELETE /api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId} should return 204 NO_CONTENT")
  void deleteVariant_whenValid_shouldReturn204NoContent() throws Exception {
    doNothing().when(variantService).delete(1L, 10L, 100L, 2L);

    mockMvc
        .perform(
            delete("/api/v1/merchant/stores/1/products/10/variants/100").header("X-User-Id", 2L))
        .andExpect(status().isNoContent());

    verify(variantService).delete(1L, 10L, 100L, 2L);
  }
}
