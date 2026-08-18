# Software Requirements Specification — FreshFlow MVP

**Phiên bản:** 2.0  
**Ngày:** 18 tháng 8 năm 2026  
**Trạng thái:** Đã đồng bộ với ERD và Order State Machine chính thức  
**Tài liệu liên quan:** `scope.md`, `order-state-machine.md`, `docs/architecture/erd.md`, backlog FreshFlow MVP 12 tuần

## 1. Giới thiệu

### 1.1. Mục đích

Tài liệu này mô tả các yêu cầu phần mềm có thể kiểm chứng của FreshFlow MVP. SRS là căn cứ cho database PostgreSQL/Flyway, REST API Spring Boot, React Merchant Web, Android Customer, Android Driver, test plan và demo portfolio.

### 1.2. Phạm vi sản phẩm

FreshFlow là nền tảng đặt món cho cửa hàng địa phương. Một Order chỉ thuộc một Store. Customer sử dụng Android app để xem ProductVariant, quản lý cart, checkout, chọn payment method và theo dõi delivery. Merchant sử dụng React Web để quản lý Store/catalog, cấu hình acceptance, xem capacity, xử lý order, dispatch Driver và giải quyết delivery failure/dispute. Driver sử dụng Android app riêng để xem order được Backend gán, xác nhận COD, nhập OTP/PIN và báo giao thất bại.

Backend dùng Java Spring Boot, Spring Security JWT/RBAC, Spring Data JPA, Bean Validation, PostgreSQL, Flyway, transaction và optimistic locking. Payment là mock; không có giao dịch ngân hàng, card hoặc ví điện tử thật.

### 1.3. Thuật ngữ

| Thuật ngữ | Định nghĩa |
|---|---|
| Customer | Người mua dùng Android Customer app |
| Merchant | Chủ Store dùng React Web |
| Driver | Người giao order dùng Android Driver app |
| Store | Cửa hàng; một Merchant sở hữu một Store trong MVP |
| Product | Món gốc, thuộc một Store |
| ProductVariant | Đơn vị Customer mua; có size/giá riêng; món không size dùng `STANDARD` |
| ProductVariant `STANDARD` | Variant mặc định có `size=NULL` cho món không có size |
| Cart | Danh sách ProductVariant của một Store trước checkout |
| Order | Đơn hàng của một Customer và một Store |
| OrderItem | Dòng mua, tham chiếu ProductVariant và lưu snapshot |
| `MADE_TO_ORDER` | Món chế biến sau khi nhận order, quản lý capacity theo ngày |
| `LIMITED_STOCK` | Món/suất có số lượng giới hạn, quản lý stock/reservation |
| MAIN_KITCHEN | Location chuẩn bị món mặc định của một Store trong MVP |
| Merchant acceptance | Việc Merchant xác nhận hoặc từ chối Order cần manual approval |
| Payment attempt | Một lần xử lý payment của Order; một Order có nhiều attempt |
| OTP/PIN | Mã giao hàng do Backend tạo; Customer hiển thị, Driver nhập |
| Delivery Assignment | Lịch sử gán Order cho Driver; một record active tại một thời điểm |
| Dispute | Khiếu nại delivery do Customer mở và Merchant xử lý |
| Idempotency-Key | Khóa chống tạo duplicate order khi checkout retry |
| UTC timestamp | Thời điểm lưu trong database theo UTC |

## 2. Actors và quyền

| Actor | Quyền |
|---|---|
| `CUSTOMER` | Catalog, ProductVariant, cart, checkout, order của chính mình, OTP của order mình, dispute |
| `MERCHANT` | Store, Category assignment, Product/Variant, capacity, order của Store, acceptance, dispatch, delivery failure và dispute |
| `DRIVER` | Profile/availability của chính mình, order được gán, COD confirmation, OTP delivery và delivery failure |
| Backend/System | Tạo transition, tính giá, reserve/release, payment mock, assignment, audit và idempotency |
| Payment Mock | Success/failure/refund/expiration/cash collection/transfer confirmation giả lập |

Một User có thể có nhiều Role; Role được scoped theo Store bằng `user_store_roles`. Mọi API kiểm tra cả role và Store/order ownership.

## 3. Business Rules

| Mã | Quy tắc |
|---|---|
| BR-01 | Chỉ Store/Product/Variant/Category assignment active mới được hiển thị để mua. |
| BR-02 | Một Order và Cart chỉ thuộc một Store. Cart không trộn Store. |
| BR-03 | ProductVariant là đơn vị mua; Product có size dùng nhiều variant, món không size có một variant `STANDARD`. |
| BR-04 | ProductVariant có giá, availability, inventory mode và optional auto-accept override riêng. |
| BR-05 | Store có auto-accept mặc định; variant có thể override. Một item manual khiến toàn bộ Order chờ Merchant. |
| BR-06 | `MADE_TO_ORDER` dùng capacity theo ngày; `LIMITED_STOCK` dùng stock/reservation. |
| BR-07 | Capacity/stock được giữ trong transaction khi tạo Order; reject/cancel/payment failure phải release. |
| BR-08 | Capacity ngày hết thì catalog không cho chọn mua; item cũ trong cart vẫn hiển thị unavailable và không checkout được. |
| BR-09 | Backend kiểm tra lại active, availability, capacity, stock, giá và Store tại checkout. |
| BR-10 | OrderItem lưu snapshot product name, variant name, unit price, quantity và line total. |
| BR-11 | Backend tính subtotal/total bằng `BigDecimal`; không tin total từ client. |
| BR-12 | `ONLINE_MOCK` thanh toán trước; COD và bank transfer on delivery không đi qua `PENDING`. |
| BR-13 | `PENDING` chỉ có nghĩa online payment đã `SUCCEEDED`, Order chờ Merchant bắt đầu chuẩn bị. |
| BR-14 | Online payment timeout tại `AWAITING_PAYMENT` chuyển Order `CANCELLED`, Payment `EXPIRED` và release capacity/stock. |
| BR-15 | Merchant reject Order chưa thanh toán: `CANCELLED`, không refund; đã thanh toán: `CANCELLED` + `REFUNDED`. |
| BR-16 | COD chỉ chuyển Payment `CASH_COLLECTED` sau khi Driver xác nhận đã nhận tiền. |
| BR-17 | COD sau `CASH_COLLECTED` cần OTP đúng mới `COMPLETED`; OTP sai hoặc chưa có giữ `SHIPPING`. |
| BR-18 | Bank transfer on delivery do Payment Mock xác nhận `TRANSFER_CONFIRMED`; không cần OTP. |
| BR-19 | Một Order có tối đa một Driver active nhưng có thể có nhiều assignment history. |
| BR-20 | Backend chỉ gán Driver `isAvailable=true` thuộc Store; ưu tiên Driver có ít order `SHIPPING` nhất; không có hard limit trong MVP. |
| BR-21 | Driver chỉ thao tác trên assignment của chính mình. |
| BR-22 | Order chỉ chuyển theo Order State Machine; state kết thúc không quay lại flow cũ, trừ `COMPLETED -> DISPUTED` theo policy. |
| BR-23 | Customer chỉ hủy trước khi Merchant bắt đầu xử lý, theo state/policy; Merchant/System xử lý các case khác. |
| BR-24 | Customer có thể mở `DISPUTED` trong `SHIPPING` hoặc sau `COMPLETED`; Merchant resolve `COMPLETED` hoặc `CANCELLED`. |
| BR-25 | OTP không lưu plaintext; OTP gắn với delivery assignment/retry và giới hạn số lần thử. |
| BR-26 | Cùng `user_id + Idempotency-Key` không tạo hai Order; request khác hash với cùng key bị từ chối. |
| BR-27 | Không hard delete Product/Category/Store/User đã tham gia nghiệp vụ; dùng active/status. |
| BR-28 | Password, payment secret, card number và CVV không lưu plaintext. |
| BR-29 | Money dùng PostgreSQL `NUMERIC(12,2)` và Java `BigDecimal`; timestamp lưu UTC. |
| BR-30 | Mọi transition/payment/inventory/assignment/OTP/dispute quan trọng có audit event. |

## 4. Functional Requirements

### 4.1. Identity, authentication và authorization

#### FR-AUTH-01 — Customer registration

Hệ thống cho phép đăng ký bằng email/username duy nhất và password hợp lệ.

**Acceptance criteria:**

- Email/username bắt buộc và unique.
- Password được hash bằng BCrypt hoặc tương đương.
- Response không chứa password hoặc password hash.
- Input invalid trả error schema `400`.

#### FR-AUTH-02 — Login

Hệ thống xác thực credential và trả JWT access token có expiry.

**Acceptance criteria:**

- Credential đúng trả `200` và token.
- Credential sai không tiết lộ email tồn tại.
- Account `LOCKED`/inactive không đăng nhập.
- Không log secret.

#### FR-AUTH-03 — JWT authentication

Endpoint protected kiểm tra Bearer JWT thiếu, sai hoặc hết hạn.

#### FR-AUTH-04 — Multi-role Store-scoped RBAC

Hệ thống hỗ trợ `CUSTOMER`, `MERCHANT`, `DRIVER`; một User có thể có nhiều role scoped trong `user_store_roles`.

**Acceptance criteria:**

- Customer không gọi Merchant/Driver endpoint.
- Merchant không truy cập Store khác.
- Driver chỉ xem assignment của mình.
- User không tự nâng role/Store qua request body.
- Role hoặc Store sai trả `403`.

#### FR-AUTH-05 — Profile

User xem được id, display name, email, status và role assignments được phép.

#### FR-AUTH-06 — Driver availability

Driver xem profile và cập nhật `isAvailable` của chính mình.

**Acceptance criteria:**

- Backend không assign Driver unavailable.
- Mỗi thay đổi current state có availability history.
- Driver không đổi Store/role bằng request body.

### 4.2. Store, Category và Catalog

#### FR-CAT-01 — Store management

Merchant tạo/cập nhật Store thuộc chính mình. Trong MVP mỗi Merchant có tối đa một Store.

#### FR-CAT-02 — Store list/detail

Customer xem Store active/open; API hỗ trợ pagination/sort whitelist.

#### FR-CAT-03 — Shared Category và Store Category

Merchant bật hoặc tạo Category assignment cho Store qua `store_categories`.

**Acceptance criteria:**

- Một Category có thể dùng bởi nhiều Store.
- `UNIQUE(store_id, category_id)`.
- Product chỉ tham chiếu `store_category_id` thuộc Store của Product.
- Category inactive làm Product thuộc assignment đó không xuất hiện catalog.

#### FR-CAT-04 — Product management

Merchant tạo Product thuộc Store và `store_category_id` với name, description, image URL và active status.

#### FR-CAT-05 — ProductVariant management

Merchant tạo/sửa variant thuộc Product với `variant_name`, optional `size`, `price`, `is_available`, `inventory_mode` và `auto_accept_override`.

**Acceptance criteria:**

- Product có size có variant M/L/XL hoặc cấu hình tương ứng.
- Product không size có đúng một variant `STANDARD` với `size=NULL`.
- Price dùng `NUMERIC(12,2)` và lớn hơn 0.
- `UNIQUE(product_id, variant_name)`.
- Product/variant đã có OrderItem không hard delete.

#### FR-CAT-06 — Catalog detail

Customer xem Product và các ProductVariant active gồm tên, size, giá, image, inventory mode, availability và capacity còn nhận theo ngày nếu áp dụng.

#### FR-CAT-07 — Catalog search/filter

API hỗ trợ search keyword, Store, Store Category, active status và pagination; sort field whitelist.

#### FR-CAT-08 — Product/Variant deactivate

Merchant có thể tắt Product hoặc Variant bằng soft status. Variant inactive/unavailable không add/checkout mới; item đã ở cart vẫn hiển thị lý do không hợp lệ.

### 4.3. Inventory và Capacity

#### FR-INV-01 — Inventory location

Mỗi Store có một `MAIN_KITCHEN` mặc định trong MVP. API không yêu cầu quản lý nguyên liệu hoặc recipe.

#### FR-INV-02 — Limited stock record

Variant `LIMITED_STOCK` có `inventory_stock_records` với `stock_quantity`, `reserved_quantity` và `version`.

**Acceptance criteria:**

- `stock_quantity >= 0`.
- `reserved_quantity >= 0`.
- `reserved_quantity <= stock_quantity`.
- Concurrent reserve không bán vượt stock.

#### FR-INV-03 — Made-to-order capacity

Variant `MADE_TO_ORDER` có `inventory_capacity_records` theo `(variant_id, location_id, capacity_date)` với `capacity_limit` và `reserved_quantity`.

**Acceptance criteria:**

- `UNIQUE(variant_id, location_id, capacity_date)`.
- `reserved_quantity <= capacity_limit`.
- Khi capacity hết, catalog đánh dấu unavailable và checkout bị từ chối.
- Capacity được giữ tại thời điểm tạo Order.

#### FR-INV-04 — Reserve trong transaction

Checkout reserve capacity/stock trong transaction và dùng optimistic locking/version hoặc tương đương.

#### FR-INV-05 — Release reservation

Backend release reservation khi Merchant reject, Customer/System cancel, payment fail/expire hoặc delivery cancellation theo policy.

#### FR-INV-06 — Idempotent inventory event

Cùng Order/event không được reserve/release/deduct hai lần khi request retry.

### 4.4. Cart và Customer Android

#### FR-CART-01 — Cart một Store

Customer có cart active theo Store; không thêm ProductVariant của Store khác vào cùng cart.

#### FR-CART-02 — Cart CRUD

Customer add/update/remove variant và quantity; Android Customer lưu cart local bằng Room theo policy MVP.

#### FR-CART-03 — Cart availability validation

Khi Product/Variant unavailable hoặc capacity hết, cart vẫn hiển thị item nhưng UI disable checkout/đánh dấu lỗi; không tự xóa item khỏi mọi Customer.

#### FR-CART-04 — Cart server validation

Backend luôn kiểm tra lại Product, Variant, Store, price, availability, capacity/stock và payment eligibility; dữ liệu Room/client chỉ là intent.

#### FR-MOB-01 — Customer login/register

Android Customer có validation, loading, error state, secure token handling và MVVM separation.

#### FR-MOB-02 — Catalog và ProductVariant UI

Customer xem Product, Variant/size, giá, availability/capacity và trạng thái unavailable.

#### FR-MOB-03 — Checkout UI

Customer chọn payment method trong ba method MVP, xem acceptance/payment requirement và gửi Idempotency-Key.

#### FR-MOB-04 — Order history/detail

Customer chỉ xem Order của mình, gồm state, payment status, item snapshot, total, timestamps, Driver delivery status và cancel/dispute action theo policy.

#### FR-MOB-05 — OTP/dispute UI

Customer xem OTP chỉ khi Backend cho phép, mở dispute trong `SHIPPING` hoặc sau `COMPLETED`, xem resolution.

### 4.5. Merchant React Web

#### FR-WEB-01 — Merchant login/routing

React Web bảo vệ Merchant routes bằng JWT và role/store authorization.

#### FR-WEB-02 — Store/category/product/variant management

Merchant tạo/sửa/deactivate Store Category, Product và ProductVariant; UI hiển thị inventory mode, size, giá, availability, daily capacity và auto-accept override.

#### FR-WEB-03 — Acceptance settings

Merchant bật/tắt Store auto-accept và override ở ProductVariant. Nếu Order có item manual, Order hiển thị trong queue chờ Merchant.

#### FR-WEB-04 — Order queue

Merchant xem/filter Order theo state, payment method/status, date và acceptance requirement.

#### FR-WEB-05 — Accept/reject order

Merchant accept hoặc reject `AWAITING_MERCHANT_CONFIRMATION`.

**Acceptance criteria:**

- Reject chưa payment không refund.
- Reject online payment đã success refund mock.
- Reject release capacity/stock.
- Transition trái policy bị từ chối.

#### FR-WEB-06 — Processing/dispatch

Merchant chuyển Order hợp lệ sang `PROCESSING`, xem Driver available và dispatch khi Backend có assignment phù hợp.

#### FR-WEB-07 — Capacity management

Merchant xem và điều chỉnh availability/capacity theo ngày; không được làm reservation hiện hữu âm hoặc mất traceability.

#### FR-WEB-08 — Delivery failure/dispute

Merchant retry/cancel `DELIVERY_FAILED` và resolve `DISPUTED` thành `COMPLETED` hoặc `CANCELLED`.

#### FR-WEB-09 — Loading/error/empty state

Mọi màn hình có loading, empty và error state; API error không làm trắng màn hình.

### 4.6. Driver Android

#### FR-DRV-01 — Driver login/routing

Driver login và chỉ truy cập Driver routes.

#### FR-DRV-02 — Assigned order list/detail

Driver xem Order được Backend gán cho mình, Store, address text, item summary, payment method, status và action hợp lệ.

#### FR-DRV-03 — Driver availability

Driver bật/tắt `isAvailable`; Backend ghi history và dùng current state cho assignment.

#### FR-DRV-04 — Assignment and dispatch

Backend tự gán Order cho Driver available thuộc Store, ưu tiên Driver có ít Order `SHIPPING` nhất; không giới hạn cứng số Order trong MVP.

#### FR-DRV-05 — Cash collected

Driver bấm `CASH_COLLECTED` sau khi nhận tiền mặt. Backend kiểm tra ownership, payment method và state trước khi sinh OTP.

#### FR-DRV-06 — OTP delivery

Driver nhập OTP đúng để hoàn tất COD. OTP sai/expired/không đủ quyền không đổi state và được audit.

#### FR-DRV-07 — Bank transfer confirmation

Driver xem payment confirmation `TRANSFER_CONFIRMED` từ Payment Mock cho `BANK_TRANSFER_ON_DELIVERY`; flow này không yêu cầu OTP.

#### FR-DRV-08 — Delivery failure

Driver báo `DELIVERY_FAILED` với reason enum và ghi audit.

### 4.7. Order, acceptance và checkout

#### FR-ORD-01 — Create Order

Backend tạo Order từ cart/intent hợp lệ, gắn Customer và một Store, snapshot item và tính total.

#### FR-ORD-02 — ProductVariant snapshot

OrderItem lưu `product_id`, `product_variant_id`, `product_name_snapshot`, `variant_name_snapshot`, `unit_price_snapshot`, `quantity`, `line_total`.

#### FR-ORD-03 — Merchant acceptance state

Nếu acceptance policy manual, Order đi `AWAITING_MERCHANT_CONFIRMATION`. Merchant accept/reject theo Store ownership.

#### FR-ORD-04 — Awaiting payment

Online payment sau Merchant accept hoặc auto-accept đi `AWAITING_PAYMENT`; timeout chuyển `CANCELLED`, Payment `EXPIRED` và release reservation.

#### FR-ORD-05 — Order state machine

Order hỗ trợ:

```text
AWAITING_MERCHANT_CONFIRMATION
AWAITING_PAYMENT
PENDING
PROCESSING
SHIPPING
DELIVERY_FAILED
DISPUTED
COMPLETED
CANCELLED
```

Transition chính:

```text
AWAITING_MERCHANT_CONFIRMATION -> AWAITING_PAYMENT  (Merchant accepts + ONLINE_MOCK)
AWAITING_MERCHANT_CONFIRMATION -> PROCESSING         (Merchant accepts + COD/Bank on delivery)
AWAITING_MERCHANT_CONFIRMATION -> CANCELLED          (Merchant rejects)
AWAITING_PAYMENT -> PENDING                         (ONLINE_MOCK success)
AWAITING_PAYMENT -> CANCELLED                       (failed/expired)
PENDING -> PROCESSING                               (Merchant starts preparation)
PROCESSING -> SHIPPING                              (Merchant dispatches, Driver assigned)
SHIPPING -> COMPLETED                               (COD OTP valid or transfer confirmed)
SHIPPING -> DELIVERY_FAILED                         (Driver reports failure)
SHIPPING -> DISPUTED                                (Customer dispute)
DELIVERY_FAILED -> PROCESSING or CANCELLED          (Merchant decision)
COMPLETED -> DISPUTED                               (Customer dispute within policy)
DISPUTED -> COMPLETED or CANCELLED                  (Merchant resolution)
```

COD/Bank on delivery bypass `PENDING`. `CANCELLED` and `COMPLETED` are terminal except allowed dispute opening from `COMPLETED`.

#### FR-ORD-06 — Payment method selection

Customer chọn `ONLINE_MOCK`, `CASH_ON_DELIVERY` hoặc `BANK_TRANSFER_ON_DELIVERY`; Backend không tin method/payment status do client tự sửa sau khi Order đã tạo.

#### FR-ORD-07 — Customer cancel

Customer chỉ hủy theo policy ở state chưa Merchant bắt đầu xử lý. Backend release reservation; payment success refund, unpaid payment không refund.

#### FR-ORD-08 — Idempotent checkout

Cùng User và Idempotency-Key trả lại kết quả tương đương, không tạo Order thứ hai; request hash khác bị từ chối.

#### FR-ORD-09 — Transaction boundary

Create order, item snapshot, reservation, payment attempt và idempotency record có transaction boundary rõ; lỗi giữa chừng không để dữ liệu nửa vời.

#### FR-ORD-10 — Order history

Customer xem order của mình; Merchant xem order Store mình; Driver chỉ xem assignment của mình.

#### FR-ORD-11 — Delivery assignment

Một Order có nhiều assignment history nhưng tối đa một active assignment; reassign cập nhật `orders.current_driver_id` và assignment trong cùng transaction.

#### FR-ORD-12 — Dispute resolution

Customer mở dispute trong `SHIPPING` hoặc sau `COMPLETED`; Merchant lưu reason/resolution/resolver/time và chuyển state hợp lệ.

### 4.8. Payment Mock

#### FR-PAY-01 — Payment attempt

Một Order có thể có nhiều payment attempt. Mỗi attempt có method, status, provider/mock reference và timestamps; tối đa một attempt `SUCCEEDED` cho online flow.

#### FR-PAY-02 — Online success/failure

Payment Mock trả `SUCCEEDED` hoặc `FAILED` cho `ONLINE_MOCK`; success tạo `PENDING`, failure hủy Order và release reservation.

#### FR-PAY-03 — Online expiration

`AWAITING_PAYMENT` quá hạn chuyển Payment `EXPIRED`, Order `CANCELLED`, release reservation.

#### FR-PAY-04 — Refund

Nếu payment đã thành công nhưng Merchant/System cancel, Payment Mock chuyển `SUCCEEDED -> REFUNDED`; payment chưa thu không refund.

#### FR-PAY-05 — Cash collected

COD chỉ chuyển `PENDING -> CASH_COLLECTED` của Payment sau Driver confirmation; không sinh OTP trước bước này.

#### FR-PAY-06 — Transfer confirmed

Bank transfer on delivery dùng Mock confirmation `TRANSFER_CONFIRMED`; khi confirmed, Order được hoàn tất mà không cần OTP.

#### FR-PAY-07 — Payment authorization

Chỉ Backend/Payment Mock được đổi payment status; Customer/Driver chỉ gửi intent tương ứng và không được set trực tiếp `SUCCEEDED`, `REFUNDED` hoặc `TRANSFER_CONFIRMED`.

### 4.9. Audit, idempotency và error handling

#### FR-PLAT-01 — Order audit

Audit các event order transition, payment attempt/status, inventory reserve/release, assignment, OTP success/failure, delivery failure và dispute.

#### FR-PLAT-02 — Idempotency record

Lưu `user_id`, `idempotency_key`, `request_hash`, `order_id`, response status/body reference và expiry; unique theo user/key.

#### FR-PLAT-03 — Error schema

API ghi dữ liệu trả lỗi thống nhất gồm code, message, field errors, trace/correlation id nếu có và timestamp UTC.

#### FR-PLAT-04 — Validation

Bean Validation ở boundary, domain validation ở service và database constraints cho invariant quan trọng.

## 5. Non-Functional Requirements

| Mã | Yêu cầu |
|---|---|
| NFR-01 | Protected API dùng JWT và RBAC/store ownership. |
| NFR-02 | Password hash BCrypt; không log secret/OTP plaintext. |
| NFR-03 | Input validation ở backend; error schema thống nhất. |
| NFR-04 | Money không dùng floating point; dùng NUMERIC/BigDecimal. |
| NFR-05 | Timestamp database/API dùng UTC. |
| NFR-06 | Checkout/reservation dùng transaction và optimistic locking. |
| NFR-07 | Checkout idempotent theo user/key/request hash. |
| NFR-08 | Flyway migration chạy repeatably trên database mới. |
| NFR-09 | API có pagination, limit page size và whitelist sorting. |
| NFR-10 | Không trả dữ liệu Store/order/user ngoài authorization scope. |
| NFR-11 | Không có N+1 nghiêm trọng ở order list/catalog chính. |
| NFR-12 | Spring Actuator health endpoint không lộ secret. |
| NFR-13 | Client có loading, empty, error state và retry phù hợp. |
| NFR-14 | Android dùng MVVM, repository boundary, coroutine lifecycle đúng. |
| NFR-15 | React không gọi API trực tiếp rải rác ngoài typed service/query boundary. |
| NFR-16 | Log có correlation/order id nhưng không chứa password/payment secret/OTP. |
| NFR-17 | Tài liệu API và database được cập nhật cùng code. |
| NFR-18 | Local development chạy được bằng Docker Compose và `.env.example`. |
| NFR-19 | Test bao phủ happy path, invalid transition, payment failure, reservation race và authorization. |
| NFR-20 | Các query chính có index theo Store, status, date, active và ownership. |

## 6. Database Requirements

### 6.1. Bảng chính theo ERD

```text
users
roles
stores
user_store_roles
driver_profiles
driver_availability_history
categories
store_categories
products
product_variants
inventory_locations
inventory_stock_records
inventory_capacity_records
carts
cart_items
orders
order_items
payments
idempotency_records
delivery_assignments
delivery_credentials
disputes
order_audits
```

### 6.2. Constraint bắt buộc

| Constraint | Mục đích |
|---|---|
| `UNIQUE(stores.owner_id)` | Một Merchant tối đa một Store trong MVP |
| `UNIQUE(user_id, store_id, role_id)` | Không trùng role assignment |
| `UNIQUE(store_id, category_id)` | Store không bật Category hai lần |
| `UNIQUE(product_id, variant_name)` | Variant không trùng trong Product |
| `UNIQUE(variant_id, location_id, capacity_date)` | Một capacity record mỗi ngày/location |
| `UNIQUE(cart_id, variant_id)` | Một dòng variant trong cart |
| `UNIQUE(user_id, idempotency_key)` | Không tạo duplicate checkout |
| Active assignment unique per order | Một Driver active tại một thời điểm |
| `reserved_quantity <= stock/capacity` | Không reserve vượt khả năng |
| Money `NUMERIC(12,2)` | Không sai số floating point |
| Soft status | Bảo toàn lịch sử order |

### 6.3. Migration order

1. Users, roles, stores, user_store_roles.
2. Driver profiles và availability history.
3. Categories, store_categories, products, product_variants.
4. Inventory locations, stock records, capacity records.
5. Carts, cart_items.
6. Orders, order_items.
7. Payments, idempotency records.
8. Delivery assignments, credentials.
9. Disputes, order audits.
10. Indexes, partial unique constraints và seed `MAIN_KITCHEN`/roles.

## 7. API Requirements

API dùng `/api/v1`, JSON, Bearer JWT và error schema thống nhất.

| Nhóm | Endpoint tiêu biểu |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /me` |
| Store | `GET /stores`, `POST /merchant/stores`, `PATCH /merchant/stores/{id}` |
| Catalog | `GET /stores/{id}/catalog`, `POST/PATCH /merchant/products`, `POST/PATCH /merchant/variants` |
| Capacity | `GET/PATCH /merchant/variants/{id}/capacity`, availability toggle |
| Cart | `GET/POST/PATCH/DELETE /customer/cart` |
| Checkout | `POST /customer/orders/checkout` với `Idempotency-Key` |
| Acceptance | `POST /merchant/orders/{id}/accept`, `/reject` |
| Order | `GET /customer/orders`, `GET /merchant/orders`, `GET /orders/{id}` |
| Payment | Mock success/fail/expire/refund endpoints chỉ cho backend/test policy |
| Driver | `PATCH /driver/me/availability`, `GET /driver/assignments`, `POST /driver/assignments/{id}/cash-collected`, `POST /driver/assignments/{id}/otp/verify`, `POST /driver/assignments/{id}/failure` |
| Dispatch | `POST /merchant/orders/{id}/dispatch` |
| Dispute | `POST /customer/orders/{id}/disputes`, `POST /merchant/disputes/{id}/resolve` |

Client không được gọi database trực tiếp. Endpoint thực tế phải được ghi trong OpenAPI và kiểm tra ownership ở service layer.

## 8. Order/Payment flow acceptance

### 8.1. Online, manual acceptance

```text
Customer checkout
-> AWAITING_MERCHANT_CONFIRMATION
-> Merchant accept
-> AWAITING_PAYMENT
-> Payment SUCCEEDED
-> PENDING
-> Merchant starts preparation
-> PROCESSING
```

### 8.2. Online, auto-accept

```text
Customer checkout
-> AWAITING_PAYMENT
-> Payment SUCCEEDED
-> PENDING
-> PROCESSING
```

### 8.3. COD

```text
Manual: AWAITING_MERCHANT_CONFIRMATION -> PROCESSING
Auto:   PROCESSING
-> SHIPPING
-> Driver CASH_COLLECTED
-> Backend creates OTP
-> Driver verifies OTP from Customer
-> COMPLETED
```

OTP sai/chưa có giữ `SHIPPING`; Driver có thể báo `DELIVERY_FAILED` theo policy.

### 8.4. Bank transfer on delivery

```text
Manual: AWAITING_MERCHANT_CONFIRMATION -> PROCESSING
Auto:   PROCESSING
-> SHIPPING
-> Customer transfers
-> Payment Mock TRANSFER_CONFIRMED
-> COMPLETED
```

Flow này không yêu cầu OTP.

## 9. Test Requirements

### 9.1. Backend/domain

1. Tạo `STANDARD` variant cho Product không có size.
2. Không cho hai variant cùng tên trong Product.
3. Cart không trộn Store.
4. Item unavailable trong cart bị từ chối checkout.
5. Capacity reserve thành công khi còn chỗ.
6. Capacity race chỉ cho một request giữ suất cuối.
7. Limited stock race không bán vượt stock.
8. Merchant manual acceptance đưa Order vào đúng state.
9. Merchant reject chưa payment không refund.
10. Merchant reject online đã success refund.
11. Online timeout tạo `EXPIRED` và release reservation.
12. COD chỉ sinh OTP sau `CASH_COLLECTED`.
13. OTP sai giữ `SHIPPING`.
14. Bank transfer confirmation hoàn tất không OTP.
15. Driver khác assignment bị `403`.
16. Dispute từ `SHIPPING` và `COMPLETED` hợp lệ.
17. Dispute resolve sai actor bị từ chối.
18. Duplicate idempotency trả cùng Order.
19. Cùng key khác request hash bị từ chối.
20. Invalid state transition bị từ chối.

### 9.2. Client

- Customer catalog hiển thị variant/size/capacity.
- Cart giữ item unavailable nhưng disable checkout.
- Merchant acceptance queue và capacity UI.
- Driver assignment list, COD confirmation, OTP và failure.
- Dispute UI và resolution result.
- Loading, empty, error state trên ba client.

## 10. Traceability

| Scope/Domain | Requirements |
|---|---|
| Multi-client/RBAC | FR-AUTH-01..06, FR-WEB-01, FR-MOB-01, FR-DRV-01..03, NFR-01..03 |
| ProductVariant/catalog | FR-CAT-01..08, FR-MOB-02..03, FR-WEB-02, BR-01..05 |
| Capacity/stock | FR-INV-01..06, BR-06..09, NFR-06 |
| Merchant acceptance | FR-WEB-03..05, FR-ORD-03..04, BR-05, BR-15 |
| Checkout/cart | FR-CART-01..04, FR-ORD-01..02, FR-ORD-08..09, BR-02..11 |
| Payment | FR-PAY-01..07, FR-ORD-06..07, BR-12..18 |
| Delivery/Driver | FR-DRV-04..08, FR-ORD-11, BR-19..21 |
| Dispute/audit | FR-WEB-08, FR-MOB-05, FR-ORD-12, FR-PLAT-01, BR-24..25, BR-30 |
| Platform/security | FR-AUTH, FR-PLAT, NFR-01..20 |

## 11. Out of Scope và Future Requirements

Các yêu cầu sau không thuộc MVP: Electron, GPS/map, route optimization, real-time location, payment thật, nguyên liệu/recipe, warehouse production phức tạp, chat, push notification thật, coupon/loyalty, multi-store order, Kafka, Redis, Kubernetes, STAFF/ADMIN/SUPPORT portal.

Phase 2 có thể bổ sung daily time slot capacity, recipe/ingredient inventory, nhiều location production, Driver limit/capacity, OTP expiry job production, evidence upload, support dispute, refund provider thật và delivery tracking.

> **SRS decision:** ProductVariant là đơn vị mua; món chế biến dùng daily capacity thay vì stock thành phẩm; Merchant acceptance đứng trước online payment khi policy manual; COD dùng `CASH_COLLECTED + OTP`; bank transfer on delivery dùng `TRANSFER_CONFIRMED` không OTP; ERD/Flyway/API phải phản ánh chính xác các quyết định này.
