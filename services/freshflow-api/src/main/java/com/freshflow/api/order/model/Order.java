package com.freshflow.api.order.model;

import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.catalog.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
