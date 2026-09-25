package com.freshflow.api.order.infrastructure.persistence;

import com.freshflow.api.order.domain.Order;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
    extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

  Page<Order> findAllByStore_IdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

  Page<Order> findAllByStore_IdAndStatusOrderByCreatedAtDesc(
      Long storeId, String status, Pageable pageable);

  Optional<Order> findByIdAndStore_Id(Long id, Long storeId);

  long countByStore_Id(Long storeId);

  long countByStore_IdAndStatus(Long storeId, String status);

  long countByStore_IdAndCreatedAtGreaterThanEqual(Long storeId, OffsetDateTime since);

  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o "
          + "WHERE o.store.id = :storeId "
          + "AND o.status <> 'CANCELLED' "
          + "AND o.createdAt >= :since")
  BigDecimal calculateRevenueSince(
      @Param("storeId") Long storeId, @Param("since") OffsetDateTime since);
}
