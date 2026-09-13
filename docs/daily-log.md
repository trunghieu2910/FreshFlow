# FreshFlow Daily Log

This log records planned work, actual outcomes, evidence, blockers and next actions. Each entry should reference a backlog task and should remain concise enough to review at the end of the day.

## Daily log format

| Field | Required content |
|---|---|
| Date | Local working date, using `YYYY-MM-DD` |
| Task | Backlog task ID and task name |
| Goal | Intended outcome for the session |
| Completed | Work that was actually finished |
| Evidence | Commands, screenshots, links, test results or commit |
| Blockers | Anything preventing completion; write `None` when clear |
| Next action | The next concrete step |

## Entry — FF-01-05-2

**Date:** `2026-08-22`
**Task:** `FF-01-05-2 — Set up issue/backlog workflow`
**Priority:** `Must`
**Area:** `Git`

### Goal

Set up a repeatable GitHub workflow so every future FreshFlow task has acceptance criteria, verification evidence, Definition of Done and a completion report.

### Completed

- Created a task issue template at `.github/ISSUE_TEMPLATE/01-task.md`.
- Created a bug report template at `.github/ISSUE_TEMPLATE/02-bug-report.md`.
- Created `.github/ISSUE_TEMPLATE/config.yml` with blank issues disabled and documentation links.
- Created `.github/PULL_REQUEST_TEMPLATE.md` with acceptance, verification, scope, security and documentation checklists.
- Prepared this daily log structure at `docs/daily-log.md`.

### Evidence

The following local checks were completed:

```text
task template: OK
bug template: OK
issue config: OK
PR template: OK
```

The issue configuration uses standard YAML indentation. The pull request template is located directly under `.github`, while issue templates are located under `.github/ISSUE_TEMPLATE`.

### Blockers

GitHub CLI is not installed in the local Git Bash environment. Labels and issue/PR sample verification will therefore be completed through the GitHub web interface or after installing an authenticated GitHub CLI.

### Next action

Create or verify the `Must`, `Should` and `Stretch` labels in the `trunghieu2910/FreshFlow` repository, then create one issue using the task template and prepare a draft PR using the pull request template.

## Reusable entry template

### Entry — YYYY-MM-DD

**Task:** `FF-XX-XX-X — Task name`
**Priority:** `Must` / `Should` / `Stretch`
**Area:** `Backend` / `Database` / `Infrastructure` / `Web` / `Android` / `Git` / `Documentation` / `Quality`

#### Goal

<!-- State the intended outcome. -->

#### Completed

<!-- Record only work actually completed. -->

#### Evidence

```text
<!-- Commands, results, links or commit SHA. -->
```

#### Blockers

<!-- Write None when there is no blocker. -->

#### Next action

<!-- State one or more concrete next steps. -->
## Entry — FF-01-05-2 verification evidence

### Goal

Verify the GitHub issue workflow through a real temporary issue and priority labels.

### Completed

- Created the `Must`, `Should` and `Stretch` labels on GitHub.
- Created issue [#1 — Verify FreshFlow issue workflow](https://github.com/trunghieu2910/FreshFlow/issues/1 ) from the FreshFlow task template.
- Confirmed that the issue contains task summary, scope, acceptance criteria, verification plan and Definition of Done.
- Confirmed that issue #1 has the `Must` label.

### Evidence

- Issue: https://github.com/trunghieu2910/FreshFlow/issues/1
- Workflow commit on main: `90007e4`
- GitHub repository: https://github.com/trunghieu2910/FreshFlow

### Blockers

None.

### Next action

Create a draft pull request from `chore/verify-github-workflow` to verify automatic loading of `.github/PULL_REQUEST_TEMPLATE.md`.

## Entry — FF-02-04-1 (kèm DB-03-A)

**Date:** `2026-09-10`
**Task:** `FF-02-04-1 — Thêm pagination, sorting, search và filter availability/variant cho catalog (kèm DB-03-A)`
**Priority:** `Must`
**Area:** `Backend` / `REST` / `Database`

### Goal

Thêm pagination, sorting, search và filter availability/variant cho catalog; đảm bảo chỉ catalog active được query và mua; tính toán công suất ngày (daily capacity) và trả về `CAPACITY_EXHAUSTED` khi hết suất.

### Completed

- Tạo tài liệu SQL tham chiếu chuẩn tại `docs/database/03-catalog-queries.sql` (DB-03-A) với đầy đủ truy vấn active join, search, size filter (M/L/STANDARD), inventory mode, daily capacity và pagination.
- Tạo DTO `ProductFilterCriteria` và lớp `ProductSpecifications` sử dụng Spring Data JPA Specification.
- Nâng cấp `ProductRepository` kế thừa `JpaSpecificationExecutor<Product>`.
- Xây dựng `CatalogCapacityService` truy vấn công suất ngày từ `inventory_capacity_records` với fallback sang `dailyCapacityDefault`.
- Nâng cấp `CatalogService.listProductsByStore` trả về `Page<ProductCatalogDto>` có tích hợp snapshot công suất và trạng thái `CAPACITY_EXHAUSTED`.
- Nâng cấp REST Controller `GET /api/v1/stores/{storeId}/products` với các query param (`search`, `storeCategoryId`, `size`, `variantSize`, `inventoryMode`, `availableOnly`, `Pageable`).
- Viết 7 integration test trong `ProductSpecificationIntegrationTest`, 3 test trong `CatalogCapacityServiceIntegrationTest`, và bổ sung 5 integration test trong `ProductControllerIntegrationTest`.

### Evidence

```text
[INFO] Running com.freshflow.api.catalog.infrastructure.persistence.ProductSpecificationIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.application.CatalogCapacityServiceIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.api.controller.ProductControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 114, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Blockers

None.

### Next action

Hoàn thành task FF-02-04-2 và DB-03-B.

## Entry — FF-02-04-2 (kèm DB-03-B)

**Date:** `2026-09-11`
**Task:** `FF-02-04-2 — Tích hợp OpenAPI và Postman collection (kèm DB-03-B)`
**Priority:** `Must`
**Area:** `Docs` / `OpenAPI` / `Postman` / `Database`

### Goal

Tích hợp tài liệu OpenAPI 3.0, Swagger UI trực quan, tạo bộ Postman collection hoàn chỉnh (`postman/catalog.json`) với đầy đủ happy và error cases; tạo migration Flyway V4 bổ sung B-tree indexes (`DB-03-B`) tối ưu hóa truy vấn phân trang, tìm kiếm và lọc danh mục sản phẩm.

### Completed

- **DB-03-B (B-Tree Index Migration)**: Tạo migration Flyway `V4__add_catalog_performance_indexes.sql` bổ sung các B-tree index:
  - `idx_products_store_active_name`: tối ưu duyệt catalog theo cửa hàng và sắp xếp tên.
  - `idx_products_store_active_created_at`: tối ưu sắp xếp theo ngày tạo.
  - `idx_products_store_category_active`: tối ưu lọc theo danh mục sản phẩm.
  - `idx_store_categories_store_active`: tối ưu join danh mục active.
  - `idx_product_variants_active_size` và `idx_product_variants_active_inventory_mode`: tối ưu lọc biến thể theo kích cỡ và chế độ kho.
- **OpenAPI Configuration**: Tạo `OpenApiConfig` định nghĩa OpenAPI bean, thông tin dự án FreshFlow MVP, schema security `bearerAuth` (JWT placeholder) và `merchantUserIdAuth` (header `X-User-Id`).
- **DTO & Schema Documentation**: Bổ sung annotation `@Schema` chi tiết cho `ProductCatalogDto`, `ProductVariantDto`, `CapacityDto`, `AvailabilityStatus`, `CreateProductRequest`, `UpdateProductRequest`, `CreateProductVariantRequest`, `UpdateProductVariantRequest`, và `ApiErrorResponse`.
- **Controller Documentation**: Bổ sung `@Operation`, `@ApiResponse`, `@Parameter`, `@SecurityRequirement` cho toàn bộ 11 endpoint trong `CatalogController`.
- **Postman Collection**: Xây dựng file `postman/catalog.json` (chuẩn Postman Collection v2.1.0) chia 3 folder (`01 - Public Catalog`, `02 - Merchant Product Management`, `03 - Merchant Variant Management`) với 18 request mẫu kèm test scripts tự động kiểm tra status code.
- **Automated Verification**: Viết integration test `CatalogOpenApiIntegrationTest` kiểm chứng endpoint `/api-docs` và `/swagger-ui/index.html`. Cập nhật `CatalogSeedMigrationTest` cho migration version 4.
- **Test Suite**: Chạy toàn bộ bộ test `mvn test` đạt **116/116 test passed**, format mã nguồn đạt chuẩn Google Java Format với `mvn spotless:apply`.

### Evidence

```text
[INFO] Running com.freshflow.api.catalog.api.controller.CatalogOpenApiIntegrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.domain.CatalogSeedMigrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.infrastructure.persistence.ProductSpecificationIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.api.controller.ProductControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 116, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Blockers

None.

### Next action

Hoàn thành task FF-02-05-1.

## Entry — FF-02-05-1

**Date:** `2026-09-11`
**Task:** `FF-02-05-1 — Viết unit/controller test catalog`
**Priority:** `Must`
**Area:** `Quality` / `Backend` / `Testing`

### Goal

Xây dựng bộ kiểm thử chuyên sâu (Catalog test suite) gồm ít nhất 20 test cases có tên rõ ràng theo chuẩn BDD; tập trung kiểm thử logic phân quyền (CatalogAccessService), quy tắc nghiệp vụ biến thể (CatalogVariantService), và các mã trạng thái/validation của Controller (CatalogControllerUnitTest).

### Completed

- **Unit Test Phân quyền (`CatalogAccessServiceTest`)**: 9 test cases bao quát logic yêu cầu `actorUserId`, kiểm tra chủ sở hữu cửa hàng (`STORE_ACCESS_DENIED`), tìm kiếm store/product (`STORE_NOT_FOUND`, `PRODUCT_NOT_FOUND`).
- **Unit Test Biến thể (`CatalogVariantServiceTest`)**: 12 test cases bao quát quy ước đặt tên và kích cỡ STANDARD/M/L (`STANDARD_SIZE_INVALID`), kiểm tra trùng tên biến thể (`VARIANT_DUPLICATE`), kiểm tra giá âm, cập nhật trường dữ liệu, soft delete và truy vấn danh sách biến thể.
- **Unit/Slice Test Controller (`CatalogControllerUnitTest`)**: 11 test cases sử dụng MockMvc + `@MockitoBean` kiểm thử toàn bộ endpoint của `CatalogController`: status `200 OK`, `201 Created` kèm `Location`, `204 No Content`, `400 Bad Request` validation, `403 Forbidden` access denied, `404 Not Found`, `409 Conflict` duplicate SKU, và phân trang/lọc tham số.
- **Kiểm thử tự động toàn diện**: Tổng số test của dự án tăng từ **116** lên **148** test (bổ sung 32 test mới, vượt chỉ tiêu tối thiểu 20 test). 100% test cases đều pass sạch sẽ (`BUILD SUCCESS`).
- **Định dạng code**: Áp dụng định dạng Google Java Format qua `mvn spotless:apply`.

### Evidence

```text
[INFO] Running com.freshflow.api.catalog.application.exception.CatalogAccessServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.application.CatalogVariantServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.api.controller.CatalogControllerUnitTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 148, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Blockers

None.

### Next action

Chuyển sang task tiếp theo trong Backlog: FF-02-05-2 (Kiểm tra JPA query và dữ liệu seed — Lazy loading, index, query correctness).

## Entry — FF-02-05-2

**Date:** `2026-09-11`
**Task:** `FF-02-05-2 — Kiểm tra JPA query và dữ liệu seed (Lazy loading, index, query correctness)`
**Priority:** `Should`
**Area:** `Database` / `Backend` / `Performance`

### Goal

Kiểm tra và tối ưu hóa truy vấn JPA trong API Catalog, loại bỏ triệt để vấn đề N+1 Query đối với các tập hợp quan hệ lười (Lazy collection `Product.variants`), bổ sung chỉ mục tối ưu cho tìm kiếm từ khóa không phân biệt hoa thường (`LOWER(name)`), và mở rộng bộ dữ liệu mẫu (seed data) đạt hơn 30 sản phẩm F&B thực tế để kiểm thử tải, phân trang và truy vấn.

### Completed

- **Khử triệt để N+1 Query bằng Batch Fetching:**
  - Cấu hình `@BatchSize(size = 50)` trên tập hợp `variants` trong thực thể [`Product.java`](file:///d:/FreshFlow/services/freshflow-api/src/main/java/com/freshflow/api/catalog/domain/Product.java).
  - Cấu hình thuộc tính toàn cục `spring.jpa.properties.hibernate.default_batch_fetch_size=50` trong [`application.properties`](file:///d:/FreshFlow/services/freshflow-api/src/main/resources/application.properties).
  - Nhờ cơ chế batch fetch, khi nạp 20 sản phẩm trên một trang, Hibernate chỉ thực thi đúng 1 câu lệnh `SELECT ... WHERE product_id IN (?, ..., ?)` gom toàn bộ biến thể, thay vì phát sinh 20 câu lệnh riêng rẽ.
- **Tạo Flyway Migration `V5__seed_realistic_catalog_and_search_indexes.sql`:**
  - Tạo Functional Index `idx_products_store_active_lower_name` trên `products (store_id, is_active, LOWER(name))`.
  - Mở rộng 4 danh mục F&B mới: `Coffee`, `Fruit Tea`, `Desserts`, `Toppings`.
  - Thêm 34 sản phẩm F&B thực tế (tổng cộng 36 sản phẩm trong cửa hàng `FreshFlow Demo Kitchen`), bao gồm 57 biến thể (M, L, STANDARD), giá tiền VND thực tế (10,000 - 55,000 VND), đường dẫn ảnh demo và sức chứa hàng ngày `daily_capacity_default` (25 - 200 suất).
  - Đảm bảo tính idempotent an toàn khi chạy lại với mệnh đề `ON CONFLICT DO NOTHING`.
- **Cập nhật & Xây dựng Test Suite:**
  - Cập nhật [`CatalogSeedMigrationTest.java`](file:///d:/FreshFlow/services/freshflow-api/src/test/java/com/freshflow/api/catalog/domain/CatalogSeedMigrationTest.java) xác thực migration version = 5, tổng số sản phẩm $\ge 30$, biến thể $\ge 40$, danh mục $\ge 6$, và chỉ mục `idx_products_store_active_lower_name` tồn tại.
  - Viết mới [`CatalogQueryPerformanceIntegrationTest.java`](file:///d:/FreshFlow/services/freshflow-api/src/test/java/com/freshflow/api/catalog/application/CatalogQueryPerformanceIntegrationTest.java) kích hoạt Hibernate Statistics, kiểm chứng việc tải trang 20 sản phẩm chỉ thực thi tối đa 5-6 SQL statements (chứng minh không có N+1 query), kiểm tra tìm kiếm hoa/thường không phân biệt (`oolong` vs `OOLONG`), và kiểm tra phân trang liên tục giữa các trang mà không bị trùng lặp.
  - Tinh chỉnh assertion trong [`ProductControllerIntegrationTest.java`](file:///d:/FreshFlow/services/freshflow-api/src/test/java/com/freshflow/api/catalog/api/controller/ProductControllerIntegrationTest.java) để tương thích chính xác với catalog 30+ sản phẩm.
- **Định dạng & Kiểm thử:**
  - Toàn bộ 151 test cases đều PASS 100% (`BUILD SUCCESS`).
  - Áp dụng chuẩn Google Java Format qua `mvn spotless:apply`.

### Evidence

```text
[INFO] Running com.freshflow.api.catalog.application.CatalogQueryPerformanceIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.freshflow.api.catalog.domain.CatalogSeedMigrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Blockers

None.

### Next action

Đánh dấu hoàn thành task FF-02-05-2 trong Backlog và chuẩn bị cho Sprint tiếp theo.

## Entry — FF-02-06-2

**Date:** `2026-09-13`
**Task:** `FF-02-06-2 — Đóng gói release Catalog v0.1`
**Priority:** `Must`
**Area:** `Refactor` / `Documentation` / `Release`

### Goal

Chuẩn hóa package boundaries, nâng cấp phiên bản theo Semantic Versioning (`0.1.0`), tài liệu hóa toàn diện hệ thống (README.md, CHANGELOG.md), đóng gói release Catalog v0.1 với Git Tag và Branch release, đồng thời kiểm chứng tính toàn vẹn thông qua bộ kiểm thử tự động và clean test.

### Completed

- **Refactor Package Boundaries:**
  - Di chuyển `CatalogController` sang `com.freshflow.api.catalog.api.controller` đồng bộ với cấu trúc package test và chuẩn Modular Monolith.
  - Di chuyển service kiểm tra phân quyền `CatalogAccessService` từ package lỗi thời `application.exception` sang đúng package nghiệp vụ `com.freshflow.api.catalog.application`.
  - Cập nhật tương ứng toàn bộ file kiểm thử và import (`CatalogAccessServiceTest`, `CatalogVariantService`, `CatalogControllerUnitTest`).
- **Semantic Versioning & Changelog:**
  - Nâng version artifact Maven trong `pom.xml` từ `0.0.1-SNAPSHOT` lên `0.1.0` (Catalog v0.1.0 release).
  - Soạn thảo tài liệu `CHANGELOG.md` chuẩn Keep a Changelog v1.1 ghi nhận chi tiết 5 Flyway migrations, API Customer/Merchant, tối ưu chỉ mục functional index và batch fetching.
- **Tài liệu hóa dự án (README.md nâng cao):**
  - Bổ sung hướng dẫn cài đặt & môi trường (JDK 21, Docker Compose, PostgreSQL 16).
  - Bảng danh mục đầy đủ các REST endpoints (Customer browsing & Merchant management kèm query parameters và header xác thực `X-User-Id`).
  - Hướng dẫn lệnh kiểm thử & chuẩn hóa mã nguồn (`mvn clean test`, `mvn spotless:apply`, `mvn verify`).
  - Mô tả rõ các giới hạn đã biết của bản v0.1 (Known Limitations) về Authentication (chờ JWT Sprint 3) và Dynamic inventory (chờ Order Sprint).
- **Kiểm thử & Định dạng:**
  - Kiểm tra chuẩn định dạng Google Java Format bằng `mvn spotless:check` thành công.
  - Toàn bộ 151 test cases chạy sạch sẽ và PASS 100% (`BUILD SUCCESS`).
- **Git Release Tag & Branch:**
  - Tạo Git branch `catalog-v0.1` và annotated tag `v0.1`, `catalog-v0.1`.
- **Cập nhật Backlog:**
  - Đánh dấu hoàn thành Task FF-02-06-2 (`Done`) trong `plan/backlog-freshflow-mvp-12-tuan-updated.xlsx`.

### Evidence

```text
[INFO] --- spotless:2.43.0:check (default-cli) @ freshflow-api ---
[INFO] Spotless.Java is keeping 72 files clean - 0 needs changes to be clean
...
[INFO] Results:
[INFO] Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Blockers

None.

### Next action

Hoàn thành task FF-02-07-1.

## Entry — FF-02-07-1

**Date:** `2026-09-13`
**Task:** `FF-02-07-1 — Ôn JPA/REST và viết retrospective`
**Priority:** `Must`
**Area:** `Review` / `Documentation`

### Goal

Tổng kết Sprint 2 (Catalog Vertical Slice), thực hiện kỹ thuật Active Recall và biên soạn tài liệu retrospective tuần W02, phân tích sâu 5 chủ đề phỏng vấn kỹ thuật cốt lõi (DTO vs Entity, Server price calculation, N+1 query, Pagination, Flyway migration & 2-tier validation), và xây dựng bài kiểm tra Self-Quiz đánh giá kiến thức đạt $\ge 80\%$.

### Completed

- **Tài liệu hóa Retrospective Tuần 2 (`docs/reviews/weekly-review-w02.md`):**
  - Đánh giá toàn diện các thành tựu đạt được (What went well): Kiến trúc Modular Monolith, Flyway migrations V1-V5, xử lý N+1 bằng Batch Fetching, test suite 151 test cases pass 100%.
  - Rút kinh nghiệm (What could be improved): Tuân thủ chặt chẽ ranh giới package boundary từ sớm, chuẩn bị thay thế mock auth bằng JWT token.
  - Thiết lập Action items cụ thể cho Sprint 3 (Authentication, JWT/RBAC, Merchant React Web).
- **Phân tích chuyên sâu 5 câu hỏi phỏng vấn kỹ thuật:**
  - Lập luận vì sao KHÔNG NÊN trả trực tiếp JPA Entity ra REST API (chống Over-fetching, Mass Assignment, LazyInitializationException, Infinite recursion).
  - Lập luận vì sao SERVER PHẢI tính toán lại tổng tiền và đơn giá (Zero Trust client, dữ liệu giá biến động thời gian thực, lưu vết bất biến bằng `OrderItemSnapshot`).
  - Phân tích chi tiết lỗi N+1 Query và cơ chế Batch Fetching `@BatchSize(50)`.
  - So sánh phân trang Offset-based vs Keyset-based và ứng dụng Functional Index `LOWER(name)`.
  - Quy tắc an toàn khi viết Flyway migration trên Production và kiến trúc Validation 2 tầng.
- **Biên soạn bài kiểm tra Self-Quiz (Active Recall):**
  - Xây dựng 10 câu hỏi trắc nghiệm & tình huống có đáp án chi tiết và bảng thang điểm chuẩn mực.
- **Cập nhật Backlog:**
  - Đánh dấu hoàn thành Task FF-02-07-1 (`Done`) trong `plan/backlog-freshflow-mvp-12-tuan-updated.xlsx`.

### Evidence

Tài liệu hoàn chỉnh tại: [`docs/reviews/weekly-review-w02.md`](file:///d:/FreshFlow/docs/reviews/weekly-review-w02.md)

### Blockers

None.

### Next action

Chuyển sang task tiếp theo trong Backlog: FF-02-07-2 (Chuẩn bị React architecture cho Merchant Web).

## Entry — FF-02-07-2

**Date:** `2026-09-13`
**Task:** `FF-02-07-2 — Chuẩn bị React architecture`
**Priority:** `Must`
**Area:** `Planning` / `Frontend Architecture`

### Goal

Thiết kế kiến trúc frontend tiêu chuẩn cho cổng thông tin quản trị Merchant Web Portal (`clients/freshflow-web`) theo mô hình Feature-Based Architecture (Screaming Architecture), xây dựng Bản đồ định tuyến (Route Map) và Cây phân cấp thành phần (Component Tree) gắn kết trực tiếp với Catalog Backend API, đưa ra các quyết định công nghệ (Axios vs Fetch, TanStack Query v5 vs SWR), và tuân thủ các quy tắc từ Web Interface Guidelines cùng định hướng tương thích với React Native Mobile.

### Completed

- **Tài liệu hóa Kiến trúc Web Hoàn chỉnh (`docs/web-architecture.md`):**
  - **Cấu trúc Thư mục Feature-Based:** Quy hoạch rõ ràng các tầng `src/app`, `src/features/catalog` (api, components, pages, types, utils), `src/features/stores`, `src/components/ui` (Design system tái sử dụng), `src/lib`, `src/types`, `src/hooks`.
  - **Bản đồ Định tuyến & Ma trận Phụ thuộc API (No Orphan Screens):** Ánh xạ 1:1 từng route với use case nghiệp vụ, HTTP method, API backend và quyền hạn:
    - `/stores`: Lựa chọn cửa hàng làm việc.
    - `/stores/:storeId/catalog`: Quản lý thực đơn số, phân trang, lọc size, danh mục, search debounce, và kiểm tra công suất.
    - `/stores/:storeId/catalog` *(Modal)*: Thêm/Sửa thông tin món ăn (`POST`/`PATCH`).
    - `/stores/:storeId/catalog/:productId`: Chi tiết món ăn và quản lý toàn diện các biến thể (M/L/STANDARD, giá, công suất ngày).
  - **Cây Phân cấp Thành phần (Component Tree):** Mô hình hóa Provider Tree và Catalog Component Hierarchy trực quan bằng sơ đồ Mermaid diagrams.
  - **Quyết định Kiến trúc Công nghệ (ADRs):**
    - *ADR 01 (HTTP Client):* Chọn **Axios** nhờ hỗ trợ sẵn Interceptors (tự động gắn `X-User-Id`), chuẩn hóa tập trung `ApiErrorResponse`, timeout và hủy request cũ khi search debounce (`AbortController`).
    - *ADR 02 (Query Strategy):* Chọn **TanStack React Query v5** quản lý Server State (cache 5 phút, `placeholderData: keepPreviousData` chống giật lag phân trang, `useMutation` với Optimistic UI updates). Phối hợp **Zustand** cho Global UI State và **React Hook Form + Zod** cho Form Validation.
  - **Tuân thủ Tiêu chuẩn Giao diện (Web Interface Guidelines):** Hỗ trợ điều hướng bàn phím WCAG 2.1 AA (Tab/Esc), Focus ring rõ nét, Skeleton loading thay cho spinner quay tròn, field-level error mapping cho form validation.
  - **Sẵn sàng Chia sẻ với Mobile App (React Native Alignment):** Đồng bộ hóa các TypeScript DTOs (`ProductCatalogDto`, `ProductVariantDto`, `CapacitySnapshot`) sẵn sàng dùng chung giữa Web và Expo / React Native Mobile sau này.

### Evidence

Tài liệu hoàn chỉnh tại: [`docs/web-architecture.md`](file:///d:/FreshFlow/docs/web-architecture.md)

### Blockers

None.

### Next action

Chuyển sang task tiếp theo trong Backlog: FF-01-07-1 (Ôn và kiểm tra kiến thức tuần 1).

## Entry — FF-01-07-1

**Date:** `2026-09-13`
**Task:** `FF-01-07-1 — Ôn và kiểm tra kiến thức tuần 1`
**Priority:** `Must`
**Area:** `Review` / `Knowledge Retention`

### Goal

Áp dụng phương pháp Active Recall và kỹ thuật Feynman để ôn tập, hệ thống hóa toàn bộ kiến thức nền tảng của Tuần 1 (Khởi động, Domain F&B, Order State Machine, Clean OOP/DDD, Database boundary và Môi trường Docker/PostgreSQL), tự trả lời 15 câu hỏi kỹ thuật chuyên sâu, tự chấm điểm đạt $\ge 80\%$, và lập danh sách điểm cần củng cố đưa vào Tuần 2. Tuyệt đối không viết code tính năng mới.

### Completed

- **Tài liệu hóa Active Recall Tuần 1 (`docs/weekly-review/w01.md` và `docs/reviews/weekly-review-w01.md`):**
  - **Nhóm 1 (REST & HTTP):** Phân tích sự khác nhau giữa `PUT` (thay thế toàn bộ) và `PATCH` (cập nhật cục bộ); phân loại Idempotent và Safe methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`); phân biệt 5 mã trạng thái HTTP kinh điển (`400`, `401`, `403`, `404`, `409`); giải thích tính phi trạng thái (Statelessness) và rủi ro khi lưu session trong RAM server.
  - **Nhóm 2 (OOP & Clean Domain):** Phân biệt cốt lõi Entity (định danh bằng ID) vs Value Object (bất biến, định danh bằng giá trị thuộc tính); giải thích lý do ưu tiên Java `record` (zero boilerplate, immutable by default, thread-safe); phân tích lợi ích bất biến trong hệ thống tính toán giá và kế toán.
  - **Nhóm 3 (Order State Machine):** Mô hình hóa vòng đời đơn hàng F&B bằng sơ đồ Mermaid (`CREATED` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY_FOR_PICKUP` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`); giải thích Guard Conditions và Invariants; chứng minh vì sao khách hàng tuyệt đối không được hủy đơn khi đang `OUT_FOR_DELIVERY`; chuẩn hóa xử lý lỗi chuyển trạng thái trái phép (`ORDER_ILLEGAL_STATE_TRANSITION` kèm HTTP `409 Conflict`).
  - **Nhóm 4 (Database Boundaries):** Định nghĩa kiến trúc Modular Monolith và so sánh ưu/nhược điểm với Microservices/Spaghetti Monolith; giải thích quy tắc ranh giới package: vì sao module `order` không được `@ManyToOne Product` trực tiếp mà chỉ lưu `Long productId`; phân tích giá trị của việc tách biệt bảng cấu hình danh mục (`products`) và bảng snapshot lịch sử bất biến (`order_item_snapshots`).
  - **Nhóm 5 (Môi trường & Công nghệ):** Lợi ích của Docker Compose trong việc đồng nhất môi trường và khởi tạo tức thì; vai trò của Flyway Migration và lý do cấm hoàn toàn `hibernate.ddl-auto=update` trên môi trường Production.
- **Tự Chấm Điểm & Đánh giá Năng lực (Self-Scoring):**
  - Đạt điểm số **14.5 / 15** (tương đương **96.7%**, vượt xa chỉ tiêu tối thiểu $\ge 80\%$).
- **Lập Danh sách Củng cố Kiến thức chuyển sang Tuần 2:**
  - Ghi nhận 3 trọng tâm kỹ thuật chuyên sâu để áp dụng trong Tuần 2: Xử lý triệt để N+1 Query bằng Batch Fetching, xây dựng JPA Specification lọc động nhiều tiêu chí, và tích hợp OpenAPI 3.0 / Swagger UI.
- **Tuân thủ kỷ luật phát triển:**
  - Không code tính năng mới, dành trọn vẹn thời gian củng cố nền tảng kiến thức.

### Evidence

Tài liệu hoàn chỉnh tại: [`docs/weekly-review/w01.md`](file:///d:/FreshFlow/docs/weekly-review/w01.md)

### Blockers

None.

### Next action

Đánh dấu hoàn thành task FF-01-07-1 trong Backlog và sẵn sàng chuyển giao các kỹ thuật vào Sprint tiếp theo.






