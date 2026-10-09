package com.freshflow.api.delivery.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
public class DriverProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "store_id", nullable = false)
  private Long storeId;

  @Column(name = "is_available", nullable = false)
  private boolean available;

  @Column(name = "vehicle_type", length = 30)
  private String vehicleType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DriverStatus status;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
