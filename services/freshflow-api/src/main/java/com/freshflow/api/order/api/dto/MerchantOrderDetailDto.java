package com.freshflow.api.order.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MerchantOrderDetailDto(
    Long id,
    String orderNumber,
    String customerName,
    String customerPhoneMasked,
    String status,
    String statusLabel,
    String paymentMethod,
    String merchantAcceptanceStatus,
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
    List<MerchantOrderItemDto> items) {}
