# ADR-003: Chuyển đổi từ Kiến trúc 4 lớp sang Kiến trúc 3 lớp tinh gọn cho FreshFlow MVP

- **Status:** Accepted
- **Date:** 2026-09-28
- **Decision owners:** FreshFlow project team
- **Related ADRs:**
  - [`ADR-001-modular-monolith`](./ADR-001-modular-monolith.md)
  - [`ADR-002-risk-first-delivery`](./ADR-002-risk-first-delivery.md)

---

## 1. Context

Trong các tuần đầu phát triển (W01 - W03), FreshFlow backend được tổ chức theo kiến trúc Clean / DDD 4 lớp cho từng module (`catalog`, `order`, `payment`, `delivery`):
1. `api`: REST Controllers, Request DTOs, Response DTOs, Mappers.
2. `application`: Use cases, Commands (`CreateProductCommand`), Queries (`ProductFilterCriteria`), Application Services (`CatalogService`), ReadModels, Custom Exceptions.
3. `domain`: JPA Entities (`Product`, `Store`), Enums, Domain Services (`OrderPricingCalculator`), Value Objects (`Money`).
4. `infrastructure`: Spring Data JPA Repositories (`ProductRepository`), JPA Specifications (`ProductSpecifications`).

### Vấn đề thực tế phát sinh (Pain Points):
1. **Quá nhiều tầng chuyển đổi dữ liệu (Mapping Overhead):**
   - Một thao tác đơn giản (ví dụ: tạo sản phẩm) yêu cầu đi qua:
     `CreateProductRequest` (API) -> `CatalogRequestMapper` -> `CreateProductCommand` (Application) -> `Product` (Entity) -> `CatalogDtoMapper` -> `ProductCatalogDto` (Response).
   - `CreateProductRequest` và `CreateProductCommand` gần như giống hệt nhau về trường dữ liệu, dẫn đến việc bảo trì 2 class song song không đem lại giá trị nghiệp vụ thực tế.
2. **Chi phí sửa đổi cao (High Maintenance Cost):**
   - Khi thêm mới hoặc sửa một thuộc tính (ví dụ: thêm thuộc tính cho Product/Variant), lập trình viên phải cập nhật đồng loạt 5-7 file (Request, Mapper, Command, Service, Entity, DTO, Test).
3. **Áp lực tiến độ 12 tuần MVP:**
   - Theo [ADR-002](./ADR-002-risk-first-delivery.md), mục tiêu hàng đầu là phân phối luồng nghiệp vụ end-to-end (Customer đặt món, Merchant xử lý đơn, Driver giao hàng) ổn định và kiểm soát rủi ro tính đúng đắn (concurrency, transaction boundary, ownership), thay vì sa vào sự phức tạp cấu trúc (structural over-engineering).
4. **Vi phạm ranh giới ngầm:**
   - Thực tế trong quá trình phát triển nhanh, việc tách 4 lớp không ngăn được vi phạm ranh giới module (ví dụ: `OrderService` đã trực tiếp inject `ProductRepository` từ `catalog.infrastructure.persistence`).

---

## 2. Decision

FreshFlow sẽ **chuyển đổi kiến trúc backend từ mô hình 4 lớp sang mô hình 3 lớp tinh gọn chuẩn Spring Boot (3-Tier Architecture)**, đồng thời **bảo toàn nguyên tắc Modular Monolith** theo ADR-001.

### 2.1. Giữ vững ranh giới Module (Package-by-Feature)
Không gom phẳng toàn bộ hệ thống thành một package chung toàn cục (`controller`, `service`, `repository` ở root level) vì điều đó sẽ phá vỡ tính module hoá của ADR-001. Thay vào đó, mỗi module nghiệp vụ vẫn là một đơn vị độc lập cấp cao:
- `com.freshflow.api.catalog`
- `com.freshflow.api.order`
- `com.freshflow.api.payment`
- `com.freshflow.api.delivery`
- `com.freshflow.api.common`

### 2.2. Chuẩn hoá 3 lớp bên trong từng Module

Mỗi module sẽ được tổ chức lại theo 3 lớp chuẩn:

```text
com.freshflow.api.<module>
├── controller/        # Lớp 1: Presentation (REST Controller, OpenAPI contract)
├── dto/               # DTOs (Request DTOs với validation @Valid, Response DTOs, Mappers)
├── service/           # Lớp 2: Business Logic (Services, Transaction orchestration, Business Exceptions)
├── repository/        # Lớp 3: Data Access (Spring Data JPA Repositories, JPA Specifications)
└── model/             # JPA Entities, Enums, Value Objects (hoặc entity/)
```

### 2.3. Loại bỏ lớp Command & Request Mapper trung gian
- Xóa bỏ package `application.command` (`CreateProductCommand`, `UpdateProductCommand`, ...).
- Xóa bỏ `CatalogRequestMapper`.
- `Service` sẽ nhận trực tiếp `Request DTO` từ `Controller`. Việc chuyển đổi từ Request DTO sang JPA Entity sẽ được thực hiện trực tiếp trong `Service` hoặc thông qua static factory method trên Entity/DTO.
- Response mapping được tinh gọn bằng method tiện ích trên `dto` hoặc giữ một `DtoMapper` gọn nhẹ.

### 2.4. Khắc phục và siết chặt ranh giới Module (Boundary Rules)
- Module `order` **tuyệt đối không** được inject `catalog.repository.ProductRepository`.
- Mọi nhu cầu truy vấn hoặc xác thực liên quan đến Catalog từ Order phải đi qua `catalog.service.CatalogService` hoặc `catalog.service.CatalogAccessService`.

---

## 3. Alternatives Considered

### Alternative A: Giữ nguyên kiến trúc 4 lớp DDD
- **Ưu điểm:** Tách biệt triệt để tầng ứng dụng và hạ tầng, phù hợp với hệ thống doanh nghiệp lớn có domain phức tạp.
- **Lý do từ chối:** Quá cồng kềnh đối với quy mô MVP 12 tuần của FreshFlow; gây lãng phí thời gian viết boilerplate code và mapping vô nghĩa cho các nghiệp vụ CRUD cơ bản.

### Alternative B: Phẳng hoá toàn bộ thành 3 lớp toàn cục (Layered Monolith không module)
```text
com.freshflow.api
├── controller/
├── service/
├── repository/
└── model/
```
- **Ưu điểm:** Cấu trúc cực kỳ đơn giản lúc ban đầu.
- **Lý do từ chối:** Vi phạm trực tiếp [ADR-001](./ADR-001-modular-monolith.md). Khi số lượng Entity tăng lên (Store, Product, Variant, Order, Payment, Driver, Delivery...), các package này sẽ trở thành "bãi rác" chứa hàng chục class lẫn lộn, coupling cao và không thể tách thành microservice khi cần.

### Alternative C: Kiến trúc 3 lớp kết hợp Modular Monolith (Lựa chọn được duyệt)
- **Ưu điểm:** Kết hợp được sự tinh gọn, ít boilerplate của mô hình 3 lớp Spring Boot với khả năng kiểm soát ranh giới và phân quyền sở hữu dữ liệu của Modular Monolith.

---

## 4. Consequences

### Positive Consequences
1. **Giảm 40-50% boilerplate code:** Loại bỏ toàn bộ hệ thống Command class và Request Mapper trung gian.
2. **Gia tăng tốc độ phát triển:** Thêm hoặc sửa thuộc tính trong model chỉ cần cập nhật Request -> Entity -> Response.
3. **Tuân thủ chuẩn Spring Boot thông dụng:** Dễ tiếp cận cho các thành viên mới, tận dụng tối đa các annotation chuẩn (`@Service`, `@RestController`, `@Repository`).
4. **Minh bạch ranh giới:** Chấn chỉnh vi phạm inject chéo tầng dữ liệu giữa `order` và `catalog`.

### Negative Consequences
1. **Cần nỗ lực refactor:** Phải di chuyển package, cập nhật imports trên toàn bộ source code và các bài kiểm thử hiện có.
2. **Mất sự cô lập tuyệt đối của Entity:** JPA Entity sẽ được sử dụng trực tiếp trong tầng Service thay vì được bao bọc bởi Domain/Application interface trừu tượng. Đối với quy mô của FreshFlow, chi phí này hoàn toàn xứng đáng để đổi lấy sự đơn giản.

---

## 5. Quy tắc chuyển đổi (Migration Rules)

1. **Bảo toàn dữ liệu & Schema:** Không đổi tên bảng, tên cột, ràng buộc khoá ngoại hay logic migration Flyway.
2. **Bảo toàn API Contract:** Giữ nguyên toàn bộ URL endpoints, HTTP methods, status codes và format JSON payload của Request/Response để không làm ảnh hưởng Frontend và Mobile.
3. **Quy trình di chuyển an toàn:**
   - Bước 1: Refactor `common` và phần Model/Repository của `catalog`.
   - Bước 2: Refactor Service/Controller của `catalog`, xóa Command và Request Mapper.
   - Bước 3: Refactor `order` và khắc phục điểm vi phạm dependency với `catalog`.
   - Bước 4: Chạy kiểm thử hồi quy toàn bộ test suite (`./mvnw clean test`) đảm bảo 100% pass trước khi merge vào nhánh chính.
