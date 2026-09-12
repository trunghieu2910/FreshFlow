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


