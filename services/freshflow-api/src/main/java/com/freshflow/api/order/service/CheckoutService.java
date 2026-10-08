package com.freshflow.api.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshflow.api.catalog.service.CatalogCheckoutReservationService;
import com.freshflow.api.catalog.service.CatalogService;
import com.freshflow.api.common.model.Money;
import com.freshflow.api.order.dto.request.CreateOrderRequest;
import com.freshflow.api.order.dto.response.CreateOrderResponse;
import com.freshflow.api.order.enums.MerchantAcceptanceStatus;
import com.freshflow.api.order.enums.OrderStatus;
import com.freshflow.api.order.exception.OrderErrorCode;
import com.freshflow.api.order.exception.OrderRuleViolationException;
import com.freshflow.api.order.model.AddressSnapshot;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates checkout writes under one PostgreSQL transaction. */
@Service
@RequiredArgsConstructor
public class CheckoutService {
  private final CatalogService catalogService;
  private final CatalogCheckoutReservationService reservationService;
  private final OrderPersistenceService persistenceService;
  private final OrderRepository orderRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** Creates an order or returns the committed result of the same idempotent request. */
  @Transactional
  public CreateOrderResponse checkout(
      Long customerId, String idempotencyKey, CreateOrderRequest request) {
    if (customerId == null || customerId <= 0) {
      throw new IllegalArgumentException("X-User-Id must be positive");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
      throw new IllegalArgumentException("Idempotency-Key must contain 1 to 120 characters");
    }
    if (!"ACTIVE".equals(catalogService.getUser(customerId).getStatus())) {
      throw new IllegalArgumentException("Customer is not active");
    }
    String requestHash = hash(request);
    jdbcTemplate.update(
        "INSERT INTO idempotency_records "
            + "(user_id, idempotency_key, request_hash, expires_at, created_at) "
            + "VALUES (?, ?, ?, now() + interval '24 hours', now()) ON CONFLICT DO NOTHING",
        customerId,
        idempotencyKey,
        requestHash);
    var prior =
        jdbcTemplate.queryForMap(
            "SELECT request_hash, order_id, response_body FROM idempotency_records "
                + "WHERE user_id = ? AND idempotency_key = ? FOR UPDATE",
            customerId,
            idempotencyKey);
    if (!requestHash.equals(prior.get("request_hash"))) {
      throw new OrderRuleViolationException(
          OrderErrorCode.ORDER_IDEMPOTENCY_CONFLICT,
          "Idempotency-Key was already used with different checkout data");
    }
    if (prior.get("order_id") != null) {
      if (prior.get("response_body") != null) {
        try {
          return objectMapper.readValue(
              prior.get("response_body").toString(), CreateOrderResponse.class);
        } catch (JsonProcessingException exception) {
          throw new IllegalStateException("Stored checkout response is invalid", exception);
        }
      }
      return CreateOrderResponse.from(
          orderRepository.findById(((Number) prior.get("order_id")).longValue()).orElseThrow());
    }

    List<CatalogCheckoutReservationService.Selection> reservationLines =
        request.items().stream()
            .map(
                item ->
                    new CatalogCheckoutReservationService.Selection(
                        item.productId(), item.variantId(), item.quantity()))
            .toList();
    boolean manual = reservationService.reserve(request.storeId(), reservationLines);
    OrderStatus initialStatus =
        manual
            ? OrderStatus.AWAITING_MERCHANT_CONFIRMATION
            : "ONLINE_MOCK".equals(request.paymentMethod())
                ? OrderStatus.AWAITING_PAYMENT
                : OrderStatus.PROCESSING;
    MerchantAcceptanceStatus acceptanceStatus =
        manual ? MerchantAcceptanceStatus.PENDING : MerchantAcceptanceStatus.ACCEPTED;
    var address = request.address();
    Order order =
        persistenceService.create(
            "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 26),
            customerId,
            request.storeId(),
            initialStatus,
            request.paymentMethod(),
            acceptanceStatus,
            request.items().stream()
                .map(
                    item ->
                        new OrderPersistenceService.LineSelection(
                            item.productId(), item.variantId(), item.quantity()))
                .toList(),
            new Money(BigDecimal.ZERO),
            new Money(BigDecimal.ZERO),
            new AddressSnapshot(
                address.recipientName(),
                address.phone(),
                address.addressLine(),
                address.ward(),
                address.district(),
                address.province()));
    if (!manual) {
      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      order.setAcceptedAt(now);
      if (initialStatus == OrderStatus.PROCESSING) {
        order.setProcessingAt(now);
      }
    }
    jdbcTemplate.update(
        "INSERT INTO payments "
            + "(order_id, attempt_number, method, status, amount, created_at, updated_at) "
            + "VALUES (?, 1, ?, 'PENDING', ?, now(), now())",
        order.getId(),
        request.paymentMethod(),
        order.getTotalAmount());
    jdbcTemplate.update(
        "INSERT INTO order_audits "
            + "(order_id, actor_user_id, actor_role, event_type, to_status, created_at) "
            + "VALUES (?, ?, 'CUSTOMER', 'ORDER_CREATED', ?, now()), "
            + "(?, ?, 'CUSTOMER', 'INVENTORY_RESERVED', ?, now()), "
            + "(?, ?, 'CUSTOMER', 'PAYMENT_ATTEMPT_CREATED', ?, now())",
        order.getId(),
        customerId,
        initialStatus.name(),
        order.getId(),
        customerId,
        initialStatus.name(),
        order.getId(),
        customerId,
        initialStatus.name());
    CreateOrderResponse response = CreateOrderResponse.from(order);
    jdbcTemplate.update(
        "UPDATE idempotency_records SET order_id = ?, response_status = 201, "
            + "response_body = ?::jsonb "
            + "WHERE user_id = ? AND idempotency_key = ?",
        order.getId(),
        json(response),
        customerId,
        idempotencyKey);
    return response;
  }

  private String json(CreateOrderResponse response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot serialize checkout response", exception);
    }
  }

  private String hash(CreateOrderRequest request) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(request);
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
      return HexFormat.of().formatHex(digest);
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Cannot hash checkout request", exception);
    }
  }
}
