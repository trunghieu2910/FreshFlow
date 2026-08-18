# Project Scope — FreshFlow MVP

**Phiên bản:** 2.0  
**Ngày:** 18 tháng 8 năm 2026  
**Trạng thái:** Đã chốt cho MVP 12 tuần  
**Tài liệu liên quan:** `requirements.md`, `order-state-machine.md`, `docs/architecture/erd.md`, backlog FreshFlow MVP 12 tuần

## 1. Giới thiệu dự án

### 1.1. Tên dự án

**FreshFlow — Local Store Ordering Platform.**

### 1.2. Mục tiêu tổng quan

FreshFlow là nền tảng đặt món cho cửa hàng địa phương. MVP cho phép Customer sử dụng Android app để xem cửa hàng và catalog, chọn ProductVariant, quản lý cart, tạo order và theo dõi delivery. Merchant sử dụng React Web để quản lý Store, Category, Product, ProductVariant, capacity, tiếp nhận order, dispatch Driver và xử lý delivery failure hoặc dispute. Driver sử dụng Android app riêng để xem các order được Backend gán, xác nhận tiền mặt, nhập OTP/PIN hoặc theo dõi xác nhận chuyển khoản tại thời điểm giao.

Backend Java Spring Boot cung cấp REST API, xác thực JWT/RBAC, kiểm tra quyền sở hữu, tính giá, xử lý Merchant acceptance, capacity/stock, payment mock, Driver assignment, Order State Machine, audit và PostgreSQL persistence.

MVP không nhằm xây một hệ thống logistics production. Mục tiêu là tạo một sản phẩm full-stack có thể chạy được, có mô hình dữ liệu rõ ràng cho food ordering, có API dùng chung cho ba client, có kiểm thử, Docker setup và đủ chất lượng để trình bày portfolio Backend Java, Full-stack hoặc Android Kotlin.

### 1.3. Vấn đề cần giải quyết

Cửa hàng nhỏ thường tiếp nhận order qua nhiều kênh, khó biết món nào còn khả năng nhận, khó kiểm soát order sau khi Merchant chấp nhận và dễ thiếu bằng chứng khi giao hàng. FreshFlow cung cấp một luồng thống nhất từ catalog, cart, Merchant acceptance, payment, chế biến, dispatch, delivery confirmation đến dispute.

## 2. Phạm vi trong MVP

| Khu vực | Phạm vi MVP |
|---|---|
| Identity | Đăng ký, đăng nhập, BCrypt, JWT và RBAC cho `CUSTOMER`, `MERCHANT`, `DRIVER`; một User có thể có nhiều role scoped theo Store |
| Store | Một Merchant có một Store trong MVP; Store có trạng thái hoạt động, chế độ auto-accept mặc định và một `MAIN_KITCHEN` mặc định |
| Catalog | Category dùng chung; Store bật Category qua `store_categories`; Product thuộc Store và Store Category; Product có ProductVariant |
| ProductVariant | Variant M/L/XL hoặc `STANDARD` cho món không có size; mỗi variant có giá, availability, `inventory_mode` và có thể override auto-accept |
| Customer ordering | Browse catalog, xem variant, cart một Store, checkout, chọn payment method, xem order detail/history |
| Cart | Cart vẫn giữ item khi variant unavailable; UI hiển thị lý do và chặn checkout cho đến khi Customer sửa cart; không tự xóa item khỏi mọi cart |
| Merchant acceptance | Store/ProductVariant có thể auto-accept hoặc manual; chỉ cần một item manual thì toàn bộ Order chờ Merchant xác nhận |
| Inventory | `MADE_TO_ORDER` dùng capacity theo ngày; `LIMITED_STOCK` dùng stock/reservation; giữ capacity/stock khi tạo order; release khi reject, cancel hoặc payment failure |
| Payment | `ONLINE_MOCK`, `CASH_ON_DELIVERY`, `BANK_TRANSFER_ON_DELIVERY`; hỗ trợ `SUCCEEDED`, `FAILED`, `REFUNDED`, `CASH_COLLECTED`, `TRANSFER_CONFIRMED`, `EXPIRED` |
| Order | Merchant acceptance, OrderItem snapshot, server-side total, Order State Machine, idempotency và audit |
| Delivery | Backend tự gán Driver available theo Store; ưu tiên Driver có ít order `SHIPPING`; Driver app hỗ trợ delivery, COD confirmation, OTP/PIN và delivery failure |
| Dispute | Customer mở dispute trong `SHIPPING` hoặc sau `COMPLETED`; Merchant resolve thành `COMPLETED` hoặc `CANCELLED` |
| Documentation | OpenAPI/Swagger, ERD, state machine, business rules, README, API examples và decision records |
| Quality | Unit, controller, integration/testcontainer khi phù hợp và smoke test Customer/Merchant/Driver |
| Infrastructure | Docker Compose, PostgreSQL, Flyway, `.env.example` và hướng dẫn chạy local |

### 2.1. Product và ProductVariant

Product là món gốc dùng để quản lý tên, mô tả, ảnh và Store ownership. ProductVariant là đơn vị Customer thực sự mua, có giá và availability riêng.

```text
Product: Trà sữa
├── ProductVariant: M, size=M, price=30.000
└── ProductVariant: L, size=L, price=40.000

Product: Cà phê đen
└── ProductVariant: STANDARD, size=NULL, price=25.000
```

Mỗi `OrderItem` tham chiếu ProductVariant và lưu snapshot `product_name`, `variant_name`, `unit_price`, `quantity` và `line_total`. M và L có inventory/capacity riêng; M hết nhưng L còn thì Customer vẫn có thể mua L.

### 2.2. Inventory mode và capacity

FreshFlow không giả định mọi món ăn đều là hàng hóa có stock thành phẩm cố định.

| Mode | Ý nghĩa | Dữ liệu chính |
|---|---|---|
| `MADE_TO_ORDER` | Món được chế biến sau khi nhận order; quản lý khả năng nhận theo ngày | `capacity_date`, `capacity_limit`, `reserved_quantity` |
| `LIMITED_STOCK` | Món/suất có số lượng hữu hạn | `stock_quantity`, `reserved_quantity`, `version` |

`MAIN_KITCHEN` là location mặc định nơi Store chuẩn bị món; MVP không quản lý nguyên liệu, recipe hoặc nhiều kho production. Capacity được giữ ngay khi tạo order để tránh overbooking. Khi order bị reject, cancel hoặc payment fail, Backend release phần đã giữ.

### 2.3. Auto-accept và Merchant acceptance

Store có cấu hình auto-accept mặc định; ProductVariant có thể override cấu hình đó. Nếu một cart có nhiều item và chỉ một item yêu cầu manual acceptance, toàn bộ Order phải chờ Merchant xác nhận.

```text
Manual:
Customer tạo Order -> AWAITING_MERCHANT_CONFIRMATION
Merchant accept -> AWAITING_PAYMENT nếu ONLINE_MOCK
Merchant reject -> CANCELLED

Auto-accept:
ONLINE_MOCK -> AWAITING_PAYMENT
COD/Bank transfer on delivery -> PROCESSING
```

### 2.4. Payment methods

| Method | Thời điểm thanh toán | Xác nhận hoàn tất |
|---|---|---|
| `ONLINE_MOCK` | Trước khi Merchant chuẩn bị; Payment Mock thành công | `Payment=SUCCEEDED`, sau đó Order đi `PENDING -> PROCESSING` |
| `CASH_ON_DELIVERY` | Driver nhận tiền mặt khi giao | Driver bấm `CASH_COLLECTED`, Backend sinh OTP, Customer đọc OTP, Driver nhập OTP |
| `BANK_TRANSFER_ON_DELIVERY` | Customer chuyển khoản tại thời điểm nhận món | Payment Mock xác nhận `TRANSFER_CONFIRMED`; không cần OTP |

COD hoặc bank transfer on delivery không đi qua `PENDING`. `PENDING` chỉ biểu thị online payment đã thành công và order chờ Merchant bắt đầu xử lý.

## 3. Actor và kênh sử dụng

| Actor | Kênh | Quyền cốt lõi |
|---|---|---|
| `CUSTOMER` | Android Kotlin | Browse, chọn variant, cart, checkout, chọn payment, xem order, xem OTP và mở dispute của mình |
| `MERCHANT` | React Web | Quản lý Store/catalog, acceptance, capacity, order, dispatch, delivery failure và dispute của Store |
| `DRIVER` | Android Kotlin | Xem order được gán, bật/tắt availability, xác nhận COD, nhập OTP/PIN, theo dõi transfer và báo delivery failure |
| `FRESHFLOW BACKEND` | Spring Boot | Auth, RBAC, validation, pricing, capacity/stock, payment, assignment, transitions, audit và idempotency |
| `PAYMENT MOCK` | Backend module | Mô phỏng success/failure/refund/cash collected/transfer confirmation/expiration |

Các role `STAFF`, `ADMIN` và `SUPPORT` là phần mở rộng. Driver đã thuộc MVP với thin Android app. GPS, bản đồ, route optimization và fleet management production không thuộc MVP.

## 4. Nền tảng và công nghệ

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17/21, Spring Boot, Spring Web, Spring Data JPA, Spring Security, Validation, Actuator |
| Database | PostgreSQL, Flyway; tiền dùng `NUMERIC(12,2)` và Java `BigDecimal`; timestamp lưu UTC |
| Web | React + TypeScript, React Router, typed API client, responsive Merchant dashboard |
| Mobile | Kotlin Android, MVVM, Retrofit, Coroutines/Flow, Room cho Customer cart |
| Infrastructure | Docker Compose, PostgreSQL; RabbitMQ chỉ là optional/future |
| API | REST `/api/v1`, JSON, Bearer JWT, OpenAPI |

## 5. Tích hợp MVP

1. React Web, Android Customer và Android Driver dùng chung REST API `/api/v1`.
2. Backend kết nối PostgreSQL qua JPA/Flyway.
3. Customer app dùng Room cho cart; Driver app dùng local state tối thiểu.
4. Spring Security kiểm tra JWT, role và Store ownership.
5. Payment Mock là module nội bộ; không có card thật hoặc provider production.
6. Backend tự gán order cho Driver `isAvailable=true`, thuộc Store tương ứng và có ít order `SHIPPING` nhất.
7. Driver không được dùng GPS/map; delivery address chỉ là text nếu cần.

## 6. Ngoài phạm vi

| Nhóm | Ngoài phạm vi MVP |
|---|---|
| Desktop | Electron Windows, IPC, auto-update, desktop offline queue, in phiếu |
| Delivery | GPS tracking, Google Maps, route optimization, delivery fee động, real-time location, fleet management production |
| Inventory | Quản lý nguyên liệu, recipe, procurement, warehouse production phức tạp, multi-location production |
| Communication | Chat/WebSocket, support chat, read receipt |
| Notification | Push/email/SMS thật; notification inbox chỉ làm nếu còn thời gian |
| Payment | Card thật, ví điện tử thật, ngân hàng thật, payment provider production |
| Data/infra | Redis, MongoDB, Kafka, Kubernetes, service mesh, multi-region |
| Business | Coupon phức tạp, loyalty, subscription, multi-store order, recommendation, BI dashboard |
| Roles | STAFF, ADMIN, SUPPORT portal và workflow quản trị nâng cao |

## 7. Assumptions và constraints

1. MVP hoàn thành trong 12 tuần, backlog tối đa hai task mỗi ngày.
2. Một Order thuộc đúng một Store; cart không trộn nhiều Store.
3. Một Merchant có một Store trong MVP; một Store có nhiều Driver.
4. Mỗi Order có tối đa một Driver active tại một thời điểm.
5. Driver assignment không giới hạn cứng số order; Backend cân bằng theo số order `SHIPPING`.
6. Store có một `MAIN_KITCHEN` mặc định trong MVP.
7. Customer không bị xóa item unavailable khỏi cart; Backend từ chối checkout cho đến khi cart hợp lệ.
8. Product/Category/Store/User dùng soft status; không hard delete dữ liệu đã tham gia order.
9. Thời gian lưu UTC; client hiển thị theo timezone phù hợp.
10. Tiền dùng PostgreSQL `NUMERIC(12,2)` và Java `BigDecimal`.
11. Payment không lưu card number, CVV hoặc secret thật.
12. Flyway là nguồn versioning schema; client không truy cập database trực tiếp.

## 8. Product acceptance criteria

### Customer

- Customer xem được Store, Category, Product và ProductVariant active.
- Customer phân biệt được size/variant, giá và availability/capacity.
- Customer có thể thêm ProductVariant vào cart của một Store.
- Item unavailable vẫn có thể hiển thị trong cart nhưng không được checkout.
- Customer chọn một trong ba payment method và xem đúng trạng thái payment/order.
- Customer xem được OTP khi COD đã được Driver xác nhận đã thu tiền.
- Customer có thể mở dispute trong `SHIPPING` hoặc sau `COMPLETED`.

### Merchant

- Merchant chỉ quản lý Store, Category assignment, Product và Variant thuộc Store mình.
- Merchant cấu hình auto-accept mặc định và override ở ProductVariant.
- Merchant xem được order chờ acceptance và chấp nhận/từ chối theo policy.
- Merchant xem capacity theo ngày, active/inactive và trạng thái availability.
- Merchant chỉ dispatch khi Backend đã gán Driver.
- Merchant retry hoặc cancel `DELIVERY_FAILED`, đồng thời resolve `DISPUTED`.

### Driver

- Driver đăng nhập bằng role `DRIVER` và bật/tắt `isAvailable` của chính mình.
- Driver chỉ thấy order được Backend gán thuộc Store hợp lệ.
- Driver xác nhận tiền COD trước khi nhận OTP.
- Driver nhập OTP đúng để hoàn tất COD; OTP sai giữ order ở `SHIPPING`.
- Bank transfer on delivery hoàn tất khi Payment Mock xác nhận, không cần OTP.
- Driver báo `DELIVERY_FAILED` với failure reason hợp lệ.

### Backend và dữ liệu

- Backend kiểm tra availability/capacity/stock ở checkout, không tin cart hoặc total từ client.
- Capacity/stock được giữ trong transaction và release đúng khi reject, cancel hoặc payment failure.
- Không tạo duplicate order với cùng `Idempotency-Key` của cùng User.
- Mọi transition quan trọng có audit actor/from/to/reason/timestamp.
- PostgreSQL schema chạy qua Flyway và ERD khớp với migration.

## 9. Milestones

| Milestone | Điều kiện |
|---|---|
| M1 — Foundation | Scope, SRS, Order State Machine, ERD, repository và PostgreSQL foundation |
| M2 — Catalog | Category, Product, ProductVariant, inventory mode, capacity và catalog API |
| M3 — Multi-client | React Merchant và Android Customer gọi backend thật |
| M4 — Acceptance and Payment | Cart, Merchant acceptance, three payment methods, payment mock và checkout |
| M5 — Delivery and Consistency | Driver assignment, COD OTP, bank transfer confirmation, locking, idempotency, dispute |
| M6 — Portfolio v1.0 | Test ba client, Docker, README, demo end-to-end và CV-ready release |

## 10. Quy trình thay đổi phạm vi

Mọi yêu cầu mới phải được ghi thành issue hoặc decision record với mục tiêu, lý do, ảnh hưởng thời gian và phụ thuộc. Nếu yêu cầu không cần thiết cho Customer flow, Merchant flow, Driver flow hoặc chất lượng backend, mặc định chuyển sang Backlog V2.

Một thay đổi chỉ được đưa vào MVP khi không phá vỡ API contract, không làm mất acceptance criteria bắt buộc và không khiến lịch vượt quá 12 tuần.

> **Quyết định phạm vi:** FreshFlow MVP 12 tuần ưu tiên một backend Spring Boot/PostgreSQL và ba client chạy thật: React Merchant, Android Customer và Android Driver. ProductVariant, daily capacity, Merchant acceptance, ba payment method, delivery confirmation và dispute là một phần của MVP; GPS, bản đồ, nguyên liệu, payment production, Electron và microservices đầy đủ là phần mở rộng sau MVP.
