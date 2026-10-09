package com.freshflow.api.order.service;

import com.freshflow.api.catalog.service.CatalogAccessService;
import com.freshflow.api.order.dto.response.OrderReadDto;
import com.freshflow.api.order.exception.OrderErrorCode;
import com.freshflow.api.order.exception.OrderNotFoundException;
import com.freshflow.api.order.exception.OrderRuleViolationException;
import com.freshflow.api.order.mapper.OrderDtoMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderReadService {
  private final JdbcTemplate jdbc;
  private final CatalogAccessService catalogAccessService;
  private final OrderDtoMapper orderDtoMapper;
  private final com.freshflow.api.identity.service.IdentityAccessService identityAccessService;

  private enum Viewer {
    CUSTOMER,
    MERCHANT,
    DRIVER
  }

  private static final String DRIVER_SCOPE =
      "EXISTS (SELECT 1 FROM delivery_assignments mine "
          + "JOIN driver_profiles dp ON dp.id = mine.driver_profile_id "
          + "WHERE mine.order_id = o.id AND dp.user_id = ? AND dp.store_id = o.store_id "
          + "AND dp.status = 'ACTIVE' AND EXISTS (SELECT 1 FROM users u WHERE u.id = dp.user_id AND u.status = 'ACTIVE') "
          + "AND EXISTS (SELECT 1 FROM user_store_roles g JOIN roles r ON r.id = g.role_id "
          + "WHERE g.user_id = dp.user_id AND g.store_id = dp.store_id AND g.status = 'ACTIVE' AND r.code = 'DRIVER') "
          + "AND mine.attempt_number = (SELECT MAX(latest.attempt_number) "
          + "FROM delivery_assignments latest WHERE latest.order_id = o.id))";

  private static final String HISTORY_SELECT =
      "SELECT o.id, o.order_number, o.store_id, o.status, o.merchant_acceptance_status, "
          + "o.payment_method, o.total_amount, o.created_at, "
          + "(SELECT p.status FROM payments p WHERE p.order_id = o.id "
          + "ORDER BY p.attempt_number DESC LIMIT 1) AS payment_status, "
          + "(SELECT da.status FROM delivery_assignments da WHERE da.order_id = o.id "
          + "ORDER BY da.attempt_number DESC LIMIT 1) AS delivery_status, "
          + "(SELECT d.status FROM disputes d WHERE d.order_id = o.id "
          + "ORDER BY d.created_at DESC, d.id DESC LIMIT 1) AS dispute_status "
          + "FROM orders o WHERE ";

  public Page<OrderReadDto.HistoryEntry> customerHistory(Long actorId, Pageable pageable) {
    requireActor(actorId);
    identityAccessService.requireGrant(actorId, "CUSTOMER", null);
    return history("o.customer_user_id = ?", actorId, pageable);
  }

  public Page<OrderReadDto.HistoryEntry> merchantHistory(
      Long storeId, Long actorId, Pageable pageable) {
    catalogAccessService.requireOwnedStore(storeId, actorId);
    return history("o.store_id = ?", storeId, pageable);
  }

  public Page<OrderReadDto.HistoryEntry> driverHistory(Long actorId, Pageable pageable) {
    requireActor(actorId);
    identityAccessService.requireDriver(actorId);
    return history(DRIVER_SCOPE, actorId, pageable);
  }

  public OrderReadDto customerDetail(Long orderId, Long actorId) {
    requireActor(actorId);
    identityAccessService.requireGrant(actorId, "CUSTOMER", null);
    return detail(orderId, "o.customer_user_id = ?", actorId, Viewer.CUSTOMER);
  }

  public OrderReadDto merchantDetail(Long storeId, Long orderId, Long actorId) {
    catalogAccessService.requireOwnedStore(storeId, actorId);
    return detail(orderId, "o.store_id = ?", storeId, Viewer.MERCHANT);
  }

  public OrderReadDto driverDetail(Long orderId, Long actorId) {
    requireActor(actorId);
    identityAccessService.requireDriver(actorId);
    return detail(orderId, DRIVER_SCOPE, actorId, Viewer.DRIVER);
  }

  private Page<OrderReadDto.HistoryEntry> history(String scope, Long actor, Pageable pageable) {
    long count =
        jdbc.queryForObject("SELECT COUNT(*) FROM orders o WHERE " + scope, Long.class, actor);
    List<OrderReadDto.HistoryEntry> rows =
        jdbc.query(
            HISTORY_SELECT + scope + " ORDER BY o.created_at DESC, o.id DESC LIMIT ? OFFSET ?",
            (rs, row) ->
                new OrderReadDto.HistoryEntry(
                    rs.getLong("id"),
                    rs.getString("order_number"),
                    rs.getLong("store_id"),
                    rs.getString("status"),
                    rs.getString("merchant_acceptance_status"),
                    rs.getString("payment_method"),
                    rs.getString("payment_status"),
                    rs.getString("delivery_status"),
                    rs.getString("dispute_status"),
                    rs.getBigDecimal("total_amount"),
                    time(rs, "created_at")),
            actor,
            pageable.getPageSize(),
            pageable.getOffset());
    return new PageImpl<>(rows, pageable, count);
  }

  private OrderReadDto detail(Long orderId, String scope, Long actor, Viewer viewer) {
    List<OrderReadDto> result =
        jdbc.query(
            "SELECT o.*, u.full_name AS customer_name, u.phone AS customer_phone "
                + "FROM orders o JOIN users u ON u.id = o.customer_user_id "
                + "WHERE o.id = ? AND "
                + scope,
            (rs, row) -> project(rs, viewer),
            orderId,
            actor);
    if (result.isEmpty()) {
      throw new OrderNotFoundException(OrderErrorCode.ORDER_NOT_FOUND, "Order", orderId);
    }
    return result.getFirst();
  }

  private OrderReadDto project(ResultSet rs, Viewer viewer) throws SQLException {
    long orderId = rs.getLong("id");
    List<OrderReadDto.Item> items =
        jdbc.query(
            "SELECT id, product_id, product_variant_id, product_name_snapshot, "
                + "variant_name_snapshot, unit_price_snapshot, quantity, line_total "
                + "FROM order_items WHERE order_id = ? ORDER BY id",
            (item, row) ->
                new OrderReadDto.Item(
                    item.getLong("id"),
                    nullableLong(item, "product_id"),
                    item.getLong("product_variant_id"),
                    item.getString("product_name_snapshot"),
                    item.getString("variant_name_snapshot"),
                    item.getBigDecimal("unit_price_snapshot"),
                    item.getInt("quantity"),
                    item.getBigDecimal("line_total")),
            orderId);
    List<OrderReadDto.Payment> payments =
        jdbc.query(
            "SELECT id, attempt_number, method, status, amount, paid_at, refunded_at "
                + "FROM payments WHERE order_id = ? ORDER BY attempt_number DESC",
            (p, row) ->
                new OrderReadDto.Payment(
                    p.getLong("id"),
                    p.getInt("attempt_number"),
                    p.getString("method"),
                    p.getString("status"),
                    p.getBigDecimal("amount"),
                    time(p, "paid_at"),
                    time(p, "refunded_at")),
            orderId);
    String deliverySql =
        "SELECT da.*, dp.user_id AS driver_user_id, u.full_name AS driver_name "
            + "FROM delivery_assignments da JOIN driver_profiles dp ON dp.id = da.driver_profile_id "
            + "JOIN users u ON u.id = dp.user_id WHERE da.order_id = ? ";
    List<Object> deliveryArgs = new ArrayList<>(List.of(orderId));
    if (viewer == Viewer.DRIVER) {
      deliverySql +=
          "AND da.attempt_number = (SELECT MAX(attempt_number) FROM delivery_assignments WHERE order_id = ?) ";
      deliveryArgs.add(orderId);
    }
    List<OrderReadDto.Delivery> deliveries =
        jdbc.query(
            deliverySql + "ORDER BY da.attempt_number DESC",
            (d, row) ->
                new OrderReadDto.Delivery(
                    d.getLong("id"),
                    d.getLong("driver_user_id"),
                    d.getString("driver_name"),
                    d.getString("status"),
                    d.getInt("attempt_number"),
                    time(d, "assigned_at"),
                    time(d, "dispatched_at"),
                    time(d, "delivered_at"),
                    d.getString("failure_reason")),
            deliveryArgs.toArray());
    List<OrderReadDto.Dispute> disputes =
        jdbc.query(
            "SELECT id, reason, status, customer_message, merchant_resolution, "
                + "created_at, resolved_at FROM disputes WHERE order_id = ? "
                + "ORDER BY created_at DESC, id DESC",
            (d, row) ->
                new OrderReadDto.Dispute(
                    d.getLong("id"),
                    d.getString("reason"),
                    d.getString("status"),
                    viewer == Viewer.DRIVER ? null : d.getString("customer_message"),
                    viewer == Viewer.DRIVER ? null : d.getString("merchant_resolution"),
                    time(d, "created_at"),
                    time(d, "resolved_at")),
            orderId);
    List<OrderReadDto.Event> events =
        jdbc.query(
            "SELECT id, event_type, from_status, to_status, actor_role, reason, created_at "
                + "FROM order_audits WHERE order_id = ? ORDER BY created_at, id",
            (e, row) ->
                new OrderReadDto.Event(
                    e.getLong("id"),
                    e.getString("event_type"),
                    e.getString("from_status"),
                    e.getString("to_status"),
                    e.getString("actor_role"),
                    viewer == Viewer.DRIVER ? null : e.getString("reason"),
                    time(e, "created_at")),
            orderId);
    boolean otpRequired = "CASH_ON_DELIVERY".equals(rs.getString("payment_method"));
    boolean cashCollected =
        !payments.isEmpty() && "CASH_COLLECTED".equals(payments.getFirst().status());
    boolean verificationReady =
        viewer == Viewer.CUSTOMER
            && otpRequired
            && cashCollected
            && "SHIPPING".equals(rs.getString("status"))
            && !deliveries.isEmpty()
            && jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM delivery_credentials dc "
                    + "WHERE dc.delivery_assignment_id = ? AND dc.used_at IS NULL "
                    + "AND dc.expires_at > CURRENT_TIMESTAMP "
                    + "AND dc.attempt_count < dc.max_attempts)",
                Boolean.class,
                deliveries.getFirst().assignmentId());
    String phone = rs.getString("customer_phone");
    return new OrderReadDto(
        orderId,
        rs.getString("order_number"),
        rs.getLong("store_id"),
        rs.getString("customer_name"),
        maskPhone(phone),
        rs.getString("status"),
        orderDtoMapper.formatStatusLabel(rs.getString("status")),
        rs.getString("merchant_acceptance_status"),
        rs.getString("payment_method"),
        payments.isEmpty() ? null : payments.getFirst().status(),
        rs.getBigDecimal("subtotal"),
        rs.getBigDecimal("delivery_fee"),
        rs.getBigDecimal("discount_amount"),
        rs.getBigDecimal("total_amount"),
        rs.getString("cancel_reason"),
        time(rs, "created_at"),
        time(rs, "accepted_at"),
        time(rs, "processing_at"),
        time(rs, "completed_at"),
        time(rs, "cancelled_at"),
        new OrderReadDto.Address(
            rs.getString("recipient_name_snapshot"), rs.getString("recipient_phone_snapshot"),
            rs.getString("address_line_snapshot"), rs.getString("ward_snapshot"),
            rs.getString("district_snapshot"), rs.getString("province_snapshot")),
        items,
        payments,
        deliveries.isEmpty() ? null : deliveries.getFirst(),
        deliveries,
        new OrderReadDto.OtpPolicy(otpRequired, verificationReady, false),
        disputes,
        events);
  }

  private static void requireActor(Long actorId) {
    if (actorId == null || actorId <= 0) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ACTOR_REQUIRED, "X-User-Id is required for order reads");
    }
  }

  private static OffsetDateTime time(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, OffsetDateTime.class);
  }

  private static Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private static String maskPhone(String phone) {
    return phone == null || phone.length() < 7
        ? null
        : phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
  }
}
