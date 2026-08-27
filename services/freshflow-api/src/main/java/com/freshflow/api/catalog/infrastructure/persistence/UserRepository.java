package com.freshflow.api.catalog.infrastructure.persistence;

import com.freshflow.api.catalog.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
