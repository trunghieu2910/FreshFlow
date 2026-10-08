package com.freshflow.api.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Checkout command. Prices and totals are intentionally absent. */
public record CreateOrderRequest(
    @NotNull @Positive Long storeId,
    @NotEmpty List<@NotNull @Valid Item> items,
    @NotNull @Valid Address address,
    @NotNull @Pattern(regexp = "ONLINE_MOCK|CASH_ON_DELIVERY|BANK_TRANSFER_ON_DELIVERY")
        String paymentMethod) {

  public record Item(
      @NotNull @Positive Long productId,
      @NotNull @Positive Long variantId,
      @NotNull @Positive Integer quantity) {}

  public record Address(
      @NotBlank @Size(max = 150) String recipientName,
      @NotBlank @Size(max = 30) String phone,
      @NotBlank @Size(max = 255) String addressLine,
      @NotBlank @Size(max = 100) String ward,
      @NotBlank @Size(max = 100) String district,
      @NotBlank @Size(max = 100) String province) {}
}
