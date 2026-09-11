package com.freshflow.api.catalog.api.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CatalogOpenApiIntegrationTest {

  @Autowired private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @DisplayName("GET /api-docs should return valid OpenAPI 3 specification")
  void apiDocs_shouldReturnOpenApiSpec() throws Exception {
    mockMvc
        .perform(get("/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi", notNullValue()))
        .andExpect(jsonPath("$.info.title").value("FreshFlow MVP REST API"))
        .andExpect(jsonPath("$.info.version").value("v1.0.0"))
        .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}/products']", notNullValue()))
        .andExpect(
            jsonPath("$.paths['/api/v1/merchant/stores/{storeId}/products']", notNullValue()))
        .andExpect(
            jsonPath(
                "$.paths['/api/v1/merchant/stores/{storeId}/products/{productId}/variants']",
                notNullValue()))
        .andExpect(jsonPath("$.components.schemas.ProductCatalogDto", notNullValue()))
        .andExpect(jsonPath("$.components.schemas.ProductVariantDto", notNullValue()))
        .andExpect(jsonPath("$.components.schemas.CreateProductRequest", notNullValue()))
        .andExpect(jsonPath("$.components.schemas.ApiErrorResponse", notNullValue()))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth", notNullValue()))
        .andExpect(jsonPath("$.components.securitySchemes.merchantUserIdAuth", notNullValue()));
  }

  @Test
  @DisplayName("GET /swagger-ui/index.html should redirect to Swagger UI bundle")
  void swaggerUi_shouldBeAccessible() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().is3xxRedirection());
  }
}
