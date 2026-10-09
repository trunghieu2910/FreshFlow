package com.freshflow.api.identity.repository;

import com.freshflow.api.identity.model.UserStoreRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStoreRoleRepository extends JpaRepository<UserStoreRole, Long> {
  Optional<UserStoreRole> findByUserIdAndStoreIdAndRoleId(Long userId, Long storeId, Long roleId);
}
