package com.freshflow.api.order.mapper;

import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderItemDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.model.Order;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrderDtoMapper {

  public MerchantOrderSummaryDto toSummaryDto(Order order) {
    String customerName =
        order.getCustomerUser() != null
            ? order.getCustomerUser().getFullName()
            : "Khách hàng ẩn danh";
    String phoneMasked =
        order.getCustomerUser() != null
            ? maskPhone(order.getCustomerUser().getPhone())
            : "0901***456";

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

  public MerchantOrderDetailDto toDetailDto(Order order) {
    String customerName =
        order.getCustomerUser() != null
            ? order.getCustomerUser().getFullName()
            : "Khách hàng ẩn danh";
    String phoneMasked =
        order.getCustomerUser() != null
            ? maskPhone(order.getCustomerUser().getPhone())
            : "0901***456";

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

  public String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "0901***456";
    }
    return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
  }

  public String formatStatusLabel(String status) {
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
