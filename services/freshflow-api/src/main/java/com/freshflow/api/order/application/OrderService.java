package com.freshflow.api.order.application;

import com.freshflow.api.catalog.application.CatalogAccessService;
import com.freshflow.api.catalog.infrastructure.persistence.ProductRepository;
import com.freshflow.api.order.api.dto.MerchantDashboardSummaryDto;
import com.freshflow.api.order.api.dto.MerchantOrderDetailDto;
import com.freshflow.api.order.api.dto.MerchantOrderItemDto;
import com.freshflow.api.order.api.dto.MerchantOrderSummaryDto;
import com.freshflow.api.order.application.exception.OrderErrorCode;
import com.freshflow.api.order.application.exception.OrderNotFoundException;
import com.freshflow.api.order.application.exception.OrderRuleViolationException;
import com.freshflow.api.order.domain.Order;
import com.freshflow.api.order.infrastructure.persistence.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
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
  private final ProductRepository productRepository;

  public MerchantDashboardSummaryDto getMerchantDashboardSummary(Long storeId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);

    long activeProducts = productRepository.countByStore_IdAndIsActiveTrue(storeId);
    long totalProducts = productRepository.countByStore_Id(storeId);
    long pendingOrders =
        orderRepository.countByStore_IdAndStatus(storeId, "AWAITING_MERCHANT_CONFIRMATION");

    OffsetDateTime startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
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

    return orders.map(this::toSummaryDto);
  }

  public MerchantOrderDetailDto getMerchantOrderDetail(
      Long storeId, Long orderId, Long actorUserId) {
    catalogAccessService.requireOwnedStore(storeId, actorUserId);

    Order order =
        orderRepository
            .findByIdAndStore_Id(orderId, storeId)
            .orElseThrow(
                () -> new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND, "Order", orderId));

    return toDetailDto(order);
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
    return toDetailDto(saved);
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
    return toDetailDto(saved);
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
    return toDetailDto(saved);
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
    return toDetailDto(saved);
  }

  private Order requireOrder(Long orderId, Long storeId) {
    return orderRepository
        .findByIdAndStore_Id(orderId, storeId)
        .orElseThrow(
            () -> new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND, "Order", orderId));
  }

  private MerchantOrderSummaryDto toSummaryDto(Order order) {
    String customerName =
        order.getCustomerUser() != null ? order.getCustomerUser().getFullName() : "Khách hàng ẩn danh";
    String phoneMasked =
        order.getCustomerUser() != null ? maskPhone(order.getCustomerUser().getPhone()) : "0901***456";

    String itemsSummary =
        order.getItems().stream()
            .map(
                item ->
                    item.getQuantity()
                        + "x "
                        + item.getProductNameSnapshot()
                        + " ("
                        + item.getVariantNameSnapshot()
                        + ")")
            .collect(Collectors.joining(", "));

    if (itemsSummary.isBlank()) {
      itemsSummary = "Không có thông tin món";
    }

    return new MerchantOrderSummaryDto(
        order.getId(),
        order.getOrderNumber(),
        customerName,
        phoneMasked,
        itemsSummary,
        order.getTotalAmount(),
        order.getStatus(),
        formatStatusLabel(order.getStatus()),
        order.getPaymentMethod(),
        order.getCreatedAt());
  }

  private MerchantOrderDetailDto toDetailDto(Order order) {
    String customerName =
        order.getCustomerUser() != null ? order.getCustomerUser().getFullName() : "Khách hàng ẩn danh";
    String phoneMasked =
        order.getCustomerUser() != null ? maskPhone(order.getCustomerUser().getPhone()) : "0901***456";

    List<MerchantOrderItemDto> itemDtos =
        order.getItems().stream()
            .map(
                i ->
                    new MerchantOrderItemDto(
                        i.getId(),
                        i.getProductNameSnapshot(),
                        i.getVariantNameSnapshot(),
                        i.getUnitPriceSnapshot(),
                        i.getQuantity(),
                        i.getLineTotal()))
            .toList();

    return new MerchantOrderDetailDto(
        order.getId(),
        order.getOrderNumber(),
        customerName,
        phoneMasked,
        order.getStatus(),
        formatStatusLabel(order.getStatus()),
        order.getPaymentMethod(),
        order.getMerchantAcceptanceStatus(),
        order.getSubtotal(),
        order.getDeliveryFee(),
        order.getDiscountAmount(),
        order.getTotalAmount(),
        order.getCancelReason(),
        order.getCreatedAt(),
        order.getAcceptedAt(),
        order.getProcessingAt(),
        order.getCompletedAt(),
        order.getCancelledAt(),
        itemDtos);
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "0901***456";
    }
    return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
  }

  private String formatStatusLabel(String status) {
    if (status == null) return "Chưa xác định";
    return switch (status) {
      case "AWAITING_MERCHANT_CONFIRMATION" -> "Chờ quán xác nhận";
      case "AWAITING_PAYMENT" -> "Chờ thanh toán online";
      case "PENDING" -> "Chờ chuẩn bị";
      case "PROCESSING" -> "Đang pha chế";
      case "SHIPPING" -> "Đang giao hàng";
      case "DELIVERY_FAILED" -> "Giao hàng thất bại";
      case "DISPUTED" -> "Đang khiếu nại";
      case "COMPLETED" -> "Hoàn tất";
      case "CANCELLED" -> "Đã hủy";
      default -> status;
    };
  }
}
