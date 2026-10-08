-- Preserve the arithmetic captured at checkout, including when rows are written outside JPA.
ALTER TABLE orders
  ADD CONSTRAINT ck_orders_discount_within_subtotal CHECK (discount_amount <= subtotal),
  ADD CONSTRAINT ck_orders_total_matches_components
    CHECK (total_amount = subtotal - discount_amount + delivery_fee);

ALTER TABLE order_items
  ADD CONSTRAINT ck_order_items_line_total_matches_snapshot
    CHECK (line_total = unit_price_snapshot * quantity);
