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

Chuyển sang task tiếp theo trong Backlog: FF-02-05-1.

