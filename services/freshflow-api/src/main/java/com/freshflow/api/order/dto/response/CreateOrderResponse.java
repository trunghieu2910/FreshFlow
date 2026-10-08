package com.freshflow.api.order.dto.response;

import com.freshflow.api.order.model.Order;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderResponse(
    Long id,
    String orderNumber,
    String status,
    String merchantAcceptanceStatus,
    String paymentMethod,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    Address address,
    List<Item> items) {
  public record Address(
      String recipientName,
      String phone,
      String addressLine,
      String ward,
      String district,
      String province) {}

  public record Item(
      Long productId,
      Long variantId,
      String productName,
      String variantName,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal) {}

  public static CreateOrderResponse from(Order order) {
    return new CreateOrderResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getMerchantAcceptanceStatus(),
        order.getPaymentMethod(),
        order.getSubtotal(),
        order.getDeliveryFee(),
        order.getDiscountAmount(),
        order.getTotalAmount(),
        new Address(
            order.getRecipientNameSnapshot(),
            order.getRecipientPhoneSnapshot(),
            order.getAddressLineSnapshot(),
            order.getWardSnapshot(),
            order.getDistrictSnapshot(),
            order.getProvinceSnapshot()),
        order.getItems().stream()
            .map(
                item ->
                    new Item(
                        item.getProduct().getId(),
                        item.getProductVariant().getId(),
                        item.getProductNameSnapshot(),
                        item.getVariantNameSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getLineTotal()))
            .toList());
  }
}
