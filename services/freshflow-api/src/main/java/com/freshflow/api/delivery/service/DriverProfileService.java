package com.freshflow.api.delivery.service;

import com.freshflow.api.delivery.model.*;
import com.freshflow.api.delivery.repository.DriverProfileRepository;
import com.freshflow.api.identity.service.RoleGrantService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverProfileService {
  private final DriverProfileRepository profiles;
  private final RoleGrantService grants;

  @Transactional
  public DriverProfile create(Long userId, Long storeId) {
    if (profiles.findByUserId(userId).isPresent())
      throw new IllegalArgumentException("Driver profile already exists");
    DriverProfile profile = new DriverProfile();
    profile.setUserId(userId);
    profile.setStoreId(storeId);
    profile.setStatus(DriverStatus.ACTIVE);
    profile.setAvailable(false);
    profile.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    profile.setUpdatedAt(profile.getCreatedAt());
    profiles.saveAndFlush(profile);
    grants.grant(userId, storeId, "DRIVER");
    return profile;
  }
}
