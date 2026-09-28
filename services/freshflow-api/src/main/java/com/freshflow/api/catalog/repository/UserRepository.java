package com.freshflow.api.catalog.repository;

import com.freshflow.api.catalog.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
