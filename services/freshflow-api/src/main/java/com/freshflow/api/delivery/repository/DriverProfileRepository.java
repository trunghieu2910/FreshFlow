package com.freshflow.api.delivery.repository;

import com.freshflow.api.delivery.model.DriverProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
  Optional<DriverProfile> findByUserId(Long userId);
}
