package com.freshflow.api.order.dto.response;

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
