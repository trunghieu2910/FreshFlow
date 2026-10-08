package com.freshflow.api.order.model;

import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.catalog.model.User;
import com.freshflow.api.common.model.Money;
import com.freshflow.api.order.enums.MerchantAcceptanceStatus;
import com.freshflow.api.order.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

  /** Builds a new aggregate from server-created item snapshots and computes its monetary totals. */
  public static Order create(
      String orderNumber,
      User customer,
      Store store,
      OrderStatus status,
      String paymentMethod,
      MerchantAcceptanceStatus acceptanceStatus,
      List<OrderItem> items,
      Money deliveryFee,
      Money discountAmount) {
    if (orderNumber == null || orderNumber.isBlank() || orderNumber.trim().length() > 30) {
      throw new IllegalArgumentException("A valid order number is required");
    }
    if (customer == null || customer.getId() == null || store == null || store.getId() == null) {
      throw new IllegalArgumentException("Persisted customer and store are required");
    }
    if (status == null
        || acceptanceStatus == null
        || paymentMethod == null
        || !List.of("ONLINE_MOCK", "CASH_ON_DELIVERY", "BANK_TRANSFER_ON_DELIVERY")
            .contains(paymentMethod)) {
      throw new IllegalArgumentException("Valid order state and payment method are required");
    }
    if (items == null || items.isEmpty() || deliveryFee == null || discountAmount == null) {
      throw new IllegalArgumentException("Items, delivery fee and discount are required");
    }
    Order order = new Order();
    order.orderNumber = orderNumber.trim();
    order.customerUser = customer;
    order.store = store;
    order.status = status.name();
    order.paymentMethod = paymentMethod;
    order.merchantAcceptanceStatus = acceptanceStatus.name();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (OrderItem item : items) {
      if (item == null
          || item.getProductVariant() == null
          || item.getProductVariant().getProduct() == null
          || !store.getId().equals(item.getProductVariant().getProduct().getStore().getId())) {
        throw new IllegalArgumentException("Every item must belong to the order store");
      }
      order.addItem(item);
      subtotal = subtotal.add(item.getLineTotal());
    }
    if (discountAmount.getAmount().compareTo(subtotal) > 0) {
      throw new IllegalArgumentException("Discount cannot exceed subtotal");
    }
    order.subtotal = subtotal;
    order.deliveryFee = deliveryFee.getAmount();
    order.discountAmount = discountAmount.getAmount();
    order.totalAmount = subtotal.subtract(order.discountAmount).add(order.deliveryFee);
    order.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    order.updatedAt = order.createdAt;
    return order;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Size(max = 30)
  @NotNull @Column(name = "order_number", nullable = false, unique = true, length = 30)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_user_id", nullable = false)
  private User customerUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Size(max = 40)
  @NotNull @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Size(max = 40)
  @NotNull @Column(name = "payment_method", nullable = false, length = 40)
  private String paymentMethod;

  @Size(max = 20)
  @NotNull @Column(name = "merchant_acceptance_status", nullable = false, length = 20)
  private String merchantAcceptanceStatus;

  @NotNull @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  @NotNull @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
  private BigDecimal deliveryFee = BigDecimal.ZERO;

  @NotNull @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @NotNull @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Size(max = 80)
  @Column(name = "cancel_reason", length = 80)
  private String cancelReason;

  @NotNull @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "accepted_at")
  private OffsetDateTime acceptedAt;

  @Column(name = "processing_at")
  private OffsetDateTime processingAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @NotNull @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @BatchSize(size = 50)
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  public void addItem(OrderItem item) {
    items.add(item);
    item.setOrder(this);
  }
}
