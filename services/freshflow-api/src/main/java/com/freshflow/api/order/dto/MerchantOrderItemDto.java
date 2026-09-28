package com.freshflow.api.order.dto;

import java.math.BigDecimal;

public record MerchantOrderItemDto(
    Long id,
    String productName,
    String variantName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal) {}
