# Project Scope — FreshFlow MVP

**Phiên bản:** 1.0  
**Ngày:** 17 tháng 8 năm 2026  
**Trạng thái:** Baseline cho MVP 12 tuần  
**Tài liệu liên quan:** `requirements.md`, backlog FreshFlow MVP 12 tuần, ERD và API map

## 1. Giới thiệu dự án

### 1.1. Tên dự án

**FreshFlow — Local Store Ordering Platform.**

### 1.2. Mục tiêu tổng quan

FreshFlow là một nền tảng đặt món và vận hành cửa hàng quy mô nhỏ. Phiên bản MVP cho phép khách hàng sử dụng ứng dụng Android để đăng nhập, xem cửa hàng và sản phẩm, quản lý giỏ hàng, tạo đơn và theo dõi lịch sử đơn. Merchant sử dụng React Web để quản lý cửa hàng, danh mục, sản phẩm và theo dõi, cập nhật trạng thái đơn hàng. Backend Java Spring Boot cung cấp REST API, xác thực, phân quyền, xử lý nghiệp vụ, PostgreSQL và các cơ chế bảo vệ dữ liệu.

Mục tiêu của MVP không phải xây một siêu ứng dụng giao đồ ăn. Mục tiêu là tạo một sản phẩm full-stack có thể chạy được, có thiết kế dữ liệu rõ ràng, có API dùng chung cho web và mobile, có kiểm thử, có Docker setup và đủ chất lượng để trình bày trong portfolio Backend Java, Full-stack hoặc Android Kotlin.

### 1.3. Vấn đề cần giải quyết

Các cửa hàng nhỏ thường quản lý menu và đơn hàng qua nhiều kênh, dẫn đến dữ liệu không đồng bộ, khó kiểm soát trạng thái đơn và dễ bán vượt số lượng tồn kho. Khách hàng cần một luồng đặt món thống nhất, còn merchant cần một giao diện đơn giản để quản lý sản phẩm và xử lý đơn.

FreshFlow giải quyết vấn đề này bằng một backend trung tâm và hai client chuyên biệt: Android cho Customer và React Web cho Merchant. Mọi dữ liệu quan trọng, bao gồm giá, tổng tiền, quyền truy cập, trạng thái đơn và tồn kho, đều được kiểm tra ở backend.

## 2. Phạm vi công việc — In Scope

### 2.1. Chức năng cốt lõi

| Khu vực | Phạm vi MVP |
|---|---|
| Identity | Đăng ký, đăng nhập, BCrypt, JWT access token và phân quyền CUSTOMER/MERCHANT |
| Store/Catalog | Store, category, product; CRUD có kiểm tra quyền sở hữu; active/inactive; search, filter, pagination và sorting |
| Customer ordering | Xem catalog, xem chi tiết sản phẩm, giỏ hàng, checkout, xem chi tiết và lịch sử order |
| Merchant operations | Quản lý sản phẩm, xem danh sách order của store và cập nhật trạng thái order theo state machine |
| Inventory | Theo dõi stock, reserve/release stock, không cho stock âm và xử lý optimistic locking ở mức MVP |
| Order | Order, OrderItem, price/name snapshot, server-side total, trạng thái và idempotency cho checkout |
| Payment | Payment mock success/failure; chỉ lưu reference và trạng thái, không xử lý thẻ thật |
| Notification | Notification inbox tối giản trong database nếu kịp; không yêu cầu push notification thật |
| Documentation | OpenAPI/Swagger, API examples, ERD, business rules, README và decision records |
| Quality | Unit test, controller test, integration test với PostgreSQL/Testcontainers khi phù hợp và smoke test hai client |
| Delivery | Docker Compose cho backend và dependency; `.env.example`; hướng dẫn chạy local từ clean clone |

### 2.2. Nền tảng hỗ trợ

| Thành phần | Công nghệ và vai trò |
|---|---|
| Backend | Java 17/21, Spring Boot, Spring Web, Spring Data JPA, Spring Security, Validation, Actuator |
| Database | PostgreSQL; Flyway cho migration; Room/SQLite trên Android cho cart local |
| Web client | React + TypeScript; React Router; typed API client; responsive merchant dashboard |
| Mobile client | Kotlin Android; MVVM; Retrofit; Coroutines/Flow; Room; secure token storage phù hợp MVP |
| Local infrastructure | Docker Compose; PostgreSQL; RabbitMQ hoặc event mock chỉ khi còn thời gian |
| API | REST `/api/v1`, JSON, Bearer JWT, OpenAPI |

### 2.3. Đối tượng người dùng trong MVP

| Actor | Kênh | Mục tiêu và quyền cốt lõi |
|---|---|---|
| CUSTOMER | Android Kotlin | Đăng ký/đăng nhập, xem catalog, quản lý cart, checkout, xem order của chính mình |
| MERCHANT | React Web | Quản lý store/product của mình, xem order thuộc store và cập nhật trạng thái hợp lệ |
| FRESHFLOW BACKEND | Spring Boot | Xác thực, phân quyền, validate, tính total, quản lý order/inventory và audit cơ bản |
| PAYMENT MOCK | Backend integration | Trả kết quả payment success/failure giả lập để kiểm tra checkout flow |

Các vai trò STAFF, DRIVER, ADMIN và SUPPORT được ghi nhận là đối tượng mở rộng nhưng không thuộc MVP 12 tuần.

### 2.4. Tích hợp

MVP bao gồm các tích hợp sau:

1. React Web và Android gọi cùng REST API version `/api/v1`.
2. Spring Boot kết nối PostgreSQL bằng JPA/Flyway.
3. Android dùng Retrofit để gọi API và Room để lưu cart local.
4. Spring Security phát và kiểm tra JWT.
5. Swagger/OpenAPI mô tả API.
6. Docker Compose khởi động backend dependency và PostgreSQL.
7. Payment Mock có thể được triển khai dưới dạng module nội bộ; RabbitMQ là Should Have, không phải điều kiện bắt buộc để hoàn thành MVP.

## 3. Ngoài phạm vi — Out of Scope

Các nội dung sau không được đưa vào MVP 12 tuần, trừ khi được bổ sung bằng quyết định phạm vi mới:

| Nhóm | Ngoài phạm vi |
|---|---|
| Desktop | Electron Windows, IPC, auto-update, desktop offline queue, in phiếu |
| Delivery | Driver app, phân công shipper, GPS tracking, delivery fee động |
| Location | Google Maps hoặc bản đồ tương tác; chỉ lưu địa chỉ dạng text nếu cần cho order |
| Communication | Chat/WebSocket, support chat, read receipt |
| Notification | Push notification thật, email/SMS provider thật |
| Payment | Thẻ thật, ví điện tử thật, payment provider production |
| Data/infra | Redis, MongoDB, Kafka, Kubernetes, service mesh, multi-region |
| Cloud | Cloud production deployment, autoscaling, managed database production |
| Analytics | Recommendation, AI, báo cáo doanh thu nâng cao, BI dashboard |
| Business | Coupon phức tạp, loyalty, subscription, multi-warehouse, multi-store order |
| Roles | Driver, Support, Admin portal và workflow quản trị nâng cao |

Các tính năng ngoài phạm vi được lưu ở backlog V2, không được tự động thêm vào task MVP.

## 4. Giả định — Assumptions

1. Người học có thể dành trung bình 15–20 giờ mỗi tuần trong 12 tuần.
2. MVP chỉ cần một order thuộc đúng một store; cart không trộn sản phẩm từ nhiều store.
3. Payment chỉ là mock success/failure; không lưu card number, CVV hoặc secret payment thật.
4. Catalog và order dùng PostgreSQL; không cần polyglot persistence trong MVP.
5. React Web là client chính cho Merchant; Android là client chính cho Customer.
6. Backend là modular monolith có module boundary rõ ràng, sẵn sàng tách thành microservice sau MVP. Việc tách đầy đủ service không phải điều kiện của bản v1.0.
7. Ảnh sản phẩm có thể dùng URL hoặc placeholder; binary image storage không phải yêu cầu MVP.
8. Môi trường demo có thể chạy local bằng Docker Compose; dữ liệu seed phục vụ học tập, không phải dữ liệu production.
9. Tất cả thời gian hệ thống được lưu theo UTC; client chịu trách nhiệm hiển thị theo timezone phù hợp.
10. Merchant test account và customer test account được tạo từ seed hoặc script local, không dùng dữ liệu cá nhân thật.

## 5. Ràng buộc — Constraints

| Ràng buộc | Quyết định |
|---|---|
| Thời gian | Hoàn thành MVP trong 12 tuần, theo backlog tối đa 2 task mỗi ngày |
| Công nghệ backend | Java Spring Boot và PostgreSQL là bắt buộc |
| Client | React Web và Android Kotlin là hai client MVP; Electron bị loại khỏi MVP |
| Database | Database track phải được thực hiện trước các phần code phụ thuộc; Flyway là nguồn schema versioning |
| API | Client không được truy cập database trực tiếp; mọi nghiệp vụ đi qua REST API |
| Security | Password không lưu plaintext; quyền phải được kiểm tra ở backend |
| Quality gate | Tính năng quan trọng phải có test hoặc kiểm chứng tương ứng trước khi đánh dấu Done |
| Scope control | Không thêm feature mới nếu chưa ghi vào scope hoặc backlog V2 |
| Reproducibility | Người khác phải có thể chạy local theo README và `.env.example` |

## 6. Tiêu chí chấp nhận cấp sản phẩm

FreshFlow MVP được coi là đạt khi thỏa tất cả nhóm tiêu chí sau:

### 6.1. Customer flow

- Customer có thể đăng ký hoặc đăng nhập trên Android.
- Customer xem được store/product active từ backend thật.
- Customer xem chi tiết sản phẩm và thêm, sửa, xóa item trong cart.
- Cart được lưu local bằng Room trong phạm vi MVP.
- Customer tạo được checkout từ cart không rỗng.
- Backend tự kiểm tra giá, tồn kho và tính total.
- Customer xem được order detail và order history của chính mình.

### 6.2. Merchant flow

- Merchant đăng nhập được trên React Web.
- Merchant chỉ thao tác được store/product mình sở hữu.
- Merchant tạo, sửa, xóa và tìm kiếm product.
- Merchant xem được order thuộc store của mình.
- Merchant cập nhật được order theo state transition hợp lệ.

### 6.3. Backend and data

- API `/api/v1` có OpenAPI và error schema thống nhất.
- PostgreSQL schema được tạo bằng Flyway và có seed data có thể lặp an toàn.
- OrderItem giữ price/name snapshot.
- Stock không âm trong các tình huống được kiểm thử.
- Checkout lặp cùng Idempotency-Key không tạo duplicate order.
- JWT/RBAC trả đúng 401/403 cho các tình huống kiểm thử.

### 6.4. Quality and delivery

- Unit test và controller/integration test cho các nghiệp vụ quan trọng chạy pass.
- Backend và PostgreSQL khởi động được từ Docker Compose.
- README có setup, migration, test, demo account và API usage.
- Có ERD, API map, architecture diagram và release note.
- Có video hoặc ảnh minh họa một flow end-to-end từ Android đến React/Backend.

## 7. Release milestones

| Milestone | Điều kiện |
|---|---|
| M1 — Foundation | Requirements, scope, ERD, repository và PostgreSQL chạy được |
| M2 — Catalog | Catalog API có migration, validation, pagination và test |
| M3 — Multi-client | React catalog và Android catalog gọi backend thật |
| M4 — Secure ordering | JWT/RBAC, cart, order, checkout cơ bản |
| M5 — Consistency | Inventory reservation, locking, idempotency và failure handling |
| M6 — Portfolio v1.0 | Test, Docker, README, demo, architecture và CV-ready release |

## 8. Quy trình thay đổi phạm vi

Mọi yêu cầu mới phải được ghi thành một issue hoặc decision record với mục tiêu, lý do, ảnh hưởng thời gian và phụ thuộc. Nếu yêu cầu không cần thiết cho Customer flow, Merchant flow hoặc chất lượng backend, mặc định chuyển sang `Backlog V2` thay vì đưa vào MVP.

Một thay đổi chỉ được đưa vào MVP khi không làm mất các tiêu chí bắt buộc, không phá vỡ API contract đã công bố và không khiến lịch vượt quá 12 tuần.

## 9. Định nghĩa hoàn thành cấp Product

FreshFlow MVP được đánh dấu `Released` khi tất cả acceptance criteria cấp sản phẩm ở trên đạt, tất cả task Must Have đã có trạng thái Done hoặc có decision record hợp lệ, các lỗi Critical/High đã được xử lý, và người khác có thể clone repository, khởi động hệ thống và thực hiện demo theo README mà không cần chỉnh sửa source thủ công.

## 10. Quyết định phạm vi quan trọng

> **FreshFlow MVP 12 tuần ưu tiên một sản phẩm web và mobile chạy thật trên cùng Spring Boot/PostgreSQL backend. Microservices đầy đủ, Electron và các tích hợp production là phần mở rộng sau MVP, không phải điều kiện để hoàn thành bản đầu tiên.**
