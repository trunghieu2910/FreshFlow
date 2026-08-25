# FreshFlow Catalog JPA Mapping

## Scope

This document records the persistence mapping for FF-02-01-1. It implements the approved ERD catalog tables: `stores`, `categories`, `store_categories`, `products` and `product_variants`.

`ProductVariant` is the purchasable unit. `Product` does not contain a purchase price or purchase size. A product with sizes has variants such as `M` and `L`; a product without size has one variant with `name = STANDARD` and `size = NULL`.

## Entity ownership

| Entity | Module | Main responsibility |
|---|---|---|
| `Store` | Catalog | Store identity, address, status and auto-accept default |
| `Category` | Catalog | Shared global category |
| `StoreCategory` | Catalog | Store-specific category activation and display order |
| `Product` | Catalog | Store-owned product definition and variant aggregate |
| `ProductVariant` | Catalog | Purchasable variant, price, availability and inventory mode |

## Mapping decisions

- All identifiers use PostgreSQL `BIGINT` identity and Java `Long`.
- Money uses PostgreSQL `NUMERIC(12,2)` and Java `BigDecimal` through the existing `Money` value object.
- Timestamps use `TIMESTAMPTZ` and Java `Instant` in UTC.
- Parent references use `@ManyToOne(fetch = FetchType.LAZY)`.
- `Product` owns its variants with `cascade = CascadeType.ALL` and `orphanRemoval = true`.
- `ProductVariant.name` is unique within its Product. No `ProductVariant.code` column exists.
- `ProductVariant.size` remains nullable because `STANDARD` uses `size = NULL`.
- `Product` stores both `store_id` and `store_category_id`, as required by the ERD. The application must verify that both references belong to the same Store.
- Soft visibility uses `is_active`; availability is a separate `is_available` field on ProductVariant.
- Catalog entities use lifecycle callbacks for `created_at` and `updated_at`; no separate auditing configuration is introduced in this task.
- Catalog does not hard-delete business records. Status/active flags preserve historical meaning.

## Constraint ownership

PostgreSQL enforces primary keys, foreign keys, unique constraints, non-negative price, positive optional quantity limits and enum-like inventory mode values. Application services must additionally enforce the `STANDARD` convention, Store/StoreCategory ownership consistency and later checkout rules.

## Snapshot boundary

Changing `ProductVariant.price` updates current catalog data only. It must not update `OrderItemSnapshot`, which stores historical product/variant name, unit price, quantity and line total at checkout.

## Migration order

This task uses four small migrations: users, stores, catalog categories and products/variants. The supporting `users` and `stores` tables are included because catalog foreign keys depend on them; user/domain entities outside the catalog remain future tasks.
