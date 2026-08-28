package com.freshflow.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Size(max = 80)
  @NotNull @Column(name = "name", nullable = false, length = 80)
  private String name;

  @Size(max = 30)
  @Column(name = "size", length = 30)
  private String size;

  @NotNull @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(name = "inventory_mode", nullable = false, length = 30)
  private InventoryMode inventoryMode;

  @Column(name = "auto_accept_override")
  private Boolean autoAcceptOverride;

  @Column(name = "max_quantity_per_order")
  private Integer maxQuantityPerOrder;

  @Column(name = "daily_capacity_default")
  private Integer dailyCapacityDefault;

  @NotNull @ColumnDefault("true")
  @Column(name = "is_available", nullable = false)
  private Boolean isAvailable;

  @NotNull @ColumnDefault("true")
  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @NotNull @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @NotNull @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public InventoryMode getInventoryMode() {
    return inventoryMode;
  }

  public void setInventoryMode(InventoryMode inventoryMode) {
    this.inventoryMode = inventoryMode;
  }

  public Boolean getAutoAcceptOverride() {
    return autoAcceptOverride;
  }

  public void setAutoAcceptOverride(Boolean autoAcceptOverride) {
    this.autoAcceptOverride = autoAcceptOverride;
  }

  public Integer getMaxQuantityPerOrder() {
    return maxQuantityPerOrder;
  }

  public void setMaxQuantityPerOrder(Integer maxQuantityPerOrder) {
    this.maxQuantityPerOrder = maxQuantityPerOrder;
  }

  public Integer getDailyCapacityDefault() {
    return dailyCapacityDefault;
  }

  public void setDailyCapacityDefault(Integer dailyCapacityDefault) {
    this.dailyCapacityDefault = dailyCapacityDefault;
  }

  public Boolean getIsAvailable() {
    return isAvailable;
  }

  public void setIsAvailable(Boolean isAvailable) {
    this.isAvailable = isAvailable;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
