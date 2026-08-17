# Software Requirements Specification — FreshFlow MVP

**Phiên bản:** 1.0  
**Ngày:** 17 tháng 8 năm 2026  
**Trạng thái:** Baseline cho MVP 12 tuần  
**Tài liệu liên quan:** `scope.md`, backlog FreshFlow MVP 12 tuần, ERD, API map và test plan

## 1. Giới thiệu

### 1.1. Mục đích

Tài liệu này mô tả các yêu cầu phần mềm của FreshFlow MVP. Tài liệu là căn cứ để thiết kế database, API, backend Spring Boot, React Web, Android Kotlin, kiểm thử và đánh giá phạm vi hoàn thành.

SRS tập trung vào hành vi có thể kiểm chứng. Mỗi yêu cầu có mã riêng để có thể truy vết từ Product Scope đến backlog, source code, test case và demo.

### 1.2. Phạm vi

FreshFlow MVP là nền tảng đặt món cho một cửa hàng trong một order. Customer sử dụng Android Kotlin để xem catalog, quản lý cart, checkout và theo dõi order. Merchant sử dụng React Web để quản lý store/product và xử lý order. Backend Java Spring Boot cung cấp REST API, PostgreSQL persistence, JWT/RBAC, validation, transaction và các cơ chế bảo vệ inventory.

MVP không bao gồm Electron, driver workflow, map, chat, push notification thật, thanh toán thật, Kafka, Redis, Kubernetes hoặc cloud production.

### 1.3. Định nghĩa, thuật ngữ và viết tắt

| Thuật ngữ | Định nghĩa |
|---|---|
| Customer | Người mua và tạo order trên Android |
| Merchant | Chủ cửa hàng quản lý catalog và order trên React Web |
| Store | Cửa hàng thuộc quyền sở hữu của merchant |
| Catalog | Store, category và product có thể hiển thị |
| Cart | Danh sách product customer đang chọn trước checkout |
| Order | Đơn hàng được tạo từ cart và thuộc một store |
| OrderItem | Một product trong order, kèm quantity và snapshot giá/tên |
| Inventory | Dữ liệu tồn kho và số lượng đã reserve của product |
| Checkout | Quy trình kiểm tra cart, stock, giá và tạo order |
| Idempotency-Key | Khóa giúp request checkout lặp không tạo duplicate order |
| JWT | JSON Web Token dùng cho authentication |
| RBAC | Role-Based Access Control |
| MVP | Minimum Viable Product trong phạm vi 12 tuần |
| API | Application Programming Interface |
| Room | SQLite persistence layer dùng cho cart local trên Android |
| Mock Payment | Module giả lập payment success/failure, không xử lý tiền thật |

### 1.4. Actor và kênh sử dụng

| Actor | Kênh | Mục tiêu |
|---|---|---|
| CUSTOMER | Android Kotlin | Tìm món, quản lý cart, checkout và xem order của mình |
| MERCHANT | React Web | Quản lý store/product và xử lý order của store |
| FreshFlow Backend | Spring Boot | Xác thực, phân quyền, xử lý nghiệp vụ và persistence |
| Payment Mock | Backend module | Trả success/failure để kiểm tra checkout |

## 2. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR-01 | Chỉ product và store ở trạng thái `ACTIVE` mới được hiển thị để mua. |
| BR-02 | Một order chỉ thuộc đúng một store; cart không được chứa product từ nhiều store. |
| BR-03 | Backend luôn đọc giá hiện tại và tự tính subtotal, total; không tin total do client gửi. |
| BR-04 | `OrderItem` phải lưu product name và unit price snapshot tại thời điểm checkout. |
| BR-05 | Quantity phải là số nguyên dương và không vượt quá giới hạn được quy định. |
| BR-06 | Inventory không được âm; reserve stock phải an toàn khi có concurrent request. |
| BR-07 | Customer chỉ xem, hủy hoặc thao tác trên order của chính mình theo quyền được phép. |
| BR-08 | Merchant chỉ thao tác trên store/product/order thuộc store mình sở hữu. |
| BR-09 | Order chỉ chuyển theo state machine hợp lệ. |
| BR-10 | Cùng Idempotency-Key và cùng user không được tạo hai order khác nhau. |
| BR-11 | Payment failure phải làm checkout thất bại; stock đã reserve phải được release hoặc ghi nhận compensation. |
| BR-12 | Password không lưu plaintext và không xuất hiện trong response hoặc log. |
| BR-13 | User identity phải lấy từ JWT; không tin `userId` nhạy cảm do client tự gửi. |
| BR-14 | Timestamp lưu theo UTC; API error có schema thống nhất. |
| BR-15 | Tất cả endpoint ghi dữ liệu phải validate input ở backend. |
| BR-16 | Trong MVP, payment là mock và không lưu card number, CVV hoặc payment secret. |

## 3. Yêu cầu chức năng

### 3.1. Identity và Access Control

#### FR-AUTH-01 — Đăng ký Customer

Hệ thống phải cho phép người dùng đăng ký tài khoản Customer bằng email hoặc username duy nhất và password hợp lệ.

**Acceptance criteria:**

- Email/username bắt buộc và không trùng.
- Password phải đạt rule tối thiểu do backend công bố.
- Password được hash bằng BCrypt hoặc cơ chế tương đương.
- Response không được chứa plaintext password hoặc password hash.
- Dữ liệu không hợp lệ trả `400 Bad Request` theo error schema chuẩn.

#### FR-AUTH-02 — Đăng nhập

Hệ thống phải xác thực credential và trả access token JWT khi hợp lệ.

**Acceptance criteria:**

- Credential đúng trả `200 OK` và access token có expiry.
- Credential sai trả lỗi không tiết lộ email có tồn tại hay không.
- Tài khoản inactive không được login.
- Login attempt được ghi log ở mức không chứa secret.

#### FR-AUTH-03 — JWT authentication

Các endpoint yêu cầu đăng nhập phải kiểm tra Bearer JWT và từ chối token thiếu, sai hoặc hết hạn.

**Acceptance criteria:**

- Thiếu token trả `401 Unauthorized`.
- Token không hợp lệ hoặc hết hạn trả `401 Unauthorized`.
- Token hợp lệ tạo được authenticated principal.
- API không sử dụng userId tùy ý từ request khi identity đã có trong JWT.

#### FR-AUTH-04 — Role-Based Access Control

Hệ thống phải hỗ trợ tối thiểu hai role `CUSTOMER` và `MERCHANT`.

**Acceptance criteria:**

- Customer không gọi được merchant management endpoint.
- Merchant không xem order của store khác.
- Role không thể tự nâng quyền qua request body.
- Role sai trả `403 Forbidden`.

#### FR-AUTH-05 — Profile cơ bản

User đã đăng nhập có thể lấy thông tin profile tối thiểu gồm id, display name, email và role.

### 3.2. Store và Catalog

#### FR-CAT-01 — Tạo Store

Merchant có thể tạo store với name, description, address, open status và thông tin liên hệ tối thiểu.

**Acceptance criteria:**

- Store được gắn với merchant hiện tại từ JWT.
- Name bắt buộc và có giới hạn độ dài.
- Store mới có trạng thái mặc định được quy định trong API contract.
- Merchant không thể gán store cho user khác bằng request body.

#### FR-CAT-02 — Xem danh sách Store

Customer và Merchant có thể xem danh sách store theo quyền và trạng thái được phép.

**Acceptance criteria:**

- Customer chỉ thấy store active/open theo policy MVP.
- Kết quả hỗ trợ page, size, sort.
- Response có total elements hoặc metadata pagination thống nhất.

#### FR-CAT-03 — Tạo Category

Merchant có thể tạo category thuộc store của mình.

**Acceptance criteria:**

- Category phải thuộc một store hợp lệ.
- Tên category không rỗng.
- Merchant không tạo category vào store không sở hữu.

#### FR-CAT-04 — Tạo Product

Merchant có thể tạo product thuộc category/store của mình với name, description, price, stock và active status.

**Acceptance criteria:**

- Price lớn hơn 0.
- Stock không âm.
- Category phải tồn tại và thuộc store của merchant.
- Product được lưu bằng migration/schema hiện hành.

#### FR-CAT-05 — Cập nhật Product

Merchant có thể sửa thông tin product thuộc store mình sở hữu.

**Acceptance criteria:**

- Không sửa được product của merchant khác.
- Không cho cập nhật price/stock bằng dữ liệu sai kiểu hoặc âm.
- Thay đổi product không làm thay đổi OrderItem snapshot cũ.

#### FR-CAT-06 — Xóa hoặc vô hiệu hóa Product

Merchant có thể xóa mềm hoặc chuyển product về inactive.

**Acceptance criteria:**

- Product đã được dùng trong order không bị xóa cứng nếu làm mất lịch sử.
- Product inactive không xuất hiện trong customer catalog.
- Product inactive không thể được checkout.

#### FR-CAT-07 — Search, filter, pagination và sorting

Catalog API phải hỗ trợ tìm kiếm theo keyword, lọc theo store/category/active status và sorting theo field được whitelist.

**Acceptance criteria:**

- Không nối trực tiếp field sort từ client vào SQL.
- Page size có giới hạn tối đa.
- Query không làm mất dữ liệu khi page vượt quá tổng số record.
- API trả metadata pagination rõ ràng.

#### FR-CAT-08 — Product detail

Customer có thể xem chi tiết product active gồm tên, mô tả, giá, ảnh URL/placeholder, category và trạng thái tồn kho hiển thị.

### 3.3. React Web cho Merchant

#### FR-WEB-01 — Merchant login

React Web phải cung cấp màn hình login và lưu phiên đăng nhập theo cơ chế được thống nhất với backend.

**Acceptance criteria:**

- Hiển thị loading khi submit.
- Hiển thị lỗi authentication rõ ràng.
- User không có role Merchant không truy cập merchant route.
- Refresh trang không làm lộ token hoặc password.

#### FR-WEB-02 — Merchant dashboard shell

React Web phải có layout responsive tối thiểu gồm navigation, main content, logout và trạng thái user hiện tại.

#### FR-WEB-03 — Product list và search

Merchant có thể xem product thuộc store của mình, tìm kiếm, lọc active/inactive và phân trang.

#### FR-WEB-04 — Product create/edit form

Merchant có thể tạo/sửa product bằng form có client validation nhưng backend validation vẫn là nguồn quyết định cuối cùng.

**Acceptance criteria:**

- Form hiển thị field error.
- Submit thành công cập nhật danh sách.
- API error được hiển thị không gây mất dữ liệu người dùng đã nhập.
- Không cho submit nhiều request do double click.

#### FR-WEB-05 — Product deactivate/delete

Merchant có thể vô hiệu hóa product và thấy trạng thái mới trên list.

#### FR-WEB-06 — Order dashboard

Merchant có thể xem order thuộc store mình và lọc theo status/date cơ bản.

#### FR-WEB-07 — Order status update

Merchant có thể chuyển order qua các trạng thái được phép trong state machine.

**Acceptance criteria:**

- Transition không hợp lệ bị từ chối.
- Order của store khác không hiển thị hoặc không update được.
- UI cập nhật trạng thái sau response thành công.

#### FR-WEB-08 — Loading, empty và error state

Mọi màn hình chính phải có trạng thái loading, empty state và error state; không hiển thị màn hình trắng khi API lỗi.

### 3.4. Android Kotlin cho Customer

#### FR-MOB-01 — Customer login/register

Android App phải cho phép Customer đăng ký và đăng nhập qua REST API.

**Acceptance criteria:**

- Có validation cơ bản trước khi gọi API.
- Hiển thị loading và lỗi mạng.
- Token được lưu trong cơ chế phù hợp MVP, không lưu trong plaintext log.
- Login thành công điều hướng tới catalog.

#### FR-MOB-02 — Store list và catalog

Customer có thể xem store/product active từ backend thật.

#### FR-MOB-03 — Product detail

Customer có thể xem detail, giá, mô tả, ảnh placeholder/URL và trạng thái có thể mua.

#### FR-MOB-04 — Room cart

Android App phải lưu cart local bằng Room trong phạm vi MVP.

**Acceptance criteria:**

- Có entity, DAO và database migration/version phù hợp.
- Có thể add, update quantity và remove item.
- Cart không chứa product từ nhiều store.
- Cart còn sau khi app recreation trong cùng môi trường local.

#### FR-MOB-05 — Cart validation

App phải cảnh báo khi product inactive, price thay đổi hoặc quantity vượt stock tại thời điểm checkout; backend vẫn là nguồn kiểm tra cuối cùng.

#### FR-MOB-06 — Checkout

Customer có thể gửi checkout từ cart không rỗng với Idempotency-Key.

**Acceptance criteria:**

- Cart rỗng không gửi checkout.
- Backend tự tính total.
- Thành công trả orderId.
- Lỗi hết hàng hoặc validation hiển thị được cho người dùng.

#### FR-MOB-07 — Order detail và history

Customer chỉ xem được order của chính mình, gồm status, item snapshot, total và timestamp.

#### FR-MOB-08 — Android architecture

Android App phải tách tối thiểu UI, ViewModel/state, repository/API và local data layer theo MVVM.

**Acceptance criteria:**

- UI không gọi Retrofit trực tiếp trong Activity/Composable.
- Network error được chuyển thành UI state.
- Coroutine không bị launch tùy tiện gây memory leak trong màn hình chính.

### 3.5. Order và Checkout

#### FR-ORD-01 — Tạo Order từ cart

Backend tạo order từ các product và quantity hợp lệ trong request.

#### FR-ORD-02 — Server-side total

Backend tính subtotal, total và các phí áp dụng trong MVP; client chỉ gửi item intent, không gửi total được tin cậy.

#### FR-ORD-03 — OrderItem snapshot

Khi tạo order, backend lưu product name và unit price tại thời điểm checkout.

#### FR-ORD-04 — Order state machine

Order phải hỗ trợ tối thiểu:

```text
PENDING → CONFIRMED → PREPARING → READY → COMPLETED
PENDING → CANCELLED
CONFIRMED → CANCELLED (nếu policy cho phép)
```

`CANCELLED` và `COMPLETED` là trạng thái kết thúc trong MVP. Transition không nằm trong state machine phải trả lỗi nghiệp vụ.

#### FR-ORD-05 — Order history

Customer xem được danh sách order của chính mình, có pagination và order detail.

#### FR-ORD-06 — Customer cancel

Customer chỉ được hủy order trước khi order chuyển sang `PREPARING`, theo rule hiện hành.

#### FR-ORD-07 — Idempotent checkout

Request checkout cùng user và cùng Idempotency-Key phải trả lại kết quả đã tạo hoặc response tương đương, không tạo order thứ hai.

#### FR-ORD-08 — Transaction boundary

Tạo order và các dữ liệu bắt buộc phải có transaction boundary rõ ràng; lỗi giữa chừng phải không để lại dữ liệu nửa vời theo policy đã thiết kế.

### 3.6. Inventory

#### FR-INV-01 — Inventory record

Mỗi product bán được phải có inventory record hoặc nguồn stock được xác định rõ.

#### FR-INV-02 — Reserve stock

Checkout phải kiểm tra và reserve stock đủ cho quantity yêu cầu.

#### FR-INV-03 — Release stock

Stock đã reserve phải được release khi payment mock thất bại hoặc order bị hủy ở trạng thái cho phép.

#### FR-INV-04 — Không stock âm

Database constraint và transaction logic phải ngăn stock âm trong các request hợp lệ.

#### FR-INV-05 — Optimistic locking

Inventory update phải có cơ chế phát hiện concurrent update, tối thiểu bằng version field hoặc chiến lược tương đương.

#### FR-INV-06 — Idempotent stock deduction

Cùng một order/event không được trừ hoặc reserve stock nhiều lần khi xử lý lặp.

### 3.7. Payment Mock và Notification tối thiểu

#### FR-PAY-01 — Create mock payment

Backend tạo payment reference cho order, không lưu dữ liệu thẻ.

#### FR-PAY-02 — Mock success

Payment success cho phép order chuyển tiếp theo policy.

#### FR-PAY-03 — Mock failure

Payment failure làm checkout thất bại và kích hoạt release/compensation inventory theo thiết kế.

#### FR-PAY-04 — Payment idempotency

Cùng payment reference không được tạo kết quả thanh toán mâu thuẫn hoặc xử lý nhiều lần.

#### FR-NOTI-01 — Notification inbox tùy chọn có điều kiện

Nếu được thực hiện trong MVP, hệ thống lưu notification tối thiểu cho các order status transition quan trọng. Push/email thật không thuộc MVP.

## 4. Yêu cầu phi chức năng

### 4.1. Hiệu năng — Performance

| Mã | Yêu cầu |
|---|---|
| NFR-PERF-01 | API read catalog trong môi trường local phải có response hợp lý với dataset demo và không tải toàn bộ catalog nếu pagination được yêu cầu. |
| NFR-PERF-02 | Endpoint list bắt buộc có page size tối đa và sorting field whitelist. |
| NFR-PERF-03 | Checkout phải tránh query thừa không cần thiết và phải có log thời gian xử lý ở mức debug/info phù hợp. |
| NFR-PERF-04 | React và Android phải hiển thị loading state thay vì block UI trong lúc gọi network. |
| NFR-PERF-05 | Android không được thực hiện network call trên main thread. |

### 4.2. Bảo mật — Security

| Mã | Yêu cầu |
|---|---|
| NFR-SEC-01 | Password phải được hash; không log password, token hoặc secret. |
| NFR-SEC-02 | Endpoint protected phải kiểm tra JWT và role/ownership ở backend. |
| NFR-SEC-03 | API phải validate input và tránh mass assignment bằng request DTO. |
| NFR-SEC-04 | Sort/filter/query field phải được whitelist hoặc parameterize an toàn. |
| NFR-SEC-05 | CORS phải được cấu hình theo môi trường, không dùng wildcard tùy tiện trong production-like configuration. |
| NFR-SEC-06 | Payment mock không được nhận hoặc lưu số thẻ thật. |
| NFR-SEC-07 | Error response không được lộ stack trace, SQL, password hoặc internal secret. |
| NFR-SEC-08 | Android không ghi token và dữ liệu nhạy cảm vào log debug. |

### 4.3. Khả năng bảo trì — Maintainability

| Mã | Yêu cầu |
|---|---|
| NFR-MNT-01 | Backend tách Controller, Service, Repository, DTO, Mapper và domain logic ở mức rõ ràng. |
| NFR-MNT-02 | Mỗi module có test và tên package phản ánh bounded context. |
| NFR-MNT-03 | Database schema thay đổi qua Flyway, không sửa thủ công trên database đã versioned. |
| NFR-MNT-04 | React dùng typed API model và component có trách nhiệm rõ ràng. |
| NFR-MNT-05 | Android tách UI, ViewModel, Repository, remote data và local data. |
| NFR-MNT-06 | README và OpenAPI phải được cập nhật khi contract thay đổi. |
| NFR-MNT-07 | Commit message và branch name phải mô tả rõ thay đổi. |

### 4.4. Khả năng mở rộng — Scalability

| Mã | Yêu cầu |
|---|---|
| NFR-SCL-01 | Module identity, catalog, order và inventory phải có ranh giới package rõ để có thể tách service sau MVP. |
| NFR-SCL-02 | API phải version bằng `/api/v1` và tránh phá vỡ response không có migration plan. |
| NFR-SCL-03 | Database query list phải có pagination và index được cân nhắc theo access pattern. |
| NFR-SCL-04 | Event/messaging chỉ được thêm khi có event contract, retry/idempotency policy và test tương ứng. |

### 4.5. Reliability và recoverability

| Mã | Yêu cầu |
|---|---|
| NFR-REL-01 | Health endpoint phải cho biết ứng dụng đang chạy; readiness dependency được xác định rõ nếu dùng. |
| NFR-REL-02 | Checkout lỗi phải trả error code ổn định và không để order ở trạng thái không xác định. |
| NFR-REL-03 | Migration và seed data phải chạy lặp an toàn trong môi trường local được hỗ trợ. |
| NFR-REL-04 | Docker Compose phải có volume PostgreSQL để dữ liệu không mất khi container restart trong local. |

## 5. Giao diện hệ thống

### 5.1. Giao diện người dùng — User Interface

#### React Web

React Web có tối thiểu các route sau:

| Route | Quyền | Mục đích |
|---|---|---|
| `/login` | Public | Merchant login |
| `/dashboard` | MERCHANT | Tổng quan và navigation |
| `/products` | MERCHANT | Product list/search/filter/pagination |
| `/products/new` | MERCHANT | Tạo product |
| `/products/:id/edit` | MERCHANT | Sửa product |
| `/orders` | MERCHANT | Order list/filter |
| `/orders/:id` | MERCHANT | Order detail và status update |

React Web phải có loading, empty, error, success feedback, route guard và responsive layout cơ bản.

#### Android App

Android App có tối thiểu các màn hình/route:

| Màn hình | Quyền | Mục đích |
|---|---|---|
| Login/Register | Public | Authentication |
| Store/Product list | CUSTOMER | Browse catalog |
| Product detail | CUSTOMER | Xem chi tiết và add cart |
| Cart | CUSTOMER | Quản lý Room cart |
| Checkout result | CUSTOMER | Xác nhận tạo order hoặc hiển thị lỗi |
| Order history | CUSTOMER | Xem danh sách order |
| Order detail | CUSTOMER | Xem item snapshot, total và status |

### 5.2. Giao diện phần cứng — Hardware Interface

Không có giao tiếp phần cứng bắt buộc trong MVP. Android chạy trên emulator hoặc thiết bị Android phù hợp. Máy in, GPS thật, máy quét mã và thiết bị thanh toán không thuộc phạm vi.

### 5.3. Giao diện phần mềm — Software Interface/API

| Hạng mục | Quy định |
|---|---|
| Base URL | `/api/v1` |
| Transport | HTTP/HTTPS tùy môi trường |
| Format | `application/json` |
| Auth | `Authorization: Bearer <JWT>` |
| Error schema | `timestamp`, `status`, `code`, `message`, `path`, `fieldErrors` tùy lỗi validation |
| Pagination | `page`, `size`, `sort`; response có `content`, `page`, `size`, `totalElements`, `totalPages` |
| Idempotency | Header `Idempotency-Key` cho checkout và thao tác yêu cầu retry an toàn |
| Docs | OpenAPI/Swagger |
| Database | PostgreSQL qua JPA/Flyway |
| Android local data | Room/SQLite cho cart |

#### API resource tối thiểu

| Resource | Endpoint mẫu | Actor |
|---|---|---|
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | Public |
| Profile | `GET /api/v1/me` | Authenticated |
| Stores | `GET /api/v1/stores`, `POST /api/v1/stores` | Customer/Merchant |
| Categories | `GET/POST/PATCH/DELETE /api/v1/.../categories` | Merchant/Customer read |
| Products | `GET/POST/PATCH/DELETE /api/v1/.../products` | Merchant/Customer read |
| Orders | `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}` | Customer |
| Merchant orders | `GET /api/v1/merchant/orders`, `PATCH /api/v1/merchant/orders/{id}/status` | Merchant |
| Inventory | `GET/PATCH /api/v1/merchant/inventory` | Merchant/Backend |
| Payment mock | Internal service/module endpoint hoặc test fixture | Backend |

Tên endpoint cuối cùng có thể điều chỉnh trong API design nhưng phải cập nhật OpenAPI, React client, Android client và traceability matrix cùng lúc.

## 6. Database và dữ liệu

### 6.1. Bảng dữ liệu MVP dự kiến

| Bảng | Mục đích |
|---|---|
| `users` | Identity và profile |
| `roles` | Role CUSTOMER/MERCHANT |
| `user_roles` | Quan hệ user-role |
| `stores` | Cửa hàng và owner |
| `categories` | Category thuộc store |
| `products` | Product thuộc category/store |
| `orders` | Order header, customer, store, status, total |
| `order_items` | Item quantity và snapshot name/price |
| `inventory_items` | Stock, reserved stock, version |
| `stock_adjustments` | Audit tăng/giảm stock nếu thực hiện trong MVP |
| `idempotency_records` | Kết quả request checkout đã xử lý |
| `payments` | Mock payment reference/status |
| `notifications` | In-app notification nếu thực hiện |

### 6.2. Yêu cầu dữ liệu

- Mọi bảng nghiệp vụ phải có primary key ổn định.
- Foreign key và constraint phải được mô tả trong ERD/migration.
- Price lưu bằng kiểu decimal phù hợp, không dùng floating point cho tiền.
- Timestamp lưu UTC.
- `order_items.unit_price` và `order_items.product_name` là snapshot, không phụ thuộc product hiện tại.
- Product không bị xóa cứng nếu làm mất lịch sử order.
- Migration phải có tên/version và được chạy từ clean database.
- Seed data phải có account demo và dữ liệu catalog tối thiểu.

## 7. Order state machine

```text
PENDING
  ├── payment success ──> CONFIRMED
  ├── customer/system cancel ──> CANCELLED
  └── payment failure ──> CANCELLED

CONFIRMED
  ├── merchant starts preparation ──> PREPARING
  └── merchant/system cancel nếu policy cho phép ──> CANCELLED

PREPARING ── merchant finishes preparation ──> READY
READY ── pickup/completion flow ──> COMPLETED
```

Các transition sau không hợp lệ trong MVP:

```text
CANCELLED -> CONFIRMED
CANCELLED -> PREPARING
COMPLETED -> PENDING
COMPLETED -> CANCELLED
PREPARING -> PENDING
```

## 8. Yêu cầu kiểm thử

| Mã | Yêu cầu kiểm thử |
|---|---|
| TEST-01 | Unit test cho total calculation, validation và order state transition. |
| TEST-02 | Controller test cho status code, response schema và validation error. |
| TEST-03 | Security test cho missing token, expired token, wrong role và wrong ownership. |
| TEST-04 | Repository/integration test cho Flyway, relationship và query pagination. |
| TEST-05 | Checkout integration test từ cart intent đến order persistence. |
| TEST-06 | Inventory concurrency test chứng minh stock không âm hoặc conflict được xử lý. |
| TEST-07 | Idempotency test submit cùng key nhiều lần không tạo duplicate order. |
| TEST-08 | Payment failure test chứng minh order failure và release/compensation inventory. |
| TEST-09 | React test cho form validation, route guard, loading/error/empty state nếu framework setup hỗ trợ. |
| TEST-10 | Android test cho ViewModel state, Room cart và network error mapping ở mức phù hợp. |
| TEST-11 | Smoke test chạy customer checkout và merchant status update với môi trường Docker. |
| TEST-12 | Regression test phải chạy trước mỗi release candidate. |

## 9. Tiêu chí chấp nhận theo workflow

### 9.1. Customer browse và cart

**Given** Customer đã đăng nhập, **when** mở catalog, **then** app hiển thị store/product active từ API với loading/error/empty state phù hợp.

**Given** Customer chọn product, **when** add vào cart, **then** Room lưu item với quantity hợp lệ và cart không trộn store.

### 9.2. Checkout

**Given** cart không rỗng và product còn hàng, **when** Customer checkout với Idempotency-Key, **then** backend tính lại giá, reserve stock và tạo order PENDING hoặc trạng thái tiếp theo theo payment mock.

**Given** payment thất bại, **when** checkout được xử lý, **then** order không được coi là thành công và stock đã reserve được release/compensate.

**Given** cùng user gửi lại cùng Idempotency-Key, **when** request được xử lý, **then** hệ thống không tạo duplicate order.

### 9.3. Merchant operations

**Given** Merchant đã đăng nhập, **when** mở product management, **then** chỉ product thuộc store của Merchant được hiển thị.

**Given** Merchant cập nhật order, **when** transition hợp lệ, **then** trạng thái được lưu và Customer có thể thấy trạng thái mới.

**Given** Merchant cố cập nhật order của store khác hoặc dùng transition sai, **then** API trả `403` hoặc lỗi nghiệp vụ thích hợp.

## 10. Traceability matrix

| Capability | Requirement | Client/Module | Backlog phase |
|---|---|---|---|
| Database foundation | BR-01 đến BR-16, NFR-MNT-03 | PostgreSQL/Flyway | 01 Database Foundation |
| Authentication | FR-AUTH-01 đến FR-AUTH-05 | identity/backend, React, Android | 02 Backend/Auth |
| Catalog | FR-CAT-01 đến FR-CAT-08 | catalog/backend, React, Android | 02–03 |
| Merchant web | FR-WEB-01 đến FR-WEB-08 | React Web | 03 React Web |
| Android customer | FR-MOB-01 đến FR-MOB-08 | Android Kotlin | 04 Android Kotlin |
| Order | FR-ORD-01 đến FR-ORD-08 | order/backend, Android, React | 02, 04, 05 |
| Inventory | FR-INV-01 đến FR-INV-06 | backend/database | 02, 05 |
| Payment mock | FR-PAY-01 đến FR-PAY-04 | backend/module | 05 Integration |
| Quality | NFR-REL, TEST-01 đến TEST-12 | backend, clients, CI/Docker | 05 Integration & Portfolio |

## 11. Ràng buộc hệ thống

1. Backend là nguồn quyết định cuối cùng cho authentication, authorization, price, total, order status và inventory.
2. React và Android không được kết nối trực tiếp đến PostgreSQL.
3. Mọi thay đổi schema phải qua Flyway.
4. Mọi public API phải dùng DTO, không expose trực tiếp JPA entity nếu tạo rủi ro contract hoặc bảo mật.
5. Mọi lỗi API phải theo error schema thống nhất.
6. Tất cả task được triển khai trong backlog 12 tuần theo tối đa hai task mỗi ngày.
7. Electron không được đưa vào acceptance criteria của MVP.
8. Tính năng ngoài scope chỉ được thực hiện sau khi MVP đạt release criteria.

## 12. Definition of Done cho MVP

Một requirement được đánh dấu `Done` khi:

- Code đã được implement đúng module.
- Có validation và error handling phù hợp.
- Có test hoặc kiểm chứng thủ công được ghi lại.
- API contract/OpenAPI đã cập nhật nếu có API.
- Database migration đã cập nhật nếu có thay đổi dữ liệu.
- React/Android đã tích hợp nếu requirement thuộc client.
- README hoặc tài liệu liên quan đã cập nhật.
- Không còn lỗi Critical/High đã biết.
- Có commit rõ ràng và có thể review.

FreshFlow MVP được đánh dấu `Released` khi toàn bộ Must Have trong `scope.md`, các workflow acceptance và release criteria đều đạt.

## 13. Future requirements — không thuộc MVP

Các requirement sau được ghi nhận cho giai đoạn sau, không dùng để đánh giá FreshFlow MVP 12 tuần:

- Tách identity, catalog, order và inventory thành microservice độc lập.
- Spring Cloud Gateway, Discovery và Config Server đầy đủ.
- RabbitMQ/Kafka event-driven flow và Saga production-like.
- Electron staff application.
- Driver workflow, map, delivery tracking.
- Chat/WebSocket và push notification.
- Redis cache/rate limit/idempotency store.
- MongoDB cho dữ liệu linh hoạt.
- CI/CD cloud, Kubernetes và observability nâng cao.
- Payment sandbox/production.

Việc chuyển một future requirement vào MVP phải được ghi nhận bằng scope change, đánh giá lại thời gian và cập nhật backlog.

## 14. Tài liệu đầu ra bắt buộc

| Artefact | Đường dẫn đề xuất |
|---|---|
| Project Scope | `docs/scope.md` |
| SRS | `docs/requirements.md` |
| ERD | `docs/architecture/erd.md` |
| API map | `docs/api/api-map.md` |
| OpenAPI | `docs/api/openapi.yaml` hoặc endpoint Swagger |
| Business rules | Có trong SRS và test reference |
| Architecture diagram | `docs/architecture/overview.md` |
| Test plan | `docs/testing/test-plan.md` |
| README | `README.md` |
| ADR | `docs/adr/` |

## 15. Quyết định baseline

> FreshFlow MVP 12 tuần là một sản phẩm web và Android chạy trên cùng Spring Boot/PostgreSQL backend. Tài liệu này là nguồn yêu cầu chi tiết để thiết kế database, API, UI, test và backlog. Electron, microservices đầy đủ và các tích hợp production thuộc roadmap mở rộng, không phải điều kiện hoàn thành MVP.
