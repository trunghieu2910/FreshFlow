# Changelog

All notable changes to the **FreshFlow** platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-09-13

### Catalog v0.1 Release

First MVP release delivering the complete Catalog vertical slice: multi-store data model, PostgreSQL migrations, Customer browsing REST APIs, Merchant catalog administration APIs, OpenAPI 3.0/Swagger documentation, Postman collection, batch fetching performance optimizations, and comprehensive test suite.

### Added

#### Database & Migrations (Flyway V1 - V5)
- **V1 (`V1__init_schema.sql`)**: Core schema creation for `users`, `stores`, `store_categories`, `products`, and `product_variants`.
- **V2 (`V2__add_foreign_keys_and_constraints.sql`)**: Foreign keys with cascading rules and uniqueness constraints (`uk_product_variants_product_name`, `uk_stores_slug`, `uk_users_email`).
- **V3 (`V3__seed_initial_catalog.sql`)**: Initial merchant account, demo store, and base categories.
- **V4 (`V4__add_catalog_indexes.sql`)**: Composite B-tree indexes for fast catalog lookups:
  - `idx_products_store_category` on `(store_id, store_category_id)`
  - `idx_products_store_active_price` on `(store_id, is_active, base_price)`
- **V5 (`V5__seed_realistic_catalog_and_search_indexes.sql`)**:
  - PostgreSQL Functional B-tree index `idx_products_store_active_lower_name` on `(store_id, is_active, LOWER(name))` for case-insensitive keyword search.
  - Realistic F&B demo dataset: 4 categories (Coffee, Fruit Tea, Desserts, Toppings), 36 products, and 57 variants with pricing in VND and daily capacity defaults.

#### Customer Browsing REST API
- `GET /api/v1/stores/{storeId}/products`:
  - Pagination (`page`, `size`) and multi-field sorting (`price,asc`, `price,desc`, `name,asc`, etc.).
  - Category filtering (`categoryId`).
  - Price range filtering (`minPrice`, `maxPrice`).
  - Availability filtering (`availableOnly=true/false`).
  - Case-insensitive keyword search (`search`).
- `GET /api/v1/stores/{storeId}/products/{productId}`:
  - Detailed product view with variant matrix and calculated availability status.

#### Merchant Management REST API
- `POST /api/v1/merchant/stores/{storeId}/products`: Create new product under a store with field validation (`name`, `storeCategoryId`, etc.).
- `PATCH /api/v1/merchant/stores/{storeId}/products/{productId}`: Update product attributes (name, description, active status, store category).
- `DELETE /api/v1/merchant/stores/{storeId}/products/{productId}`: Soft delete product (`deleted_at`), cascading deactivation.
- `POST /api/v1/merchant/stores/{storeId}/products/{productId}/variants`: Add product variant (name, size `STANDARD`/`M`/`L`, price, inventory mode `MADE_TO_ORDER`/`PRE_PACKAGED`, daily capacity default).
- `PATCH /api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}`: Update variant price and capacity.
- `DELETE /api/v1/merchant/stores/{storeId}/products/{productId}/variants/{variantId}`: Soft delete product variant.

#### Application & Security
- **Store Ownership Enforcement**: `CatalogAccessService` validates merchant ownership against `X-User-Id` header (`403 STORE_ACCESS_DENIED`, `400 ACTOR_REQUIRED`).
- **Standardized Error Handling**: `ApiErrorResponse` (`code`, `message`, `path`, `timestamp`) and `ApiExceptionHandler` intercepting validation errors, business rule violations, and not found conditions.
- **Batch Fetching Optimization**: Configured `@BatchSize(size = 50)` on `Product.variants` and Hibernate `default_batch_fetch_size=50`, eliminating N+1 queries during catalog pagination.

#### Documentation & Integration
- **OpenAPI 3.0 & Swagger UI**: Available at `/swagger-ui/index.html` and raw JSON spec at `/api-docs`.
- **Postman Collection**: `postman/catalog.json` covering end-to-end flows, happy paths, pagination, sorting, search, negative validation, and access denial test cases.
- **Automated Test Suite**: 151 test cases passing with 100% success rate, spanning unit tests (BDD naming), JPA migration tests, MockMvc controller slice tests, query performance verification, and domain calculator tests.

### Changed
- **Package Refactoring**:
  - Relocated `CatalogController` to `com.freshflow.api.catalog.api.controller` according to the modular monolith package boundary specification.
  - Relocated `CatalogAccessService` from `application.exception` to `com.freshflow.api.catalog.application` to separate domain services from error types.
- **Version Bump**: Bumped `freshflow-api` Maven artifact version from `0.0.1-SNAPSHOT` to `0.1.0`.
