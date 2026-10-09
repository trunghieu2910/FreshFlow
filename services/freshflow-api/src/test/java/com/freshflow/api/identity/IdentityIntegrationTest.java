package com.freshflow.api.identity;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.freshflow.api.identity.exception.IdentityAccessException;
import com.freshflow.api.identity.repository.UserRepository;
import com.freshflow.api.identity.seed.IdentityDemoSeeder;
import com.freshflow.api.identity.service.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class IdentityIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired IdentityAccessService access;
  @Autowired RoleGrantService grants;
  @Autowired PasswordEncoder encoder;
  @Autowired UserRepository users;
  @Autowired WebApplicationContext context;
  @Autowired com.freshflow.api.delivery.service.DriverProfileService driverProfiles;
  @Autowired com.freshflow.api.catalog.service.CatalogService catalog;
  @Autowired com.freshflow.api.catalog.service.CatalogAccessService catalogAccess;

  private long user() {
    return jdbc.queryForObject(
        "INSERT INTO users (email, password_hash, full_name, status, created_at, updated_at) "
            + "VALUES (?, ?, 'Test actor', 'ACTIVE', now(), now()) RETURNING id",
        Long.class,
        UUID.randomUUID() + "@test.local",
        encoder.encode("Test-only-password"));
  }

  private long store() {
    return jdbc.queryForObject("SELECT min(id) FROM stores", Long.class);
  }

  @Test
  void duplicatesAreRejectedAtDatabaseBoundary() {
    long actor = user();
    grants.grant(actor, store(), "DRIVER");
    grants.grant(actor, store(), "MERCHANT");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM user_store_roles WHERE user_id = ?", Integer.class, actor))
        .isEqualTo(2);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at) "
                        + "SELECT user_id, store_id, role_id, status, now(), now() FROM user_store_roles WHERE user_id = ? LIMIT 1",
                    actor))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void duplicateGlobalRoleIsRejected() {
    long actor = user();
    grants.grant(actor, null, "CUSTOMER");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at) "
                        + "SELECT user_id, store_id, role_id, status, now(), now() FROM user_store_roles WHERE user_id = ?",
                    actor))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void profileCreationCreatesMatchingGrantAndRemainsUnavailable() {
    long actor = user();
    var profile = driverProfiles.create(actor, store());
    assertThat(profile.getId()).isNotNull();
    assertThat(profile.isAvailable()).isFalse();
    access.requireDriver(actor);
    assertThatThrownBy(() -> driverProfiles.create(actor, store()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void storeCreationPersistsOwnerGrantTogether() {
    long actor = user();
    var store =
        catalog.createStore(
            new com.freshflow.api.catalog.dto.request.CreateStoreRequest(
                actor,
                "Identity Test Store",
                null,
                "Test address",
                false,
                com.freshflow.api.catalog.enums.StoreStatus.ACTIVE));
    assertThat(catalogAccess.requireOwnedStore(store.getId(), actor).getId())
        .isEqualTo(store.getId());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM user_store_roles WHERE user_id = ? AND store_id = ?",
                Integer.class,
                actor,
                store.getId()))
        .isEqualTo(1);
  }

  @Test
  void merchantNeedsOwnershipAndActiveGrantAtApiBoundary() throws Exception {
    var storeRow = jdbc.queryForMap("SELECT id, owner_user_id FROM stores ORDER BY id LIMIT 1");
    long storeId = ((Number) storeRow.get("id")).longValue();
    long owner = ((Number) storeRow.get("owner_user_id")).longValue();
    var mvc = MockMvcBuilders.webAppContextSetup(context).build();
    long stranger = user();
    grants.grant(stranger, storeId, "MERCHANT");
    mvc.perform(get("/api/v1/merchant/stores/{id}/orders", storeId).header("X-User-Id", stranger))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE user_store_roles SET status = 'INACTIVE' WHERE user_id = ?", owner);
    mvc.perform(get("/api/v1/merchant/stores/{id}/orders", storeId).header("X-User-Id", owner))
        .andExpect(status().isForbidden());
    grants.grant(owner, storeId, "MERCHANT");
    jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", owner);
    mvc.perform(get("/api/v1/merchant/stores/{id}/orders", storeId).header("X-User-Id", owner))
        .andExpect(status().isForbidden());
  }

  @Test
  void sameRoleCanBelongToDistinctStoresAndCustomerScopeIsExplicit() throws Exception {
    long actor = user();
    for (long storeId : jdbc.queryForList("SELECT id FROM stores", Long.class)) {
      grants.grant(actor, storeId, "MERCHANT");
      access.requireGrant(actor, "MERCHANT", storeId);
    }
    MockMvcBuilders.webAppContextSetup(context)
        .build()
        .perform(get("/api/v1/orders").header("X-User-Id", actor))
        .andExpect(status().isForbidden());
    grants.grant(actor, null, "CUSTOMER");
    access.requireGrant(actor, "CUSTOMER", null);
    assertThat(access.activeGrants(actor))
        .anySatisfy(
            grant -> {
              assertThat(grant.userId()).isEqualTo(actor);
              assertThat(grant.roleCode()).isEqualTo("CUSTOMER");
              assertThat(grant.storeId()).isNull();
            });
  }

  @Test
  void rolesAreScopedAndInactiveGrantDoesNotAuthorize() {
    long actor = user(), store = store();
    grants.grant(actor, store, "MERCHANT");
    access.requireGrant(actor, "MERCHANT", store);
    long other =
        jdbc.queryForObject("SELECT id FROM stores WHERE id <> ? LIMIT 1", Long.class, store);
    assertThatThrownBy(() -> access.requireGrant(actor, "MERCHANT", other))
        .isInstanceOf(IdentityAccessException.class);
    assertThatThrownBy(() -> grants.grant(actor, store, "CUSTOMER"))
        .isInstanceOf(IllegalArgumentException.class);
    jdbc.update("UPDATE user_store_roles SET status = 'INACTIVE' WHERE user_id = ?", actor);
    assertThatThrownBy(() -> access.requireGrant(actor, "MERCHANT", store))
        .isInstanceOf(IdentityAccessException.class);
    grants.grant(actor, store, "MERCHANT");
    access.requireGrant(actor, "MERCHANT", store);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM user_store_roles WHERE user_id = ?", Integer.class, actor))
        .isEqualTo(1);
  }

  @Test
  void lockedAccountAndSuspendedProfileDenyDriverAccess() {
    long actor = user(), store = store();
    grants.grant(actor, store, "DRIVER");
    jdbc.update(
        "INSERT INTO driver_profiles (user_id, store_id, status, created_at, updated_at) VALUES (?, ?, 'ACTIVE', now(), now())",
        actor,
        store);
    access.requireDriver(actor); // unavailable by default still permits reading assigned work
    jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", actor);
    assertThatThrownBy(() -> access.requireDriver(actor))
        .isInstanceOf(IdentityAccessException.class);
    jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", actor);
    jdbc.update("UPDATE driver_profiles SET status = 'SUSPENDED' WHERE user_id = ?", actor);
    assertThatThrownBy(() -> access.requireDriver(actor))
        .isInstanceOf(IdentityAccessException.class);
  }

  @Test
  void passwordEncodingAndStoreResponseNeverExposeCredentials() throws Exception {
    String encoded = encoder.encode("Test-only-password");
    assertThat(encoded).startsWith("{bcrypt}").isNotEqualTo("Test-only-password");
    assertThat(encoder.matches("Test-only-password", encoded)).isTrue();
    assertThat(encoder.matches("wrong", encoded)).isFalse();
    MockMvcBuilders.webAppContextSetup(context)
        .build()
        .perform(get("/api/v1/stores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$..passwordHash").doesNotExist())
        .andExpect(jsonPath("$..ownerUser").doesNotExist());
  }

  @Test
  void demoSeedIsRepeatableAndPreservesValidCredentialsAndLocks() {
    assertThat(context.getBeansOfType(IdentityDemoSeeder.class)).isEmpty();
    var seeder = new IdentityDemoSeeder(users, encoder, jdbc);
    ReflectionTestUtils.setField(seeder, "password", "Local-test-only-password");
    seeder.run(new DefaultApplicationArguments());
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM users WHERE email = 'customer.demo@freshflow.vn'",
            String.class);
    assertThat(encoder.matches("Local-test-only-password", hash)).isTrue();
    jdbc.update("UPDATE users SET status = 'LOCKED' WHERE email = 'customer.demo@freshflow.vn'");
    users.flush();
    seeder.run(new DefaultApplicationArguments());
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'customer.demo@freshflow.vn'",
                String.class))
        .isEqualTo(hash);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM users WHERE email = 'customer.demo@freshflow.vn'",
                String.class))
        .isEqualTo("LOCKED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM driver_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email IN ('driver.tea@freshflow.vn', 'driver.bakery@freshflow.vn')",
                Integer.class))
        .isEqualTo(2);
  }
}
