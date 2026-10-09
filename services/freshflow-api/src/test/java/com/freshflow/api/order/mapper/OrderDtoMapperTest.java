package com.freshflow.api.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.identity.model.User;
import com.freshflow.api.order.dto.response.MerchantOrderDetailDto;
import com.freshflow.api.order.dto.response.MerchantOrderSummaryDto;
import com.freshflow.api.order.model.Order;
import com.freshflow.api.order.model.OrderItem;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderDtoMapperTest {

  private OrderDtoMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new OrderDtoMapper();
  }

  @Test
  @DisplayName("toSummaryDto correctly maps order with customer and items")
  void toSummaryDto_success() {
    User customer = new User();
    customer.setId(10L);
    customer.setFullName("Nguyen Van A");
    customer.setPhone("0987654321");

    Store store = new Store();
    store.setId(1L);

    Order order = new Order();
    order.setId(100L);
    order.setOrderNumber("ORD-2026-001");
    order.setCustomerUser(customer);
    order.setStore(store);
    order.setStatus("AWAITING_MERCHANT_CONFIRMATION");
    order.setPaymentMethod("COD");
    order.setMerchantAcceptanceStatus("PENDING");
    order.setSubtotal(new BigDecimal("100000.00"));
    order.setDeliveryFee(new BigDecimal("15000.00"));
    order.setDiscountAmount(BigDecimal.ZERO);
    order.setTotalAmount(new BigDecimal("115000.00"));
    order.setCreatedAt(OffsetDateTime.of(2026, 9, 28, 10, 0, 0, 0, ZoneOffset.UTC));

    OrderItem item1 = new OrderItem();
    item1.setId(1L);
    item1.setQuantity(2);
    item1.setProductNameSnapshot("Trà Sữa Ô Long");
    item1.setVariantNameSnapshot("Size M");

    OrderItem item2 = new OrderItem();
    item2.setId(2L);
    item2.setQuantity(1);
    item2.setProductNameSnapshot("Cà Phê Muối");
    item2.setVariantNameSnapshot("Mặc định");

    order.setItems(java.util.List.of(item1, item2));

    MerchantOrderSummaryDto dto = mapper.toSummaryDto(order);

    assertThat(dto.id()).isEqualTo(100L);
    assertThat(dto.orderNumber()).isEqualTo("ORD-2026-001");
    assertThat(dto.customerName()).isEqualTo("Nguyen Van A");
    assertThat(dto.customerPhoneMasked()).isEqualTo("0987***321");
    assertThat(dto.itemsSummary())
        .isEqualTo("2x Trà Sữa Ô Long (Size M), 1x Cà Phê Muối (Mặc định)");
    assertThat(dto.totalAmount()).isEqualByComparingTo("115000.00");
    assertThat(dto.status()).isEqualTo("AWAITING_MERCHANT_CONFIRMATION");
    assertThat(dto.statusLabel()).isEqualTo("Chờ quán xác nhận");
    assertThat(dto.paymentMethod()).isEqualTo("COD");
    assertThat(dto.createdAt()).isEqualTo(order.getCreatedAt());
  }

  @Test
  @DisplayName("toSummaryDto handles null customer and empty items safely")
  void toSummaryDto_nullCustomerAndEmptyItems() {
    Order order = new Order();
    order.setId(101L);
    order.setOrderNumber("ORD-2026-002");
    order.setStatus("PROCESSING");
    order.setPaymentMethod("ONLINE_MOCK");
    order.setItems(java.util.List.of());

    MerchantOrderSummaryDto dto = mapper.toSummaryDto(order);

    assertThat(dto.customerName()).isEqualTo("Khách hàng ẩn danh");
    assertThat(dto.customerPhoneMasked()).isEqualTo("0901***456");
    assertThat(dto.itemsSummary()).isEqualTo("Không có thông tin món");
    assertThat(dto.statusLabel()).isEqualTo("Đang pha chế");
  }

  @Test
  @DisplayName("toDetailDto maps detailed fields and item breakdown")
  void toDetailDto_success() {
    User customer = new User();
    customer.setId(11L);
    customer.setFullName("Tran Thi B");
    customer.setPhone("0912345678");

    Order order = new Order();
    order.setId(200L);
    order.setOrderNumber("ORD-2026-003");
    order.setCustomerUser(customer);
    order.setStatus("CANCELLED");
    order.setPaymentMethod("COD");
    order.setMerchantAcceptanceStatus("REJECTED");
    order.setSubtotal(new BigDecimal("50000.00"));
    order.setDeliveryFee(new BigDecimal("10000.00"));
    order.setDiscountAmount(new BigDecimal("5000.00"));
    order.setTotalAmount(new BigDecimal("55000.00"));
    order.setCancelReason("Hết hàng");
    order.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    order.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));

    OrderItem item = new OrderItem();
    item.setId(5L);
    item.setProductNameSnapshot("Nước Cam Ép");
    item.setVariantNameSnapshot("Ly Lớn");
    item.setUnitPriceSnapshot(new BigDecimal("50000.00"));
    item.setQuantity(1);
    item.setLineTotal(new BigDecimal("50000.00"));
    order.setItems(java.util.List.of(item));

    MerchantOrderDetailDto detail = mapper.toDetailDto(order);

    assertThat(detail.id()).isEqualTo(200L);
    assertThat(detail.orderNumber()).isEqualTo("ORD-2026-003");
    assertThat(detail.customerName()).isEqualTo("Tran Thi B");
    assertThat(detail.customerPhoneMasked()).isEqualTo("0912***678");
    assertThat(detail.status()).isEqualTo("CANCELLED");
    assertThat(detail.statusLabel()).isEqualTo("Đã hủy");
    assertThat(detail.merchantAcceptanceStatus()).isEqualTo("REJECTED");
    assertThat(detail.cancelReason()).isEqualTo("Hết hàng");
    assertThat(detail.items()).hasSize(1);
    assertThat(detail.items().get(0).productName()).isEqualTo("Nước Cam Ép");
    assertThat(detail.items().get(0).variantName()).isEqualTo("Ly Lớn");
  }

  @Test
  @DisplayName("maskPhone formats phone numbers correctly")
  void maskPhone_formatsCorrectly() {
    assertThat(mapper.maskPhone("0987654321")).isEqualTo("0987***321");
    assertThat(mapper.maskPhone("0123456")).isEqualTo("0123***456");
    assertThat(mapper.maskPhone(null)).isEqualTo("0901***456");
    assertThat(mapper.maskPhone("12345")).isEqualTo("0901***456");
  }

  @Test
  @DisplayName("formatStatusLabel returns corresponding Vietnamese text")
  void formatStatusLabel_returnsVietnameseText() {
    assertThat(mapper.formatStatusLabel("AWAITING_MERCHANT_CONFIRMATION"))
        .isEqualTo("Chờ quán xác nhận");
    assertThat(mapper.formatStatusLabel("AWAITING_PAYMENT")).isEqualTo("Chờ thanh toán online");
    assertThat(mapper.formatStatusLabel("PENDING")).isEqualTo("Chờ chuẩn bị");
    assertThat(mapper.formatStatusLabel("PROCESSING")).isEqualTo("Đang pha chế");
    assertThat(mapper.formatStatusLabel("SHIPPING")).isEqualTo("Đang giao hàng");
    assertThat(mapper.formatStatusLabel("DELIVERY_FAILED")).isEqualTo("Giao hàng thất bại");
    assertThat(mapper.formatStatusLabel("DISPUTED")).isEqualTo("Đang khiếu nại");
    assertThat(mapper.formatStatusLabel("COMPLETED")).isEqualTo("Hoàn tất");
    assertThat(mapper.formatStatusLabel("CANCELLED")).isEqualTo("Đã hủy");
    assertThat(mapper.formatStatusLabel("CUSTOM_STATUS")).isEqualTo("CUSTOM_STATUS");
    assertThat(mapper.formatStatusLabel(null)).isEqualTo("Chưa xác định");
  }
}
