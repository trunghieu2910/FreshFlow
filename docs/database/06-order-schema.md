# FF-06-03-1 — Order aggregate and snapshot persistence

`Order` is the aggregate root. `OrderItem` belongs to exactly one order and is persisted with the root in one transaction through `OrderPersistenceService`. Flyway V1 created `orders` and `order_items`; V7 adds arithmetic checks without changing or rewriting historical rows.

The creation service receives IDs and quantities, loads the current customer, store, product and variant on the server, and rejects cross-store or inactive catalog entries. It copies product name, variant name and unit price into each order item. `line_total` is `unit_price_snapshot × quantity`; `subtotal` is the sum of line totals; `total_amount` is `subtotal - discount_amount + delivery_fee`. All amounts use Java `BigDecimal` / `Money` and PostgreSQL `NUMERIC(12,2)`. The caller supplies only server-determined fee and discount values. The service never accepts client-supplied item prices or totals.

The catalog foreign keys remain for traceability, while order reads use snapshot columns. Renaming a product or changing a variant price cannot change an existing order. V7 checks the stored arithmetic at the database boundary. Existing V6 demo rows preserve their original historical values; some demo order subtotals do not equal their seeded item sums, so V7 does not assert cross-row equality for legacy data.

`OrderPersistenceService` is used by the FF-06-03-2 checkout transaction, documented in [06-checkout-transaction.md](./06-checkout-transaction.md). Checkout owns inventory reservation, payment attempt creation, idempotency and initial state selection. This persistence component does not expose a customer-facing endpoint by itself.
