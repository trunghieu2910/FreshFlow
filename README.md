# FreshFlow

[![Release](https://img.shields.io/badge/release-catalog--v0.1-brightgreen.svg)](CHANGELOG.md)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

FreshFlow là nền tảng đặt món và giao hàng Quick-Commerce / F&B dành cho cửa hàng nhỏ, được xây dựng theo kiến trúc Modular Monolith chuẩn công nghiệp phục vụ học tập, thực hành kỹ thuật và làm portfolio dự án.

---

## 1. Mục tiêu & Phạm vi kiến trúc

- **Modular Monolith**: Tổ chức ranh giới rõ ràng giữa các module (`catalog`, `order`, `payment`, `delivery`, `common`) trên nền Java 21 & Spring Boot 4.
- **Three-Client Architecture**:
  - Backend REST API (Spring Boot + PostgreSQL 16 + Flyway).
  - Merchant Web Client (React).
  - Customer Mobile App (Android Kotlin).
- **Phạm vi Release v0.1 (Catalog Release)**:
  - Hoàn thành trọn gói Catalog vertical slice: Schema migration, quan hệ Store - Category - Product - Variant, chỉ mục B-tree functional index, API duyệt sản phẩm (tìm kiếm, lọc, phân trang, sắp xếp), API quản trị merchant, bảo mật sở hữu cửa hàng, OpenAPI 3.0/Swagger UI, tối ưu batch fetching, và bộ test suite 151 test cases.
- **Phạm vi mở rộng sau MVP**:
  - Electron desktop, workflow tài xế nâng cao, bản đồ GPS real-time, Chat trực tiếp, cổng thanh toán thật, Redis cache, Kafka event streaming, Kubernetes.

---

## 2. Cấu trúc Repository

```text
FreshFlow/
├── CHANGELOG.md             # Lịch sử phiên bản theo chuẩn Keep a Changelog & SemVer
├── CONTRIBUTING.md          # Hướng dẫn đóng góp, quy chuẩn code, test và Git commit
├── README.md                # Tài liệu tổng quan dự án và hướng dẫn vận hành
├── docs/                    # Tài liệu kiến trúc, SRS, ERD, ADR và nhật ký công việc (daily-log.md)
│   ├── architecture/        # Package boundary, ERD, design decisions
│   └── daily-log.md         # Nhật ký tiến độ từng task theo backlog
├── infrastructure/          # Cấu hình hạ tầng môi trường phát triển cục bộ
│   ├── docker-compose.yml   # Dịch vụ PostgreSQL 16 phát triển cục bộ
│   └── .env.example         # Biến môi trường mẫu
├── plan/                    # Kế hoạch phát triển MVP 12 tuần
├── postman/                 # Postman collections kiểm thử API tự động
│   └── catalog.json         # Bộ kiểm thử E2E Catalog API (100% pass)
└── services/
    └── freshflow-api/       # Backend service chính (Spring Boot modular monolith)
        ├── pom.xml          # Cấu hình Maven & dependencies (Release v0.1.0)
        └── src/             # Mã nguồn ứng dụng và bộ kiểm thử tự động
```

---

## 3. Yêu cầu cài đặt & Môi trường (Prerequisites)

| Công cụ | Phiên bản yêu cầu | Ghi chú |
|---|---|---|
| **Java Development Kit (JDK)** | JDK 21 (LTS) | Khuyến nghị Eclipse Temurin hoặc Oracle JDK 21 |
| **Apache Maven** | 3.9+ | Hoặc sử dụng Maven Wrapper đi kèm |
| **Docker & Docker Compose** | Docker Desktop mới nhất | Cần thiết để chạy container PostgreSQL 16 |
| **Git** | Bản ổn định | Git Bash trên Windows hoặc Terminal trên macOS/Linux |

Kiểm tra môi trường cục bộ:
```bash
java -version
docker --version
docker compose version
```

---

## 4. Thiết lập & Khởi chạy cục bộ (Setup & Run)

### Bước 1: Khởi động cơ sở dữ liệu PostgreSQL

FreshFlow sử dụng PostgreSQL 16 chạy trong container Docker:

```bash
# Di chuyển vào thư mục hạ tầng
cd infrastructure

# Khởi động PostgreSQL ở chế độ chạy nền
docker compose up -d

# Kiểm tra trạng thái container
docker compose ps
```

Container PostgreSQL sẽ lắng nghe tại cổng `localhost:5432` với cơ sở dữ liệu `freshflow`.

### Bước 2: Build & Chạy Backend API

Database schema và dữ liệu mẫu F&B sẽ được Flyway tự động nạp (Migrations V1 đến V5):

```bash
# Di chuyển vào thư mục backend
cd ../services/freshflow-api

# Khởi chạy ứng dụng Spring Boot
mvn spring-boot:run
```

Ứng dụng sẽ khởi động thành công trên cổng `http://localhost:8080`.

---

## 5. Danh mục REST API (Catalog API Reference)

### 5.1. Customer Catalog APIs (Công khai)

| Phương thức | Endpoint | Mô tả | Tham số truy vấn (Query Parameters) |
|---|---|---|---|
| `GET` | `/api/v1/stores/{storeId}/products` | Danh sách sản phẩm của cửa hàng có phân trang, tìm kiếm & bộ lọc | - `page`: Số trang (0-indexed, mặc định: 0)<br>- `size`: Kích thước trang (mặc định: 20)<br>- `sort`: Sắp xếp (ví dụ: `price,asc`, `price,desc`, `name,asc`, mặc định: `name,asc`)<br>- `search`: Tìm kiếm tên sản phẩm không phân biệt hoa/thường<br>- `categoryId`: Lọc theo danh mục sản phẩm<br>- `minPrice`: Giá tối thiểu (VND)<br>- `maxPrice`: Giá tối đa (VND)<br>- `availableOnly`: `true`/`false` chỉ lấy món đang bán |
| `GET` | `/api/v1/stores/{storeId}/products/{productId}` | Lấy thông tin chi tiết một sản phẩm kèm danh sách biến thể (Size M, L, Standard) | Không |

### 5.2. Merchant Management APIs (Yêu cầu xác thực chủ cửa hàng)

*Ghi chú:* Trong phiên bản v0.1, các API Merchant yêu cầu định danh qua Header `X-User-Id: <userId>`. Hệ thống sẽ kiểm tra quyền sở hữu cửa hàng tương ứng qua `CatalogAccessService`.

| Phương thức | Endpoint | Header bắt buộc | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/merchant/stores/{storeId}/products` | `X-User-Id: <id>` | Tạo mới sản phẩm cho cửa hàng |
| `PATCH` | `/api/v1/merchant/stores/{storeId}/products/{productId}` | `X-User-Id: <id>` | Cập nhật thông tin sản phẩm (tên, mô tả, danh mục, trạng thái active) |
| `DELETE` | `/api/v1/merchant/stores/{storeId}/products/{productId}` | `X-User-Id: <id>` | Xóa mềm sản phẩm (đánh dấu `deleted_at`) |
| `POST` | `/api/v1/merchant/stores/{storeId}/products/{productId}/variants` | `X-User-Id: <id>` | Thêm biến thể kích cỡ, giá và sức chứa hàng ngày cho sản phẩm |
| `PATCH` | `/api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}` | `X-User-Id: <id>` | Cập nhật giá và giới hạn suất bán của biến thể |
| `DELETE` | `/api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}` | `X-User-Id: <id>` | Xóa mềm biến thể sản phẩm |

### 5.3. Monitoring & Interactive Documentation

- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3.0 Specification**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **Health Check Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Postman Collection**: File kiểm thử mẫu tại [`postman/catalog.json`](postman/catalog.json)

---

## 6. Kiểm thử & Tiêu chuẩn chất lượng (Testing & Quality Commands)

Dự án áp dụng chuẩn kiểm thử nghiêm ngặt. Toàn bộ mã nguồn phải vượt qua 100% test cases và kiểm tra định dạng trước khi release hoặc tạo PR:

```bash
cd services/freshflow-api

# 1. Chạy toàn bộ bộ kiểm thử tự động (Unit, Integration, Migration, Controller, Performance):
mvn clean test

# 2. Chạy một lớp test cụ thể:
mvn -Dtest=CatalogControllerUnitTest test
mvn -Dtest=CatalogQueryPerformanceIntegrationTest test

# 3. Tự động chuẩn hóa định dạng mã nguồn theo Google Java Format (Spotless):
mvn spotless:apply

# 4. Kiểm tra tuân thủ định dạng mà không sửa đổi file:
mvn spotless:check

# 5. Chạy toàn bộ quy trình xác minh (Build, Spotless check, Testing):
mvn verify
```

---

## 7. Giới hạn đã biết của phiên bản v0.1 (Known Limitations)

Phiên bản **Catalog v0.1** tập trung hoàn thiện module Danh mục sản phẩm (Catalog). Các chức năng sau thuộc phạm vi lộ trình các tuần tiếp theo:

1. **Cơ chế xác thực (Authentication/Authorization)**:
   - Hiện tại quyền Merchant đang được định danh tạm thời qua header HTTP `X-User-Id`. Cơ chế bảo mật hoàn chỉnh với Spring Security, JWT token và phân quyền RBAC (Customer, Merchant, Admin, Driver) sẽ được tích hợp trong Sprint 3 (Week 3).
2. **Quản lý tồn kho động (Dynamic Inventory & Capacity)**:
   - Sức chứa hàng ngày (`dailyCapacityDefault`) đang phản ánh hạn mức chế biến tĩnh của cửa hàng. Khả năng tự động trừ kho thời gian thực khi có đơn đặt hàng mới sẽ được liên kết trong Sprint Order & Checkout.
3. **Chính sách khuyến mãi & Tuỳ chọn nâng cao (Modifiers/Toppings Matrix)**:
   - v0.1 hỗ trợ các biến thể kích cỡ chính (`STANDARD`, `M`, `L`). Nhóm topping/tuỳ chọn tuỳ biến linh hoạt (modifiers group) sẽ được mở rộng trong phiên bản tiếp theo.
4. **Hạ tầng phân tán nâng cao**:
   - Chưa triển khai Redis caching và Kafka message streaming vì hệ thống đang vận hành tối ưu ở dạng Modular Monolith với PostgreSQL Functional Index và Batch Fetching.
