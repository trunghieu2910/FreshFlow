package com.freshflow.api.identity.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
