package com.freshflow.api.identity.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_store_roles")
@Getter
@Setter
@NoArgsConstructor
public class UserStoreRole {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "store_id")
  private Long storeId;

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GrantStatus status;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
