package com.freshflow.api.order.domain;

import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_variant_id", nullable = false)
  private ProductVariant productVariant;

  @Size(max = 150)
  @NotNull
  @Column(name = "product_name_snapshot", nullable = false, length = 150)
  private String productNameSnapshot;

  @Size(max = 80)
  @NotNull
  @Column(name = "variant_name_snapshot", nullable = false, length = 80)
  private String variantNameSnapshot;

  @NotNull
  @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPriceSnapshot;

  @NotNull
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @NotNull
  @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineTotal;
}
