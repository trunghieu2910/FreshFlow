package com.freshflow.api.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.freshflow.api.catalog.dto.request.CreateStoreRequest;
import com.freshflow.api.catalog.enums.StoreStatus;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.delivery.service.DriverProfileService;
import com.freshflow.api.identity.service.RoleGrantService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class IdentityCreationRollbackIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired CatalogService catalog;
  @Autowired DriverProfileService profiles;
  @Autowired PasswordEncoder encoder;
  @MockitoBean RoleGrantService grants;

  private long actor() {
    return jdbc.queryForObject(
        "INSERT INTO users (email, password_hash, full_name, status, created_at, updated_at) "
            + "VALUES (?, ?, 'Rollback actor', 'ACTIVE', now(), now()) RETURNING id",
        Long.class,
        UUID.randomUUID() + "@test.local",
        encoder.encode("Test-only-password"));
  }

  @Test
  void storeCreationRollsBackWhenOwnerGrantFails() {
    long actor = actor();
    when(grants.grant(eq(actor), anyLong(), eq("MERCHANT")))
        .thenThrow(new IllegalStateException("Grant failure"));
    try {
      assertThatThrownBy(
              () ->
                  catalog.createStore(
                      new CreateStoreRequest(
                          actor,
                          "Rollback store",
                          null,
                          "Test address",
                          false,
                          StoreStatus.ACTIVE)))
          .isInstanceOf(IllegalStateException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM stores WHERE owner_user_id = ?", Integer.class, actor))
          .isZero();
    } finally {
      jdbc.update("DELETE FROM users WHERE id = ?", actor);
    }
  }

  @Test
  void driverCreationRollsBackWhenDriverGrantFails() {
    long actor = actor();
    long store = jdbc.queryForObject("SELECT min(id) FROM stores", Long.class);
    when(grants.grant(actor, store, "DRIVER"))
        .thenThrow(new IllegalStateException("Grant failure"));
    try {
      assertThatThrownBy(() -> profiles.create(actor, store))
          .isInstanceOf(IllegalStateException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM driver_profiles WHERE user_id = ?", Integer.class, actor))
          .isZero();
    } finally {
      jdbc.update("DELETE FROM users WHERE id = ?", actor);
    }
  }
}
