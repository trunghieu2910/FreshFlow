package com.freshflow.api.catalog.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogValidationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void invalidProductPayloadReturnsStructuredFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {
              "storeCategoryId": null,
              "name": "",
              "description": "valid description",
              "imageUrl": "valid-image-url"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.path").value("/api/v1/merchant/stores/1/products"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("storeCategoryId")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist())
        .andExpect(jsonPath("$.message", not(containsString("org.springframework"))));
  }

  @Test
  void invalidVariantPriceReturnsFieldError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products/1/variants")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {
              "name": "M",
              "size": "M",
              "price": 0,
              "inventoryMode": "MADE_TO_ORDER",
              "available": true,
              "dailyCapacityDefault": 10
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());
  }

  @Test
  void malformedJsonReturnsSafeBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/products")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"name":
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist())
        .andExpect(jsonPath("$.message").value("Request body is malformed"));
  }
}
