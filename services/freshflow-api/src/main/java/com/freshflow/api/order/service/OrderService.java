package com.freshflow.api.order.service;

import com.freshflow.api.catalog.service.CatalogAccessService;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.order.dto.response.MerchantDashboardSummaryDto;
import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.exception.OrderErrorCode;
import com.freshflow.api.order.exception.OrderNotFoundException;
import com.freshflow.api.order.exception.OrderRuleViolationException;
import com.freshflow.api.order.mapper.OrderDtoMapper;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

  private final CatalogAccessService catalogAccessService;
  private final OrderRepository orderRepository;
  private final CatalogService catalogService;
  private final OrderDtoMapper orderDtoMapper;

  public MerchantDashboardSummaryDto getMerchantDashboardSummary(Long storeId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);

    long activeProducts = catalogService.countActiveProductsByStore(storeId);
    long totalProducts = catalogService.countTotalProductsByStore(storeId);
    long pendingOrders =
        orderRepository.countByStore_IdAndStatus(storeId, "AWAITING_MERCHANT_CONFIRMATION");

    OffsetDateTime startOfToday =
        LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
    long todayOrders =
        orderRepository.countByStore_IdAndCreatedAtGreaterThanEqual(storeId, startOfToday);
    BigDecimal todayRevenue = orderRepository.calculateRevenueSince(storeId, startOfToday);

    return new MerchantDashboardSummaryDto(
        activeProducts,
        totalProducts,
        pendingOrders,
        todayOrders,
        todayRevenue != null ? todayRevenue : BigDecimal.ZERO,
        "NORMAL",
        8);
  }

  public Page<MerchantOrderSummaryDto> getMerchantOrders(
      Long storeId, Long actorUserId, String status, Pageable pageable) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);

    Page<Order> orders;
    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
      orders =
          orderRepository.findAllByStore_IdAndStatusOrderByCreatedAtDesc(
              storeId, status.trim(), pageable);
    } else {
      orders = orderRepository.findAllByStore_IdOrderByCreatedAtDesc(storeId, pageable);
    }

    return orders.map(orderDtoMapper::toSummaryDto);
  }

  public MerchantOrderDetailDto getMerchantOrderDetail(
      Long storeId, Long orderId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);

    Order order =
        orderRepository
            .findByIdAndStore_Id(orderId, storeId)
            .orElseThrow(
                () -> new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND, "Order", orderId));

    return orderDtoMapper.toDetailDto(order);
  }

  @Transactional
  public MerchantOrderDetailDto acceptOrder(Long storeId, Long orderId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);
    Order order = requireOrder(orderId, storeId);

    if (!"AWAITING_MERCHANT_CONFIRMATION".equals(order.getStatus())) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ORDER_INVALID_TRANSITION,
          "Order " + orderId + " is in status " + order.getStatus() + ", cannot accept.");
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    order.setMerchantAcceptanceStatus("ACCEPTED");
    order.setAcceptedAt(now);

    if ("ONLINE_MOCK".equals(order.getPaymentMethod())) {
      order.setStatus("AWAITING_PAYMENT");
    } else {
      order.setStatus("PROCESSING");
      order.setProcessingAt(now);
    }
    order.setUpdatedAt(now);

    Order saved = orderRepository.save(order);
    return orderDtoMapper.toDetailDto(saved);
  }

  @Transactional
  public MerchantOrderDetailDto rejectOrder(
      Long storeId, Long orderId, String reason, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);
    Order order = requireOrder(orderId, storeId);

    if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ORDER_INVALID_TRANSITION,
          "Order " + orderId + " is in terminal status " + order.getStatus() + ", cannot reject.");
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    order.setMerchantAcceptanceStatus("REJECTED");
    order.setStatus("CANCELLED");
    order.setCancelReason(reason != null && !reason.isBlank() ? reason : "MERCHANT_REJECTED");
    order.setCancelledAt(now);
    order.setUpdatedAt(now);

    Order saved = orderRepository.save(order);
    return orderDtoMapper.toDetailDto(saved);
  }

  @Transactional
  public MerchantOrderDetailDto startPreparing(Long storeId, Long orderId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);
    Order order = requireOrder(orderId, storeId);

    if (!"PENDING".equals(order.getStatus())
        && !"AWAITING_MERCHANT_CONFIRMATION".equals(order.getStatus())) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ORDER_INVALID_TRANSITION,
          "Cannot prepare order in status: " + order.getStatus());
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    order.setStatus("PROCESSING");
    order.setProcessingAt(now);
    order.setUpdatedAt(now);

    Order saved = orderRepository.save(order);
    return orderDtoMapper.toDetailDto(saved);
  }

  @Transactional
  public MerchantOrderDetailDto dispatchOrder(Long storeId, Long orderId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);
    Order order = requireOrder(orderId, storeId);

    if (!"PROCESSING".equals(order.getStatus())) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ORDER_INVALID_TRANSITION,
          "Cannot dispatch order not currently in PROCESSING. Current: " + order.getStatus());
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    order.setStatus("SHIPPING");
    order.setUpdatedAt(now);

    Order saved = orderRepository.save(order);
    return orderDtoMapper.toDetailDto(saved);
  }

  private Order requireOrder(Long orderId, Long storeId) {
    return orderRepository
        .findByIdAndStore_Id(orderId, storeId)
        .orElseThrow(
            () -> new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND, "Order", orderId));
  }
}
