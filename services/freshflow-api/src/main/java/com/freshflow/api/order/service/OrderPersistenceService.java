package com.freshflow.api.order.service;

import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.model.ProductVariant;
import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.common.model.Money;
import com.freshflow.api.order.enums.MerchantAcceptanceStatus;
import com.freshflow.api.order.enums.OrderStatus;
import com.freshflow.api.order.model.AddressSnapshot;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.model.OrderItem;
import com.freshflow.api.order.repository.OrderRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists a complete order using current catalog data within one transaction. */
@Service
@RequiredArgsConstructor
public class OrderPersistenceService {
  private final CatalogService catalogService;
  private final OrderRepository orderRepository;

  public record LineSelection(Long productId, Long variantId, int quantity) {}

  /**
   * Creates the order aggregate from catalog variants. Checkout must reserve capacity or stock in
   * its surrounding transaction before calling this method.
   */
  @Transactional
  public Order create(
      String orderNumber,
      Long customerId,
      Long storeId,
      OrderStatus status,
      String paymentMethod,
      MerchantAcceptanceStatus acceptanceStatus,
      List<LineSelection> selections,
      Money deliveryFee,
      Money discountAmount) {
    return create(
        orderNumber,
        customerId,
        storeId,
        status,
        paymentMethod,
        acceptanceStatus,
        selections,
        deliveryFee,
        discountAmount,
        null);
  }

  @Transactional
  public Order create(
      String orderNumber,
      Long customerId,
      Long storeId,
      OrderStatus status,
      String paymentMethod,
      MerchantAcceptanceStatus acceptanceStatus,
      List<LineSelection> selections,
      Money deliveryFee,
      Money discountAmount,
      AddressSnapshot address) {
    if (selections == null || selections.isEmpty()) {
      throw new IllegalArgumentException("At least one order item is required");
    }
    Store store = catalogService.getStore(storeId);
    if (!"ACTIVE".equals(store.getStatus())) {
      throw new IllegalArgumentException("Store is not active");
    }
    var customer = catalogService.getUser(customerId);
    if (!"ACTIVE".equals(customer.getStatus())) {
      throw new IllegalArgumentException("Customer is not active");
    }
    List<OrderItem> items = new ArrayList<>();
    Set<Long> selectedVariantIds = new HashSet<>();
    for (LineSelection selection : selections) {
      if (selection == null || selection.productId() == null || selection.variantId() == null) {
        throw new IllegalArgumentException("Product and variant IDs are required");
      }
      if (selection.quantity() <= 0 || !selectedVariantIds.add(selection.variantId())) {
        throw new IllegalArgumentException("Each variant must have one positive-quantity line");
      }
      Product product = catalogService.getProduct(selection.productId());
      if (!storeId.equals(product.getStore().getId())
          || !storeId.equals(product.getStoreCategory().getStore().getId())
          || !Boolean.TRUE.equals(product.getIsActive())
          || !Boolean.TRUE.equals(product.getStoreCategory().getIsActive())
          || !Boolean.TRUE.equals(product.getStoreCategory().getCategory().getIsActive())) {
        throw new IllegalArgumentException("Product is not purchasable in this store");
      }
      ProductVariant variant =
          product.getVariants().stream()
              .filter(candidate -> selection.variantId().equals(candidate.getId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Variant is not in the product"));
      if (!Boolean.TRUE.equals(variant.getIsActive())
          || !Boolean.TRUE.equals(variant.getIsAvailable())
          || (variant.getMaxQuantityPerOrder() != null
              && selection.quantity() > variant.getMaxQuantityPerOrder())) {
        throw new IllegalArgumentException("Variant is not purchasable in this quantity");
      }
      items.add(OrderItem.fromCatalog(variant, selection.quantity()));
    }
    Order order =
        Order.create(
            orderNumber,
            customer,
            store,
            status,
            paymentMethod,
            acceptanceStatus,
            items,
            deliveryFee,
            discountAmount);
    if (address != null) {
      order.setDeliveryAddress(address);
    }
    return orderRepository.save(order);
  }
}
