package com.freshflow.api.order.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MerchantOrderSummaryDto(
    Long id,
    String orderNumber,
    String customerName,
    String customerPhoneMasked,
    String itemsSummary,
    BigDecimal totalAmount,
    String status,
    String statusLabel,
    String paymentMethod,
    OffsetDateTime createdAt) {}
