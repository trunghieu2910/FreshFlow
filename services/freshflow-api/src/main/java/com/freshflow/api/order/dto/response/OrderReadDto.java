package com.freshflow.api.order.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Historical order data is read from immutable order snapshots, never from the current catalog. */
public record OrderReadDto(
    Long id,
    String orderNumber,
    Long storeId,
    String customerName,
    String customerPhoneMasked,
    String status,
    String statusLabel,
    String merchantAcceptanceStatus,
    String paymentMethod,
    String paymentStatus,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    String cancelReason,
    OffsetDateTime createdAt,
    OffsetDateTime acceptedAt,
    OffsetDateTime processingAt,
    OffsetDateTime completedAt,
    OffsetDateTime cancelledAt,
    Address address,
    List<Item> items,
    List<Payment> payments,
    Delivery delivery,
    List<Delivery> deliveryHistory,
    OtpPolicy otp,
    List<Dispute> disputes,
    List<Event> events) {

  public record Address(
      String recipientName,
      String phone,
      String addressLine,
      String ward,
      String district,
      String province) {}

  public record Item(
      Long id,
      Long productId,
      Long variantId,
      String productName,
      String variantName,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal) {}

  public record Payment(
      Long id,
      int attemptNumber,
      String method,
      String status,
      BigDecimal amount,
      OffsetDateTime paidAt,
      OffsetDateTime refundedAt) {}

  public record Delivery(
      Long assignmentId,
      Long driverUserId,
      String driverName,
      String status,
      int attemptNumber,
      OffsetDateTime assignedAt,
      OffsetDateTime dispatchedAt,
      OffsetDateTime deliveredAt,
      String failureReason) {}

  /** A hash-only credential can be verified, but its plaintext cannot be displayed by this API. */
  public record OtpPolicy(boolean required, boolean verificationReady, boolean codeVisible) {}

  public record Dispute(
      Long id,
      String reason,
      String status,
      String customerMessage,
      String merchantResolution,
      OffsetDateTime createdAt,
      OffsetDateTime resolvedAt) {}

  public record Event(
      Long id,
      String eventType,
      String fromStatus,
      String toStatus,
      String actorRole,
      String reason,
      OffsetDateTime createdAt) {}

  public record HistoryEntry(
      Long id,
      String orderNumber,
      Long storeId,
      String status,
      String merchantAcceptanceStatus,
      String paymentMethod,
      String paymentStatus,
      String deliveryStatus,
      String disputeStatus,
      BigDecimal totalAmount,
      OffsetDateTime createdAt) {}
}
