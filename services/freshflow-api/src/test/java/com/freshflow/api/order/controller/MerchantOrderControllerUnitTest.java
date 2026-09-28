package com.freshflow.api.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshflow.api.common.api.error.ApiExceptionHandler;
import com.freshflow.api.order.dto.request.RejectOrderRequest;
import com.freshflow.api.order.dto.response.MerchantDashboardSummaryDto;
import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.service.OrderService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class MerchantOrderControllerUnitTest {

  @Mock private OrderService orderService;
  @InjectMocks private MerchantOrderController controller;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  @DisplayName("GET /dashboard/summary returns 200 OK")
  void getDashboardSummary_success() throws Exception {
    MerchantDashboardSummaryDto dto =
        new MerchantDashboardSummaryDto(10, 15, 2, 5, new BigDecimal("100000"), "NORMAL", 8);

    when(orderService.getMerchantDashboardSummary(1L, 2L)).thenReturn(dto);

    mockMvc
        .perform(get("/api/v1/merchant/stores/1/dashboard/summary").header("X-User-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeProductsCount").value(10))
        .andExpect(jsonPath("$.operationalStatus").value("NORMAL"));
  }

  @Test
  @DisplayName("GET /orders returns 200 OK with page")
  void getOrders_success() throws Exception {
    MerchantOrderSummaryDto item =
        new MerchantOrderSummaryDto(
            1L,
            "ORD-001",
            "Nguyen Van A",
            "0901***456",
            "1x Tra dao",
            new BigDecimal("35000"),
            "PROCESSING",
            "Đang pha chế",
            "COD",
            OffsetDateTime.now());

    when(orderService.getMerchantOrders(eq(1L), eq(2L), eq("PROCESSING"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

    mockMvc
        .perform(
            get("/api/v1/merchant/stores/1/orders")
                .header("X-User-Id", 2L)
                .param("status", "PROCESSING")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-001"));
  }

  @Test
  @DisplayName("POST /orders/{orderId}/reject with RejectOrderRequest returns 200 OK")
  void rejectOrder_success() throws Exception {
    RejectOrderRequest request = new RejectOrderRequest("Hết nguyên liệu");

    MerchantOrderDetailDto detail =
        new MerchantOrderDetailDto(
            10L,
            "ORD-010",
            "Nguyen Van A",
            "0901***456",
            "CANCELLED",
            "Đã hủy",
            "COD",
            "REJECTED",
            new BigDecimal("50000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("50000"),
            "Hết nguyên liệu",
            OffsetDateTime.now(),
            null,
            null,
            null,
            OffsetDateTime.now(),
            List.of());

    when(orderService.rejectOrder(1L, 10L, "Hết nguyên liệu", 2L)).thenReturn(detail);

    mockMvc
        .perform(
            post("/api/v1/merchant/stores/1/orders/10/reject")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelReason").value("Hết nguyên liệu"));

    verify(orderService).rejectOrder(1L, 10L, "Hết nguyên liệu", 2L);
  }

  @Test
  @DisplayName("POST /orders/{orderId}/accept returns 200 OK")
  void acceptOrder_success() throws Exception {
    MerchantOrderDetailDto detail =
        new MerchantOrderDetailDto(
            10L,
            "ORD-010",
            "Nguyen Van A",
            "0901***456",
            "PROCESSING",
            "Đang pha chế",
            "COD",
            "ACCEPTED",
            new BigDecimal("50000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("50000"),
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null,
            List.of());

    when(orderService.acceptOrder(1L, 10L, 2L)).thenReturn(detail);

    mockMvc
        .perform(post("/api/v1/merchant/stores/1/orders/10/accept").header("X-User-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.merchantAcceptanceStatus").value("ACCEPTED"));
  }
}
