package com.freshflow.api.order.controller;

import com.freshflow.api.order.dto.request.CreateOrderRequest;
import com.freshflow.api.order.dto.response.CreateOrderResponse;
import com.freshflow.api.order.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class CheckoutController {
  private final CheckoutService checkoutService;

  @Operation(summary = "Create an order from server-validated catalog items")
  @PostMapping
  public ResponseEntity<CreateOrderResponse> createOrder(
      @RequestHeader("X-User-Id") Long customerId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request) {
    CreateOrderResponse response = checkoutService.checkout(customerId, idempotencyKey, request);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + response.id())).body(response);
  }
}
