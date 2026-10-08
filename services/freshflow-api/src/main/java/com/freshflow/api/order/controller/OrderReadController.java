package com.freshflow.api.order.controller;

import com.freshflow.api.order.dto.response.OrderReadDto;
import com.freshflow.api.order.service.OrderReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderReadController {
  private final OrderReadService readService;

  @GetMapping("/api/v1/orders")
  public ResponseEntity<Page<OrderReadDto.HistoryEntry>> customerHistory(
      @RequestHeader("X-User-Id") Long actorId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(readService.customerHistory(actorId, page(page, size)));
  }

  @GetMapping("/api/v1/orders/{orderId}")
  public ResponseEntity<OrderReadDto> customerDetail(
      @PathVariable Long orderId, @RequestHeader("X-User-Id") Long actorId) {
    return ResponseEntity.ok(readService.customerDetail(orderId, actorId));
  }

  @GetMapping("/api/v1/driver/orders")
  public ResponseEntity<Page<OrderReadDto.HistoryEntry>> driverHistory(
      @RequestHeader("X-User-Id") Long actorId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(readService.driverHistory(actorId, page(page, size)));
  }

  @GetMapping("/api/v1/driver/orders/{orderId}")
  public ResponseEntity<OrderReadDto> driverDetail(
      @PathVariable Long orderId, @RequestHeader("X-User-Id") Long actorId) {
    return ResponseEntity.ok(readService.driverDetail(orderId, actorId));
  }

  @GetMapping("/api/v1/merchant/stores/{storeId}/orders/history")
  public ResponseEntity<Page<OrderReadDto.HistoryEntry>> merchantHistory(
      @PathVariable Long storeId,
      @RequestHeader("X-User-Id") Long actorId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(readService.merchantHistory(storeId, actorId, page(page, size)));
  }

  private static PageRequest page(int page, int size) {
    return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
  }
}
