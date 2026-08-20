# ADR-001: Chọn Modular Monolith cho FreshFlow MVP

- **Status:** Accepted
- **Date:** 2026-08-20
- **Decision owners:** FreshFlow project team

## 1. Context

FreshFlow là nền tảng đặt món ăn có nhiều client: React Merchant Web, Android Customer và Android Driver. Backend cần phục vụ các nghiệp vụ catalog, order, payment và delivery.

Dự án đồng thời dùng để học Spring Boot, RESTful API, Android và các khái niệm microservices. Tuy nhiên, MVP có thời gian giới hạn và cần ưu tiên một sản phẩm end-to-end chạy ổn định để làm portfolio intern.

Nếu triển khai nhiều microservice ngay từ đầu, dự án sẽ phải xử lý thêm network communication, service discovery, distributed tracing, message broker, retry, eventual consistency, nhiều pipeline build và nhiều database connection. Các vấn đề này có giá trị học tập nhưng có thể làm chậm order flow cốt lõi.

## 2. Decision

FreshFlow MVP sử dụng **modular monolith** trên một Spring Boot application.

Các module nghiệp vụ được tách bằng package boundary:

```text
common
catalog
order
payment
delivery
```

Mỗi module sở hữu domain logic của mình. Các module chạy cùng process và cùng deployment unit trong MVP, nhưng không được tùy ý truy cập implementation nội bộ của nhau.

## 3. Boundary rules

`common` chỉ chứa shared type thực sự dùng chung và không phụ thuộc module nghiệp vụ.

`catalog` sở hữu store, category, product, product variant và inventory availability.

`order` sở hữu cart, order, order item, pricing snapshot và order state transition.

`payment` sở hữu payment method, payment attempt và payment state.

`delivery` sở hữu driver, assignment và delivery state.

Một module không được truy cập trực tiếp repository hoặc private domain object của module khác. Nếu cần giao tiếp, module phải dùng public application contract, identifier, hoặc domain event phù hợp.

## 4. Alternatives considered

### Alternative A: Tạo nhiều microservice ngay từ đầu

Phương án này thể hiện rõ kiến trúc microservices nhưng làm tăng đáng kể độ phức tạp vận hành và thời gian hoàn thành MVP. Nó chưa phù hợp khi domain model và API contract vẫn đang được khám phá.

### Alternative B: Một package chung không có module boundary

Phương án này dễ bắt đầu nhưng nhanh chóng tạo ra package lớn, coupling cao và khó xác định ownership của business rule. Việc tách thành service ở giai đoạn sau sẽ khó hơn.

### Alternative C: Modular monolith

Phương án này giữ được tốc độ phát triển và debug của một ứng dụng đơn, đồng thời cho phép luyện tập module boundary, dependency direction và ownership. Đây là phương án được chọn.

## 5. Consequences

### Positive consequences

- Một ứng dụng dễ chạy local và dễ debug.
- Một database connection và một deployment unit trong MVP.
- Domain logic được tổ chức theo business capability.
- Có thể kiểm thử module và boundary trước khi tách thành microservice.
- Có đường nâng cấp rõ ràng cho Phase 2 khi cần scale.

### Negative consequences

- Các module vẫn dùng chung process và có nguy cơ coupling nếu boundary không được review.
- Chưa có network failure hoặc distributed transaction để luyện tập đầy đủ microservices.
- Việc tách service sau này vẫn cần thiết kế lại API contract, data ownership và deployment.

## 6. Migration path to microservices

Nếu cần tách service ở Phase 2, thứ tự dự kiến là:

```text
modular monolith
    ↓
ổn định public contract và test boundary
    ↓
tách payment hoặc delivery theo business need
    ↓
thêm messaging khi có use case rõ ràng
    ↓
tách database ownership cho service đã đủ độc lập
```

Không tách service chỉ để tạo thêm container. Việc tách phải xuất phát từ nhu cầu độc lập về scaling, deployment, ownership hoặc fault isolation.

## 7. Review trigger

ADR này cần được xem xét lại khi một trong các điều kiện xảy ra:

- Một module cần scale độc lập rõ ràng.
- Một module có release cycle khác đáng kể so với phần còn lại.
- Một module cần fault isolation riêng.
- Team đã có test contract, observability và deployment pipeline đủ ổn định.
- Chi phí coupling trong monolith lớn hơn chi phí vận hành service riêng.

## 8. Result for MVP

Trong MVP, FreshFlow giữ một Spring Boot application với package boundary rõ ràng. RabbitMQ và việc tách microservice là optional extension, không được phép trở thành dependency bắt buộc làm chậm các luồng Customer đặt món, Merchant xử lý đơn và Driver giao hàng.
