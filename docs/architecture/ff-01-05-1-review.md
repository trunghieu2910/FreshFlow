# FF-01-05-1 — ERD Review and Business Rule Traceability

**Task:** `FF-01-05-1`
**Status:** Approved
**Canonical ERD:** [`docs/architecture/erd.md`](./erd.md)
**Source requirements:** `docs/FF-01-01-1_Software Requirements Specification — FreshFlow MVP.md`
**Source state machine:** `docs/FF-01-02-1_FreshFlow Order State Machine.md`

## 1. Purpose

This document records the review of the first ten FreshFlow business rules against the canonical ERD. The review verifies table coverage, relationship ownership, key constraints, and the boundary between database constraints and application transaction rules.

The ERD is the conceptual and logical database design. PostgreSQL DDL, Flyway migrations, JPA entities, repositories, and REST API implementation are subsequent persistence and application tasks.

## 2. Review result

| Review area | Result | Evidence |
|---|---|---|
| Scope and requirements alignment | PASS | Identity, Store, catalog, ProductVariant, inventory, cart, order and payment concepts are present in the ERD |
| ProductVariant as purchasable unit | PASS | `products` and `product_variants` are separated; OrderItem references the variant |
| Store ownership | PASS | Store-scoped relationships are represented through Store foreign keys and `user_store_roles` |
| Inventory modes | PASS | `inventory_mode`, stock records and capacity records are represented |
| Order snapshots | PASS | Order item snapshot fields are explicitly documented |
| Keys and uniqueness | PASS in design | PK/FK/unique/check requirements are documented in the ERD and must be enforced by the future migration |
| Transaction rules | PASS as application responsibility | Reservation and checkout rules are identified as transactional service behavior, not only static schema rules |
| Review status | APPROVED FOR IMPLEMENTATION PLANNING | No unresolved contradiction was found in the first ten business rules |

## 3. Business-rule traceability

| Rule | Business rule | ERD tables/columns involved | Required constraint or invariant | Owner | Result |
|---|---|---|---|---|---|
| BR-01 | Only active Store, Category assignment, Product and ProductVariant are visible for purchase. | `stores`, `store_categories`, `products`, `product_variants` | Active/status fields must exist; catalog query must filter inactive records. | Catalog module | PASS |
| BR-02 | A Cart and an Order belong to exactly one Store; a Cart cannot mix Stores. | `carts`, `cart_items`, `orders`, `order_items`, `stores` | `store_id` is required on Cart and Order; every item must belong to the same Store through its variant/product ownership. | Cart and Order modules | PASS |
| BR-03 | ProductVariant is the purchasable unit. Products with sizes use multiple variants; products without a size use one `STANDARD` variant with `size = NULL`. | `products`, `product_variants` | `product_variants.product_id` is required; variant identity is unique within a Product; the STANDARD convention must be validated. | Catalog module | PASS |
| BR-04 | Each ProductVariant has its own price, availability, inventory mode and optional auto-accept override. | `product_variants` | Price is positive; inventory mode is restricted to the approved enum; availability is separate from Product status. | Catalog module | PASS |
| BR-05 | Store auto-accept is the default; a ProductVariant may override it. One manual item makes the whole Order wait for Merchant confirmation. | `stores`, `product_variants`, `orders` | Store default and nullable variant override are required; the “one manual item controls the Order” rule belongs to checkout/acceptance service logic. | Catalog and Order modules | PASS |
| BR-06 | `MADE_TO_ORDER` uses daily capacity; `LIMITED_STOCK` uses stock and reservation. | `product_variants`, `inventory_locations`, `inventory_capacity_records`, `inventory_stock_records` | Inventory records must reference the variant and location; mode-specific records must not be mixed incorrectly. | Inventory module | PASS |
| BR-07 | Capacity/stock is reserved in the checkout transaction and released on reject, cancel or payment failure. | `orders`, `order_items`, `inventory_capacity_records`, `inventory_stock_records` | Non-negative quantities and reservation bounds are required; reserve/release must be atomic and idempotent. | Order and Inventory modules | PASS |
| BR-08 | When capacity is exhausted, the catalog marks a variant unavailable; an old Cart item remains visible but checkout is blocked. | `product_variants`, `carts`, `cart_items`, `inventory_capacity_records` | Capacity availability is derived from remaining capacity; Cart items are not hard-deleted by catalog availability changes. | Catalog and Cart modules | PASS |
| BR-09 | Checkout revalidates active status, Store ownership, price, availability, capacity and stock on the server. | `stores`, `products`, `product_variants`, `carts`, `cart_items`, `orders`, inventory records | Client totals and availability are never trusted; validation and reservation execute inside the checkout transaction. | Order module | PASS |
| BR-10 | OrderItem stores Product/Variant name, unit price, quantity and line total snapshots. | `orders`, `order_items`, `products`, `product_variants` | Snapshot columns are required and remain unchanged when the catalog is later edited. | Order module | PASS |

## 4. Constraint and ownership review

The following responsibilities are intentionally divided between PostgreSQL and application services.

| Concern | Database responsibility | Application responsibility |
|---|---|---|
| Primary key | Define a primary key for every table. | Use generated identifiers and never accept an arbitrary ownership-changing ID from a client. |
| Foreign key | Prevent references to non-existing Store, Product, Variant, Order, User or related records. | Check that the authenticated actor is allowed to access the referenced Store or Order. |
| Unique rule | Enforce identities such as user identity, Store-category assignment and Product-variant name within the correct parent. | Return a domain error instead of exposing a raw database exception. |
| Check rule | Enforce non-negative quantities, positive price, valid reservation bounds and valid enum-like values where practical. | Enforce cross-row and cross-table rules such as checkout capacity and acceptance policy. |
| Ownership | Store the foreign-key path needed to determine ownership. | Enforce role, Store scope, Customer ownership and Driver assignment ownership. |
| Transaction boundary | Provide rows and version columns that can be locked or updated atomically. | Reserve/release inventory, create the Order and write audit events in one business transaction. |
| Historical integrity | Preserve OrderItem snapshots and audit records. | Never rewrite historical order meaning when current catalog data changes. |

## 5. Rules that are not solvable by a static ERD alone

The ERD can represent the required data and constraints, but the following behavior must be implemented and tested by application services:

1. Rechecking availability and price during checkout.
2. Reserving stock/capacity without overselling under concurrency.
3. Releasing a reservation exactly once.
4. Deciding whether an Order requires Merchant acceptance.
5. Calculating Order pricing from server-side data.
6. Preventing a Cart from mixing Stores when the referenced ProductVariant changes.
7. Enforcing role and Store ownership.
8. Maintaining the Order State Machine.
9. Recording audit events in the same transaction as the state change.
10. Handling retry and idempotency behavior.

## 6. Review conclusion

The canonical ERD covers the first ten business rules and identifies the required PK, FK, unique, check and ownership responsibilities. The ERD is approved as the database design baseline for the subsequent relational data dictionary, migration plan and persistence implementation tasks.

No JPA entity, Flyway SQL migration or REST API implementation is created by this review document.
