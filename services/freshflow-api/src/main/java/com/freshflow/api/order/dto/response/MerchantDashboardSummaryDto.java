package com.freshflow.api.order.dto.response;

import java.math.BigDecimal;

public record MerchantDashboardSummaryDto(
    long activeProductsCount,
    long totalProductsCount,
    long pendingOrdersCount,
    long todayOrdersCount,
    BigDecimal todayRevenue,
    String operationalStatus,
    int avgPreparationMinutes) {}
