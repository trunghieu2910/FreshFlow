package com.freshflow.api.catalog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "stores")
public class Store {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private User ownerUser;

  @Size(max = 150)
  @NotNull @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Size(max = 30)
  @Column(name = "phone", length = 30)
  private String phone;

  @Size(max = 255)
  @NotNull @Column(name = "address_line", nullable = false)
  private String addressLine;

  @NotNull @ColumnDefault("false")
  @Column(name = "auto_accept_default", nullable = false)
  private Boolean autoAcceptDefault;

  @Size(max = 20)
  @NotNull @Column(name = "status", nullable = false, length = 20)
  private String status;

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

  public User getOwnerUser() {
    return ownerUser;
  }

  public void setOwnerUser(User ownerUser) {
    this.ownerUser = ownerUser;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public void setAddressLine(String addressLine) {
    this.addressLine = addressLine;
  }

  public Boolean getAutoAcceptDefault() {
    return autoAcceptDefault;
  }

  public void setAutoAcceptDefault(Boolean autoAcceptDefault) {
    this.autoAcceptDefault = autoAcceptDefault;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
