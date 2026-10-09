package com.freshflow.api.identity.service;

import com.freshflow.api.identity.model.*;
import com.freshflow.api.identity.repository.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal assignment writer; never exposed as a client-selected role API. */
@Service
@RequiredArgsConstructor
public class RoleGrantService {
  private final RoleRepository roles;
  private final UserStoreRoleRepository grants;

  @Transactional
  public UserStoreRole grant(Long userId, Long storeId, String roleCode) {
    if (!java.util.Set.of("CUSTOMER", "MERCHANT", "DRIVER").contains(roleCode)
        || ("CUSTOMER".equals(roleCode) != (storeId == null))) {
      throw new IllegalArgumentException("Role scope is invalid");
    }
    Role role = roles.findByCode(roleCode).orElseThrow();
    var existing = grants.findByUserIdAndStoreIdAndRoleId(userId, storeId, role.getId());
    UserStoreRole grant = existing.orElseGet(UserStoreRole::new);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (grant.getId() == null) grant.setCreatedAt(now);
    grant.setUserId(userId);
    grant.setStoreId(storeId);
    grant.setRoleId(role.getId());
    grant.setStatus(GrantStatus.ACTIVE);
    grant.setUpdatedAt(now);
    try {
      return grants.saveAndFlush(grant);
    } catch (org.springframework.dao.DataIntegrityViolationException exception) {
      throw new com.freshflow.api.identity.exception.IdentityGrantConflictException(exception);
    }
  }
}
