package com.freshflow.api.identity.service;

import com.freshflow.api.identity.exception.IdentityAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public identity contract. Store grants never become global authorities. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityAccessService {
  private final JdbcTemplate jdbc;

  public record ActorGrant(Long userId, String roleCode, Long storeId) {}

  /** Returns scoped grants without flattening privileges across Stores. */
  public java.util.List<ActorGrant> activeGrants(Long actorId) {
    return jdbc.query(
        "SELECT u.id, r.code, g.store_id FROM users u JOIN user_store_roles g ON g.user_id = u.id "
            + "JOIN roles r ON r.id = g.role_id WHERE u.id = ? AND u.status = 'ACTIVE' "
            + "AND g.status = 'ACTIVE' ORDER BY r.code, g.store_id NULLS FIRST",
        (rs, row) ->
            new ActorGrant(
                rs.getLong("id"), rs.getString("code"), rs.getObject("store_id", Long.class)),
        actorId);
  }

  public ActorGrant requireGrant(Long actorId, String role, Long storeId) {
    Boolean allowed =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM users u JOIN user_store_roles g ON g.user_id = u.id "
                + "JOIN roles r ON r.id = g.role_id WHERE u.id = ? AND u.status = 'ACTIVE' "
                + "AND g.status = 'ACTIVE' AND r.code = ? AND g.store_id IS NOT DISTINCT FROM ?::bigint)",
            Boolean.class,
            actorId,
            role,
            storeId);
    if (!Boolean.TRUE.equals(allowed)) throw new IdentityAccessException();
    return new ActorGrant(actorId, role, storeId);
  }

  public void requireDriver(Long actorId) {
    Long storeId =
        jdbc.query(
            "SELECT store_id FROM driver_profiles WHERE user_id = ? AND status = 'ACTIVE'",
            rs -> rs.next() ? rs.getLong(1) : null,
            actorId);
    if (storeId == null) throw new IdentityAccessException();
    requireGrant(actorId, "DRIVER", storeId);
  }
}
