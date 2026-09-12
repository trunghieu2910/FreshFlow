package com.freshflow.api.catalog.api.controller;

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
import com.freshflow.api.catalog.api.dto.AvailabilityStatus;
import com.freshflow.api.catalog.api.dto.ProductCatalogDto;
import com.freshflow.api.catalog.api.dto.ProductVariantDto;
import com.freshflow.api.catalog.api.mapper.CatalogDtoMapper;
import com.freshflow.api.catalog.api.mapper.CatalogRequestMapper;
import com.freshflow.api.catalog.api.request.CreateProductRequest;
import com.freshflow.api.catalog.api.request.CreateProductVariantRequest;
import com.freshflow.api.catalog.api.request.UpdateProductRequest;
import com.freshflow.api.catalog.application.CatalogAccessService;
import com.freshflow.api.catalog.application.CatalogService;
import com.freshflow.api.catalog.application.CatalogVariantService;
import com.freshflow.api.catalog.application.command.CreateProductCommand;
import com.freshflow.api.catalog.application.command.CreateProductVariantCommand;
import com.freshflow.api.catalog.application.command.UpdateProductCommand;
import com.freshflow.api.catalog.application.exception.CatalogErrorCode;
import com.freshflow.api.catalog.application.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.application.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.application.query.ProductFilterCriteria;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CatalogControllerUnitTest {

  @Autowired private WebApplicationContext wac;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CatalogService catalogService;
  @MockitoBean private CatalogVariantService variantService;
  @MockitoBean private CatalogAccessService accessService;
  @MockitoBean private CatalogDtoMapper dtoMapper;
  @MockitoBean private CatalogRequestMapper requestMapper;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @DisplayName("GET /api/v1/stores should return 200 OK with list of stores")
  void listStores_shouldReturn200OkWithStoreList() throws Exception {
    when(catalogService.listStores()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/stores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName(
      "GET /api/v1/stores/{storeId}/products should pass filters and return 200 OK with page")
  void listProducts_withFiltersAndPagination_shouldPassCriteriaAndReturn200() throws Exception {
    ProductCatalogDto dto =
        new ProductCatalogDto(1L, "Trà Sữa Oolong", "Mô tả", "img.jpg", true, List.of());
    PageImpl<ProductCatalogDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

    when(catalogService.listProductsByStore(
            eq(1L), any(ProductFilterCriteria.class), any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/api/v1/stores/1/products")
                .param("search", "tra")
                .param("storeCategoryId", "1")
                .param("variantSize", "M")
                .param("inventoryMode", "MADE_TO_ORDER")
                .param("availableOnly", "true")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Trà Sữa Oolong"));
  }

  @Test
  @DisplayName("GET /api/v1/stores/{storeId}/products/{productId} should return 200 OK when exists")
  void getProduct_whenExists_shouldReturn200WithDto() throws Exception {
    Product product = new Product();
    product.setId(10L);
    product.setName("Trà Sữa Oolong");

    ProductCatalogDto dto =
        new ProductCatalogDto(10L, "Trà Sữa Oolong", "Mô tả", "img.jpg", true, List.of());

    when(catalogService.getProduct(10L)).thenReturn(product);
    when(dtoMapper.toProductDto(product)).thenReturn(dto);

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
    when(catalogService.getProduct(999L))
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
    CreateProductCommand command =
        new CreateProductCommand(1L, 1L, "Trà Sen Vàng", "Mô tả", "img.jpg", true);

    Product savedProduct = new Product();
    savedProduct.setId(50L);
    savedProduct.setName("Trà Sen Vàng");

    ProductCatalogDto dto =
        new ProductCatalogDto(50L, "Trà Sen Vàng", "Mô tả", "img.jpg", true, List.of());

    when(requestMapper.toCreateProductCommand(eq(1L), any(CreateProductRequest.class)))
        .thenReturn(command);
    when(catalogService.createProduct(command)).thenReturn(savedProduct);
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
    CreateProductCommand command =
        new CreateProductCommand(1L, 1L, "Trà Sen Vàng", "Mô tả", "img.jpg", true);

    when(requestMapper.toCreateProductCommand(eq(1L), any(CreateProductRequest.class)))
        .thenReturn(command);
    when(catalogService.createProduct(command))
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
    UpdateProductCommand command = new UpdateProductCommand("Tên mới", null, null, null);

    Product updatedProduct = new Product();
    updatedProduct.setId(10L);
    updatedProduct.setName("Tên mới");

    ProductCatalogDto dto =
        new ProductCatalogDto(10L, "Tên mới", "Mô tả", "img.jpg", true, List.of());

    when(requestMapper.toUpdateProductCommand(any(UpdateProductRequest.class))).thenReturn(command);
    when(catalogService.updateProduct(10L, command)).thenReturn(updatedProduct);
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
    CreateProductVariantCommand command =
        new CreateProductVariantCommand(
            "Size M",
            "M",
            new BigDecimal("35000"),
            InventoryMode.MADE_TO_ORDER,
            null,
            null,
            null,
            null);

    when(requestMapper.toCreateVariantCommand(any(CreateProductVariantRequest.class)))
        .thenReturn(command);
    when(variantService.create(eq(1L), eq(10L), eq(2L), any(CreateProductVariantCommand.class)))
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
