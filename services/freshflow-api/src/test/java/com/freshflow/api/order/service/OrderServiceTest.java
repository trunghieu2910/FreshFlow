package com.freshflow.api.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.freshflow.api.catalog.service.CatalogAccessService;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.order.dto.response.MerchantDashboardSummaryDto;
import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.exception.OrderNotFoundException;
import com.freshflow.api.order.exception.OrderRuleViolationException;
import com.freshflow.api.order.mapper.OrderDtoMapper;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private CatalogAccessService catalogAccessService;
  @Mock private OrderRepository orderRepository;
  @Mock private CatalogService catalogService;
  @Spy private OrderDtoMapper orderDtoMapper = new OrderDtoMapper();

  @InjectMocks private OrderService orderService;

  private final Long storeId = 1L;
  private final Long actorUserId = 10L;

  @Test
  @DisplayName("getMerchantDashboardSummary aggregates counts and revenue")
  void getMerchantDashboardSummary_success() {
    when(catalogService.countActiveProductsByStore(storeId)).thenReturn(12L);
    when(catalogService.countTotalProductsByStore(storeId)).thenReturn(15L);
    when(orderRepository.countByStore_IdAndStatus(storeId, "AWAITING_MERCHANT_CONFIRMATION"))
        .thenReturn(3L);
    when(orderRepository.countByStore_IdAndCreatedAtGreaterThanEqual(
            eq(storeId), any(OffsetDateTime.class)))
        .thenReturn(5L);
    when(orderRepository.calculateRevenueSince(eq(storeId), any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal("250000.00"));

    MerchantDashboardSummaryDto summary =
        orderService.getMerchantDashboardSummary(storeId, actorUserId);

    verify(catalogAccessService).requireOwnedStore(storeId, actorUserId);
    assertThat(summary.activeProductsCount()).isEqualTo(12L);
    assertThat(summary.totalProductsCount()).isEqualTo(15L);
    assertThat(summary.pendingOrdersCount()).isEqualTo(3L);
    assertThat(summary.todayOrdersCount()).isEqualTo(5L);
    assertThat(summary.todayRevenue()).isEqualByComparingTo("250000.00");
    assertThat(summary.operationalStatus()).isEqualTo("NORMAL");
    assertThat(summary.avgPreparationMinutes()).isEqualTo(8);
  }

  @Test
  @DisplayName("getMerchantOrders queries with status filter")
  void getMerchantOrders_withStatusFilter() {
    Order order = new Order();
    order.setId(101L);
    order.setOrderNumber("ORD-101");
    order.setItems(List.of());

    Pageable pageable = PageRequest.of(0, 10);
    when(orderRepository.findAllByStore_IdAndStatusOrderByCreatedAtDesc(
            storeId, "PROCESSING", pageable))
        .thenReturn(new PageImpl<>(List.of(order)));

    Page<MerchantOrderSummaryDto> page =
        orderService.getMerchantOrders(storeId, actorUserId, "PROCESSING", pageable);

    verify(catalogAccessService).requireOwnedStore(storeId, actorUserId);
    assertThat(page.getTotalElements()).isEqualTo(1L);
    assertThat(page.getContent().get(0).id()).isEqualTo(101L);
  }

  @Test
  @DisplayName("getMerchantOrders queries all orders when status is null or ALL")
  void getMerchantOrders_withoutStatusFilter() {
    Order order = new Order();
    order.setId(102L);
    order.setOrderNumber("ORD-102");
    order.setItems(List.of());

    Pageable pageable = PageRequest.of(0, 10);
    when(orderRepository.findAllByStore_IdOrderByCreatedAtDesc(storeId, pageable))
        .thenReturn(new PageImpl<>(List.of(order)));

    Page<MerchantOrderSummaryDto> page =
        orderService.getMerchantOrders(storeId, actorUserId, "ALL", pageable);

    verify(catalogAccessService).requireOwnedStore(storeId, actorUserId);
    assertThat(page.getTotalElements()).isEqualTo(1L);
    assertThat(page.getContent().get(0).id()).isEqualTo(102L);
  }

  @Test
  @DisplayName("getMerchantOrderDetail throws OrderNotFoundException if order does not exist")
  void getMerchantOrderDetail_notFound() {
    when(orderRepository.findByIdAndStore_Id(999L, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getMerchantOrderDetail(storeId, 999L, actorUserId))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  @DisplayName("acceptOrder transitions COD order to PROCESSING")
  void acceptOrder_cod_transitionsToProcessing() {
    Order order = new Order();
    order.setId(1L);
    order.setOrderNumber("ORD-1");
    order.setStatus("AWAITING_MERCHANT_CONFIRMATION");
    order.setPaymentMethod("COD");
    order.setItems(List.of());

    when(orderRepository.findByIdAndStore_Id(1L, storeId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    MerchantOrderDetailDto detail = orderService.acceptOrder(storeId, 1L, actorUserId);

    assertThat(detail.merchantAcceptanceStatus()).isEqualTo("ACCEPTED");
    assertThat(detail.status()).isEqualTo("PROCESSING");
    verify(orderRepository).save(order);
  }

  @Test
  @DisplayName("acceptOrder transitions ONLINE_MOCK order to AWAITING_PAYMENT")
  void acceptOrder_onlineMock_transitionsToAwaitingPayment() {
    Order order = new Order();
    order.setId(2L);
    order.setOrderNumber("ORD-2");
    order.setStatus("AWAITING_MERCHANT_CONFIRMATION");
    order.setPaymentMethod("ONLINE_MOCK");
    order.setItems(List.of());

    when(orderRepository.findByIdAndStore_Id(2L, storeId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    MerchantOrderDetailDto detail = orderService.acceptOrder(storeId, 2L, actorUserId);

    assertThat(detail.merchantAcceptanceStatus()).isEqualTo("ACCEPTED");
    assertThat(detail.status()).isEqualTo("AWAITING_PAYMENT");
  }

  @Test
  @DisplayName("acceptOrder throws exception if order is not AWAITING_MERCHANT_CONFIRMATION")
  void acceptOrder_invalidStatus_throwsException() {
    Order order = new Order();
    order.setId(3L);
    order.setStatus("PROCESSING");

    when(orderRepository.findByIdAndStore_Id(3L, storeId)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.acceptOrder(storeId, 3L, actorUserId))
        .isInstanceOf(OrderRuleViolationException.class)
        .hasMessageContaining("cannot accept");
  }

  @Test
  @DisplayName("rejectOrder cancels order with reason")
  void rejectOrder_success() {
    Order order = new Order();
    order.setId(4L);
    order.setOrderNumber("ORD-4");
    order.setStatus("AWAITING_MERCHANT_CONFIRMATION");
    order.setItems(List.of());

    when(orderRepository.findByIdAndStore_Id(4L, storeId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    MerchantOrderDetailDto detail =
        orderService.rejectOrder(storeId, 4L, "Hết nguyên liệu", actorUserId);

    assertThat(detail.merchantAcceptanceStatus()).isEqualTo("REJECTED");
    assertThat(detail.status()).isEqualTo("CANCELLED");
    assertThat(detail.cancelReason()).isEqualTo("Hết nguyên liệu");
  }

  @Test
  @DisplayName("rejectOrder on terminal status throws OrderRuleViolationException")
  void rejectOrder_terminalStatus_throwsException() {
    Order order = new Order();
    order.setId(5L);
    order.setStatus("COMPLETED");

    when(orderRepository.findByIdAndStore_Id(5L, storeId)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.rejectOrder(storeId, 5L, "Reason", actorUserId))
        .isInstanceOf(OrderRuleViolationException.class)
        .hasMessageContaining("terminal status");
  }

  @Test
  @DisplayName("startPreparing transitions PENDING order to PROCESSING")
  void startPreparing_success() {
    Order order = new Order();
    order.setId(6L);
    order.setOrderNumber("ORD-6");
    order.setStatus("PENDING");
    order.setItems(List.of());

    when(orderRepository.findByIdAndStore_Id(6L, storeId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    MerchantOrderDetailDto detail = orderService.startPreparing(storeId, 6L, actorUserId);

    assertThat(detail.status()).isEqualTo("PROCESSING");
  }

  @Test
  @DisplayName("dispatchOrder transitions PROCESSING order to SHIPPING")
  void dispatchOrder_success() {
    Order order = new Order();
    order.setId(7L);
    order.setOrderNumber("ORD-7");
    order.setStatus("PROCESSING");
    order.setItems(List.of());

    when(orderRepository.findByIdAndStore_Id(7L, storeId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    MerchantOrderDetailDto detail = orderService.dispatchOrder(storeId, 7L, actorUserId);

    assertThat(detail.status()).isEqualTo("SHIPPING");
  }

  @Test
  @DisplayName("dispatchOrder when not PROCESSING throws exception")
  void dispatchOrder_notProcessing_throwsException() {
    Order order = new Order();
    order.setId(8L);
    order.setStatus("PENDING");

    when(orderRepository.findByIdAndStore_Id(8L, storeId)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.dispatchOrder(storeId, 8L, actorUserId))
        .isInstanceOf(OrderRuleViolationException.class)
        .hasMessageContaining("Cannot dispatch order");
  }
}
