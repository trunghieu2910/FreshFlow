package com.freshflow.api.identity.seed;

import com.freshflow.api.identity.model.User;
import com.freshflow.api.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional local-only demo identities. Existing hashes, locks and availability are preserved. */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class IdentityDemoSeeder implements ApplicationRunner {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JdbcTemplate jdbc;

  @Value("${freshflow.demo.password:}")
  private String password;

  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    if (password == null || password.length() < 8) {
      throw new IllegalStateException(
          "Set freshflow.demo.password with at least 8 characters for dev seed");
    }
    seedUser("customer.demo@freshflow.vn", "Demo Customer");
    jdbc.update(
        "INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at) "
            + "SELECT u.id, NULL, r.id, 'ACTIVE', now(), now() FROM users u CROSS JOIN roles r "
            + "WHERE u.email = 'customer.demo@freshflow.vn' AND r.code = 'CUSTOMER' ON CONFLICT DO NOTHING");
    for (String suffix : java.util.List.of("tea", "bakery")) {
      String merchant = "merchant." + suffix + "@freshflow.vn";
      users.findByEmail(merchant).ifPresent(user -> seedUser(merchant, user.getFullName()));
      var stores =
          jdbc.queryForList(
              "SELECT s.id FROM stores s JOIN users u ON u.id = s.owner_user_id WHERE u.email = ?",
              Long.class,
              merchant);
      if (stores.isEmpty()) continue;
      Long storeId = stores.getFirst();
      jdbc.update(
          "INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at) "
              + "SELECT u.id, ?, r.id, 'ACTIVE', now(), now() FROM users u CROSS JOIN roles r "
              + "WHERE u.email = ? AND r.code = 'MERCHANT' ON CONFLICT DO NOTHING",
          storeId,
          merchant);
      User driver = seedUser("driver." + suffix + "@freshflow.vn", "Demo Driver " + suffix);
      jdbc.update(
          "INSERT INTO driver_profiles (user_id, store_id, status, is_available, created_at, updated_at) "
              + "VALUES (?, ?, 'ACTIVE', false, now(), now()) ON CONFLICT (user_id) DO NOTHING",
          driver.getId(),
          storeId);
      jdbc.update(
          "INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at) "
              + "SELECT dp.user_id, dp.store_id, r.id, 'ACTIVE', now(), now() FROM driver_profiles dp CROSS JOIN roles r "
              + "WHERE dp.user_id = ? AND r.code = 'DRIVER' ON CONFLICT DO NOTHING",
          driver.getId());
    }
  }

  private User seedUser(String email, String name) {
    User user =
        users
            .findByEmail(email)
            .orElseGet(
                () -> {
                  User created = new User();
                  created.setEmail(email);
                  created.setFullName(name);
                  created.setStatus("ACTIVE");
                  created.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                  return created;
                });
    if (user.getPasswordHash() == null
        || java.util.Set.of("demo-hash", "demo-only-seed-hash-not-for-authentication")
            .contains(user.getPasswordHash())) {
      user.setPasswordHash(encoder.encode(password));
      user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      return users.saveAndFlush(user);
    }
    return user;
  }
}
