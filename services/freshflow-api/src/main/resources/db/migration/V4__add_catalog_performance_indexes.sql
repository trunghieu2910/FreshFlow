-- Migration V4: Add catalog performance B-Tree indexes for pagination, sorting, and filtering (DB-03-B)
-- Optimizes query selectivity on active products, category filtering, variant size, and inventory mode.

-- Index for public catalog listing by store, filtering active products, and sorting by name
CREATE INDEX IF NOT EXISTS idx_products_store_active_name
  ON products (store_id, is_active, name);

-- Index for public catalog listing by store, filtering active products, and sorting by creation timestamp
CREATE INDEX IF NOT EXISTS idx_products_store_active_created_at
  ON products (store_id, is_active, created_at DESC);

-- Composite index for store category active joins and filtering
CREATE INDEX IF NOT EXISTS idx_products_store_category_active
  ON products (store_id, store_category_id, is_active);

-- Index for active store categories lookups
CREATE INDEX IF NOT EXISTS idx_store_categories_store_active
  ON store_categories (store_id, is_active);

-- Index for product variant filtering by active state and size (M, L, STANDARD)
CREATE INDEX IF NOT EXISTS idx_product_variants_active_size
  ON product_variants (is_active, size);

-- Index for product variant filtering by active state and inventory mode (MADE_TO_ORDER, LIMITED_STOCK)
CREATE INDEX IF NOT EXISTS idx_product_variants_active_inventory_mode
  ON product_variants (is_active, inventory_mode);
