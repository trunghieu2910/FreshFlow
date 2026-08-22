# FF-01-05-1 — Relational Model, Data Dictionary and Migration Plan

**Task:** `FF-01-05-1`
**Status:** Approved design baseline
**Canonical ERD:** [`docs/architecture/erd.md`](../architecture/erd.md)
**Business-rule review:** [`docs/architecture/ff-01-05-1-review.md`](../architecture/ff-01-05-1-review.md)
**Database convention:** [`docs/database/02-postgres-convention.md`](./02-postgres-convention.md)

## 1. Purpose and scope

This document defines the relational-model review for the FreshFlow MVP. It translates the canonical ERD into a reviewable data dictionary containing the table inventory, primary keys, foreign keys, unique rules, check rules and module ownership.

The document also defines the planned order for future Flyway migrations. It is a design artifact only. It does not create JPA entities, repositories, services, REST controllers or SQL migration files.

The model covers Store-scoped catalog management, ProductVariant, daily capacity, limited stock, Cart, Order, payment attempts, Driver delivery, OTP/PIN, disputes, audit records and idempotency protection.

## 2. Design principles

FreshFlow uses a modular-monolith backend. Tables are relational and are grouped by business ownership rather than by separate physical services. A module owns the lifecycle and invariants of its tables, while foreign keys preserve database-level referential integrity across modules.

The database uses PostgreSQL conventions from `docs/database/02-postgres-convention.md`:

| Concern | FreshFlow convention |
|---|---|
| Identifier | `BIGINT` generated identifier |
| Money | `NUMERIC(12,2)` and non-negative where applicable |
| Time | `TIMESTAMPTZ`, represented by Java `Instant`, UTC |
| Boolean state | `BOOLEAN` with an explicit default where appropriate |
| Enum-like state | `VARCHAR` plus a `CHECK` constraint or application enum policy |
| Historical records | Preserve records; use status/soft visibility instead of hard deletion |
| Public order identity | Internal numeric `id` plus unique public `order_number` |
| Optimistic concurrency | `version` column on inventory records |

## 3. Normalization review: 1NF to 3NF

### 3.1. First Normal Form — 1NF

The model satisfies 1NF by keeping each column atomic. Repeating values are represented by child tables instead of comma-separated strings or repeated column groups.

Examples include:

| Repeating concept | Normalized representation |
|---|---|
| Users assigned to Stores and roles | `user_store_roles` |
| Categories assigned to Stores | `store_categories` |
| Product sizes/variants | `product_variants` |
| Items in a Cart | `cart_items` |
| Items in an Order | `order_items` |
| Payment retries | `payments` |
| Delivery assignment history | `delivery_assignments` |
| Order state history | `order_audits` |

JSONB is used only for flexible metadata or cached response payloads where the structure is not a core relational attribute. Core searchable business attributes remain normal columns.

### 3.2. Second Normal Form — 2NF

The model uses a surrogate `BIGINT` primary key for each table. Non-key attributes depend on the row identified by that primary key. Relationship attributes belong to relationship tables rather than being duplicated in unrelated parent tables.

For example, the active flag and display order for a Store-category relationship belong to `store_categories`, not to `categories`, because those values describe the relationship between a particular Store and Category.

### 3.3. Third Normal Form — 3NF

The model avoids transitive dependencies between non-key attributes. Store information is stored in `stores`; Product information is stored in `products`; variant-specific price and inventory mode are stored in `product_variants`; Order history stores immutable snapshots where historical meaning must not depend on current catalog data.

`order_items.product_name_snapshot`, `variant_name_snapshot` and `unit_price_snapshot` are intentionally denormalized historical values. They are not a 3NF violation in the transactional catalog model because they preserve the state of an Order at checkout and must remain unchanged when the catalog changes.

## 4. Complete table inventory

The approved ERD contains the following 23 physical tables:

| No. | Table | Module owner | Purpose |
|---:|---|---|---|
| 1 | `users` | Common/Identity | Customer, Merchant, Driver and administrative user identity |
| 2 | `roles` | Common/Identity | Global role catalog |
| 3 | `stores` | Store/Catalog | Merchant-owned Store and Store-level defaults |
| 4 | `user_store_roles` | Common/Identity | User-to-Store role assignment |
| 5 | `driver_profiles` | Delivery | Driver profile and Store affiliation |
| 6 | `driver_availability_audits` | Delivery | History of Driver availability changes |
| 7 | `categories` | Catalog | Global category catalog |
| 8 | `store_categories` | Catalog | Store-specific category assignment and display settings |
| 9 | `products` | Catalog | Store-owned product definition |
| 10 | `product_variants` | Catalog | Purchasable variant, price and inventory mode |
| 11 | `inventory_locations` | Inventory/Catalog | Store preparation location such as `MAIN_KITCHEN` |
| 12 | `inventory_stock_records` | Inventory | Limited-stock quantity and reservation state |
| 13 | `inventory_capacity_records` | Inventory | Made-to-order capacity by date and location |
| 14 | `carts` | Cart/Order | Customer cart scoped to one Store |
| 15 | `cart_items` | Cart/Order | ProductVariant quantities in a Cart |
| 16 | `orders` | Order | Checkout result and current Order state |
| 17 | `order_items` | Order | Purchased items and historical snapshots |
| 18 | `payments` | Payment | Payment attempts and payment status |
| 19 | `idempotency_records` | Common/Order | Duplicate request protection |
| 20 | `delivery_assignments` | Delivery | Driver assignment history for an Order |
| 21 | `delivery_credentials` | Delivery | Hashed OTP/PIN delivery confirmation credential |
| 22 | `disputes` | Order/Delivery | Customer dispute and Merchant resolution |
| 23 | `order_audits` | Order/Common | State-change and operational audit history |

## 5. Data dictionary and constraint matrix

The following sections are the logical data dictionary. `NOT NULL` is represented by the `Required` column. The Unique and Check columns describe constraints that must be enforced by the future PostgreSQL migration unless explicitly marked as application-level behavior.

### 5.1. Identity, Store and Delivery foundation

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `users` | `id`, `email`, `password_hash`, `full_name`, `phone`, `status`, `created_at`, `updated_at` | `id` | None | `email`; `phone` may become unique if phone login is enabled | `email` normalized lowercase; `status` in `ACTIVE`, `LOCKED`, `PENDING`; password is never plaintext | Common/Identity |
| `roles` | `id`, `code`, `name`, `created_at` | `id` | None | `code` | `code` uppercase and non-empty | Common/Identity |
| `stores` | `id`, `owner_user_id`, `name`, `phone`, `address_line`, `auto_accept_default`, `status`, timestamps | `id` | `owner_user_id -> users.id` | `owner_user_id` in MVP | `status` in `ACTIVE`, `INACTIVE`, `SUSPENDED`; `auto_accept_default` has a default | Store/Catalog |
| `user_store_roles` | `id`, `user_id`, `store_id`, `role_id`, `status`, timestamps | `id` | `user_id -> users.id`; `store_id -> stores.id`; `role_id -> roles.id` | `(user_id, store_id, role_id)` | `status` in `ACTIVE`, `INACTIVE` | Common/Identity |
| `driver_profiles` | `id`, `user_id`, `store_id`, `is_available`, `vehicle_type`, `status`, timestamps | `id` | `user_id -> users.id`; `store_id -> stores.id` | `user_id` | `status` in `ACTIVE`, `SUSPENDED`, `INACTIVE`; unavailable by default | Delivery |
| `driver_availability_audits` | `id`, `driver_profile_id`, `is_available`, `changed_by_user_id`, `changed_at`, `reason` | `id` | `driver_profile_id -> driver_profiles.id`; `changed_by_user_id -> users.id` | None required | `changed_at` required; `reason` optional | Delivery |

### 5.2. Catalog and ProductVariant

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `categories` | `id`, `name`, `description`, `is_active`, timestamps | `id` | None | `name` according to global catalog policy | `name` non-empty; `is_active` controls visibility | Catalog |
| `store_categories` | `id`, `store_id`, `category_id`, `is_active`, `display_order`, timestamps | `id` | `store_id -> stores.id`; `category_id -> categories.id` | `(store_id, category_id)` | `display_order >= 0` | Catalog |
| `products` | `id`, `store_id`, `store_category_id`, `name`, `description`, `image_url`, `is_active`, timestamps | `id` | `store_id -> stores.id`; `store_category_id -> store_categories.id` | Product name policy is Store-scoped | Product must belong to the same Store as its `store_category_id`; `name` non-empty | Catalog |
| `product_variants` | `id`, `product_id`, `name`, `size`, `price`, `inventory_mode`, `auto_accept_override`, `max_quantity_per_order`, `is_available`, `is_active`, timestamps | `id` | `product_id -> products.id` | `(product_id, name)` | `price >= 0`; inventory mode in `MADE_TO_ORDER`, `LIMITED_STOCK`; quantity limit positive when present; STANDARD convention validated | Catalog |

`product_variants` is the purchasable unit. For a product without size, the approved convention is one `STANDARD` variant with `size = NULL`. This convention is partly a data constraint and partly a catalog application rule because a simple database check cannot, by itself, guarantee that a product has exactly one STANDARD variant.

### 5.3. Inventory and capacity

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `inventory_locations` | `id`, `store_id`, `name`, `type`, `is_default`, `is_active`, timestamps | `id` | `store_id -> stores.id` | `(store_id, name)` | `type` in `MAIN_KITCHEN`, future `WAREHOUSE`; one default location per Store; name non-empty | Inventory/Catalog |
| `inventory_stock_records` | `id`, `variant_id`, `location_id`, `stock_quantity`, `reserved_quantity`, `version`, `updated_at` | `id` | `variant_id -> product_variants.id`; `location_id -> inventory_locations.id` | `(variant_id, location_id)` | `stock_quantity >= 0`; `reserved_quantity >= 0`; `reserved_quantity <= stock_quantity`; `version >= 0`; location belongs to variant Store | Inventory |
| `inventory_capacity_records` | `id`, `variant_id`, `location_id`, `capacity_date`, `capacity_limit`, `reserved_quantity`, `version`, timestamps | `id` | `variant_id -> product_variants.id`; `location_id -> inventory_locations.id` | `(variant_id, location_id, capacity_date)` | `capacity_limit >= 0`; `reserved_quantity >= 0`; `reserved_quantity <= capacity_limit`; `version >= 0`; date is the Store business date | Inventory |

Stock/capacity reservation is a transaction boundary owned by the Inventory and Order modules. Foreign keys and checks prevent invalid static values, while row locking or optimistic version checks prevent overselling under concurrency.

### 5.4. Cart and Order

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `carts` | `id`, `customer_user_id`, `store_id`, `status`, timestamps | `id` | `customer_user_id -> users.id`; `store_id -> stores.id` | At most one active Cart per Customer and Store | `status` in `ACTIVE`, `CHECKED_OUT`, `ABANDONED` | Cart/Order |
| `cart_items` | `id`, `cart_id`, `variant_id`, `quantity`, timestamps | `id` | `cart_id -> carts.id`; `variant_id -> product_variants.id` | `(cart_id, variant_id)` | `quantity > 0`; variant must belong to Cart Store; availability is rechecked at checkout | Cart/Order |
| `orders` | `id`, `order_number`, `customer_user_id`, `store_id`, `current_driver_id`, `status`, `payment_method`, `merchant_acceptance_status`, pricing fields, cancellation/timestamp fields | `id` | `customer_user_id -> users.id`; `store_id -> stores.id`; `current_driver_id -> driver_profiles.id` nullable | `order_number` | Money values non-negative; `payment_method` in approved methods; `status` follows Order State Machine; acceptance status in `PENDING`, `ACCEPTED`, `REJECTED` | Order |
| `order_items` | `id`, `order_id`, `product_id`, `product_variant_id`, product/variant snapshots, `unit_price_snapshot`, `quantity`, `line_total` | `id` | `order_id -> orders.id`; `product_id -> products.id` nullable; `product_variant_id -> product_variants.id` | None required | `unit_price_snapshot >= 0`; `quantity > 0`; `line_total >= 0`; snapshot fields required | Order |

`orders` owns the current aggregate state, while `order_audits` owns historical state transitions. `order_items` keeps snapshots so that catalog edits cannot change the meaning of a completed or disputed Order.

### 5.5. Payment, idempotency and delivery

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `payments` | `id`, `order_id`, `attempt_number`, `method`, `status`, `amount`, provider/failure/timestamp fields | `id` | `order_id -> orders.id` | `(order_id, attempt_number)`; `provider_reference` when present | `attempt_number >= 1`; amount non-negative; method in `ONLINE_MOCK`, `CASH_ON_DELIVERY`, `BANK_TRANSFER_ON_DELIVERY`; status follows payment policy | Payment |
| `idempotency_records` | `id`, `user_id`, `idempotency_key`, `request_hash`, `order_id`, `response_status`, `response_body`, `expires_at`, `created_at` | `id` | `user_id -> users.id`; `order_id -> orders.id` nullable | `(user_id, idempotency_key)` | Key and request hash required; `response_status` is a valid HTTP status when present; expiration required | Common/Order |
| `delivery_assignments` | `id`, `order_id`, `driver_profile_id`, `status`, `attempt_number`, assignment timestamps, failure reason | `id` | `order_id -> orders.id`; `driver_profile_id -> driver_profiles.id` | `(order_id, attempt_number)` | attempt number starts at 1; status in `ASSIGNED`, `DISPATCHED`, `DELIVERING`, `DELIVERED`, `FAILED`, `ENDED`; Driver belongs to Order Store | Delivery |
| `delivery_credentials` | `id`, `delivery_assignment_id`, `otp_hash`, `expires_at`, `used_at`, `attempt_count`, `max_attempts`, `created_at` | `id` | `delivery_assignment_id -> delivery_assignments.id` | One active credential policy per assignment | OTP is never plaintext; attempt counts non-negative; `attempt_count <= max_attempts`; expiry required | Delivery |

COD OTP/PIN verification is an application service rule. The database stores only the hash and attempt/expiry state. The plaintext OTP must never be persisted or returned by the API.

### 5.6. Dispute and audit

| Table | Main columns | PK | FK | Unique | Check/invariant | Owner |
|---|---|---|---|---|---|---|
| `disputes` | `id`, `order_id`, `customer_user_id`, `reason`, `customer_message`, `status`, resolution fields, timestamps | `id` | `order_id -> orders.id`; `customer_user_id -> users.id`; `resolved_by_user_id -> users.id` nullable | Dispute uniqueness policy is application-defined; MVP may allow one active dispute per Order | Reason is from approved policy; status in `OPEN`, `RESOLVED_DELIVERED`, `RESOLVED_CANCELLED`; resolution fields required when resolved | Order/Delivery |
| `order_audits` | `id`, `order_id`, `actor_user_id`, `actor_role`, `event_type`, `from_status`, `to_status`, `reason`, `metadata`, `created_at` | `id` | `order_id -> orders.id`; `actor_user_id -> users.id` nullable | None required | `from_status`/`to_status` follow state policy; system events may have null actor; event type required | Order/Common |

## 6. Relationship and ownership matrix

| Parent | Child | Cardinality | Ownership rule |
|---|---|---:|---|
| `users` | `stores` | 1 to 0..1 in MVP | `stores.owner_user_id` identifies the Merchant owner |
| `users` | `user_store_roles` | 1 to many | A user receives role assignments per Store |
| `stores` | `user_store_roles` | 1 to many | Store access is Store-scoped |
| `stores` | `driver_profiles` | 1 to many | Driver belongs to one Store in MVP |
| `stores` | `store_categories` | 1 to many | Store controls category visibility/order |
| `store_categories` | `products` | 1 to many | Product must use a category assigned to the same Store |
| `products` | `product_variants` | 1 to many | ProductVariant is the purchasable unit |
| `stores` | `inventory_locations` | 1 to many | MVP creates one default `MAIN_KITCHEN` |
| `product_variants` | `inventory_stock_records` | 1 to 0..many | Limited-stock quantity is location-scoped |
| `product_variants` | `inventory_capacity_records` | 1 to many by date | Made-to-order capacity is date/location-scoped |
| `users` | `carts` | 1 to many | Customer owns the Cart |
| `carts` | `cart_items` | 1 to many | All items must use variants from one Store |
| `stores` | `orders` | 1 to many | Each Order belongs to exactly one Store |
| `orders` | `order_items` | 1 to many | Order owns immutable item snapshots |
| `orders` | `payments` | 1 to many | Payment attempts are append-oriented history |
| `orders` | `delivery_assignments` | 1 to many | Assignment retries are historical records |
| `delivery_assignments` | `delivery_credentials` | 1 to many by policy | Credential lifecycle is assignment-scoped |
| `orders` | `disputes` | 1 to many by policy | Dispute is linked to the affected Order |
| `orders` | `order_audits` | 1 to many | All state changes and operational events are auditable |

## 7. Transaction boundaries

The following business operations have explicit application transaction boundaries:

| Transaction | Main tables | Required behavior |
|---|---|---|
| Create/update Cart | `carts`, `cart_items` | Enforce one Store per Cart and positive quantities |
| Checkout | `carts`, `cart_items`, catalog tables, inventory tables, `orders`, `order_items`, `payments`, `idempotency_records` | Revalidate client input, reserve stock/capacity, calculate pricing from server data and create the Order atomically |
| Merchant accept/reject | `orders`, `order_audits`, inventory tables | Validate state transition, release reservation on rejection and write audit event |
| Payment callback/confirmation | `payments`, `orders`, `order_audits` | Be idempotent and avoid duplicate state transitions |
| Driver assignment | `delivery_assignments`, `orders`, `order_audits` | Enforce Store/Driver ownership and update the current assignment atomically |
| Delivery completion | `delivery_credentials`, `delivery_assignments`, `orders`, `order_audits` | Verify OTP/PIN policy, prevent replay and record completion |
| Dispute resolution | `disputes`, `orders`, `order_audits` | Enforce allowed resolution and preserve audit history |

A static foreign key or check constraint cannot fully enforce cross-row rules such as “do not oversell under concurrency” or “one manual ProductVariant causes Merchant acceptance.” Those rules belong to application services and must later receive integration tests.

## 8. Planned Flyway migration order

The migration plan is intentionally separate from the current FF-01-05-1 design artifact. Future SQL migrations should be small, reviewable and ordered by dependency.

| Planned migration group | Tables | Dependency reason |
|---|---|---|
| V1 | `users`, `roles` | Root identity tables with no application foreign keys |
| V2 | `stores`, `user_store_roles` | Store ownership and user role assignment |
| V3 | `driver_profiles`, `driver_availability_audits` | Driver identity depends on users and stores |
| V4 | `categories`, `store_categories` | Catalog category foundation |
| V5 | `products`, `product_variants` | ProductVariant depends on Store and category assignment |
| V6 | `inventory_locations`, `inventory_stock_records`, `inventory_capacity_records` | Inventory depends on variants and Stores |
| V7 | `carts`, `cart_items` | Cart depends on Customer, Store and ProductVariant |
| V8 | `orders`, `order_items` | Order depends on Customer, Store, Driver, Product and ProductVariant |
| V9 | `payments`, `idempotency_records` | Payment/idempotency depend on Order and User |
| V10 | `delivery_assignments`, `delivery_credentials` | Delivery depends on Order and Driver |
| V11 | `disputes`, `order_audits` | Dispute/audit depend on Order and actor users |
| V12 | Cross-table indexes and partial uniqueness | Add query indexes and policy-specific indexes after the base schema is stable |

The exact version names may be consolidated or split during the implementation task. A migration must not be written until the table definitions, constraint names and deletion/update policies are approved.

## 9. Review checklist

| Review item | Status | Evidence |
|---|---|---|
| All 23 ERD tables listed | PASS | Section 4 |
| Every table has a primary key | PASS in design | Section 5 |
| Foreign-key ownership documented | PASS in design | Sections 5 and 6 |
| Unique rules documented | PASS in design | Section 5 |
| Check/invariant rules documented | PASS in design | Section 5 |
| 1NF review completed | PASS | Section 3.1 |
| 2NF review completed | PASS | Section 3.2 |
| 3NF review completed | PASS | Section 3.3 |
| Store-scoped ownership reviewed | PASS | Section 6 |
| ProductVariant and inventory modes reviewed | PASS | Sections 5.2 and 5.3 |
| Payment, delivery and dispute areas reviewed | PASS | Sections 5.5 and 5.6 |
| Future migration dependency order documented | PASS | Section 8 |
| SQL migration implemented | NOT IN THIS TASK | Deferred to persistence implementation task |
| JPA entity implemented | NOT IN THIS TASK | Deferred to persistence implementation task |

## 10. Definition of done for FF-01-05-1

FF-01-05-1 is complete as a design task when all of the following are true:

1. The canonical ERD is stored at `docs/architecture/erd.md`.
2. The ERD metadata identifies task `FF-01-05-1` and status `Approved`.
3. The ten business rules are reviewed in `docs/architecture/ff-01-05-1-review.md`.
4. This relational-model/data-dictionary document lists all 23 tables.
5. Every table has documented PK, FK, unique/check and owner responsibilities.
6. The migration dependency order is recorded without prematurely implementing SQL.
7. Scope/requirements/order-state-machine references resolve to the canonical ERD path.
8. Markdown and Git whitespace checks pass.

## References

1. [`docs/architecture/erd.md`](../architecture/erd.md) — Canonical FreshFlow MVP ERD.
2. [`docs/architecture/ff-01-05-1-review.md`](../architecture/ff-01-05-1-review.md) — Ten business-rule review and traceability.
3. [`docs/FF-01-01-1_scope.md`](../FF-01-01-1_scope.md) — FreshFlow MVP scope.
4. [`docs/FF-01-01-1_Software Requirements Specification — FreshFlow MVP.md`](../FF-01-01-1_Software%20Requirements%20Specification%20%E2%80%94%20FreshFlow%20MVP.md) — MVP requirements.
5. [`docs/FF-01-02-1_FreshFlow Order State Machine.md`](../FF-01-02-1_FreshFlow%20Order%20State%20Machine.md) — Order state transitions.
6. [`docs/database/02-postgres-convention.md`](./02-postgres-convention.md) — PostgreSQL naming and datatype conventions.

> This document is a design baseline. It must be reviewed against the canonical ERD before any Flyway migration or JPA persistence implementation is started.

---

**Next implementation task:** create the initial PostgreSQL schema only after this relational model and migration order are approved.
