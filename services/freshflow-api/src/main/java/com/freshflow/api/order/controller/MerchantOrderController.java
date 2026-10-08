package com.freshflow.api.order.controller;

import com.freshflow.api.order.dto.request.RejectOrderRequest;
import com.freshflow.api.order.dto.response.MerchantDashboardSummaryDto;
import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.dto.response.OrderReadDto;
import com.freshflow.api.order.service.OrderReadService;
import com.freshflow.api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant/stores/{storeId}")
@RequiredArgsConstructor
public class MerchantOrderController {

  private final OrderService orderService;
  private final OrderReadService orderReadService;

  @GetMapping("/dashboard/summary")
  public ResponseEntity<MerchantDashboardSummaryDto> getDashboardSummary(
      @PathVariable Long storeId,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    MerchantDashboardSummaryDto summary =
        orderService.getMerchantDashboardSummary(storeId, actorUserId);
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/orders")
  public ResponseEntity<Page<MerchantOrderSummaryDto>> getOrders(
      @PathVariable Long storeId,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
    Page<MerchantOrderSummaryDto> orders =
        orderService.getMerchantOrders(storeId, actorUserId, status, pageable);
    return ResponseEntity.ok(orders);
  }

  @GetMapping("/orders/{orderId}")
  public ResponseEntity<OrderReadDto> getOrderDetail(
      @PathVariable Long storeId,
      @PathVariable Long orderId,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    OrderReadDto detail = orderReadService.merchantDetail(storeId, orderId, actorUserId);
    return ResponseEntity.ok(detail);
  }

  @PostMapping("/orders/{orderId}/accept")
  public ResponseEntity<MerchantOrderDetailDto> acceptOrder(
      @PathVariable Long storeId,
      @PathVariable Long orderId,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    MerchantOrderDetailDto updated = orderService.acceptOrder(storeId, orderId, actorUserId);
    return ResponseEntity.ok(updated);
  }

  @PostMapping("/orders/{orderId}/reject")
  public ResponseEntity<MerchantOrderDetailDto> rejectOrder(
      @PathVariable Long storeId,
      @PathVariable Long orderId,
      @RequestBody(required = false) RejectOrderRequest request,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    String reason = request != null ? request.reason() : null;
    MerchantOrderDetailDto updated =
        orderService.rejectOrder(storeId, orderId, reason, actorUserId);
    return ResponseEntity.ok(updated);
  }

  @PostMapping("/orders/{orderId}/prepare")
  public ResponseEntity<MerchantOrderDetailDto> prepareOrder(
      @PathVariable Long storeId,
      @PathVariable Long orderId,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    MerchantOrderDetailDto updated = orderService.startPreparing(storeId, orderId, actorUserId);
    return ResponseEntity.ok(updated);
  }

  @PostMapping("/orders/{orderId}/dispatch")
  public ResponseEntity<MerchantOrderDetailDto> dispatchOrder(
      @PathVariable Long storeId,
      @PathVariable Long orderId,
      @RequestHeader(value = "X-User-Id", required = false) Long actorUserId) {
    MerchantOrderDetailDto updated = orderService.dispatchOrder(storeId, orderId, actorUserId);
    return ResponseEntity.ok(updated);
  }
}
